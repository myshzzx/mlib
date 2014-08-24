package de.neotos.imageanalyzer;

import com.jogamp.opencl.CLPlatform;
import edu.wlu.cs.levy.CG.KDNode;
import edu.wlu.cs.levy.CG.KDTree;
import mysh.gpgpu.JogAmpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mysh
 * @since 2014/8/23 23:35
 */
public class KDTreeParaMgr<UO> implements ImageFeatureManager<UO> {

	private static final Logger log = LoggerFactory.getLogger(KDTreeParaMgr.class);

	private KDTree<UO> kdtree;
	private Map<UO, List<ImageFeature>> reverseKeys;

	private ExecutorService exec;
	private CLPlatform clPlatform;

	/**
	 * 从流恢复状态.
	 */
	public static KDTreeParaMgr restoreFromStream(ObjectInputStream in) throws IOException, ClassNotFoundException {
		KDTreeParaMgr mgr = new KDTreeParaMgr(0);
		mgr.kdtree = (KDTree) in.readObject();
		mgr.reverseKeys = (Map) in.readObject();
		return mgr;
	}


	@Override
	public void storeToStream(ObjectOutputStream out) throws IOException {
		out.writeObject(this.kdtree);
		out.writeObject(this.reverseKeys);
	}

	public KDTreeParaMgr(int d) {
		kdtree = new KDTree<>(d);
		reverseKeys = new HashMap<>(4096);
		clPlatform = JogAmpUtil.getPropCLPlat(null);

		final AtomicInteger execThreadCount = new AtomicInteger(0);
		exec = Executors.newFixedThreadPool(
						Runtime.getRuntime().availableProcessors(),
						new ThreadFactory() {
							@Override
							public Thread newThread(Runnable r) {
								Thread t = new Thread(r, "No." + execThreadCount.incrementAndGet() + " of KDTree Mgr Exec Thread");
								t.setDaemon(true);
								t.setPriority(Thread.MIN_PRIORITY);
								return t;
							}
						}
		);
	}

	/**
	 * ignores existing keys, so if you have same features along different pictures (and so different userobjects)
	 * these features will not be inserted
	 */
	public void putFeatures(List<ImageFeature> features, UO userObj) {

		removeFeatures(userObj);

		reverseKeys.put(userObj, features);

		try {
			for (ImageFeature f : features)
				kdtree.insert(f.getDescriptor(), userObj);

			log.debug("Inserted " + features.size() + " features into KD-Tree with value " + userObj);
		} catch (Exception e) {
			log.error("insert features error, going to rollback, user object: " + userObj);
			this.removeFeatures(userObj);
		}
	}

	/**
	 * note that the actual implementation doesn't remove keys from the tree but only marks them as deleted.
	 */
	public List<ImageFeature> removeFeatures(UO userObj) {
		List<ImageFeature> imgFeatures = reverseKeys.get(userObj);

		if (imgFeatures != null) {
			float[] key;
			for (ImageFeature f : imgFeatures) {
				key = f.getDescriptor();
				kdtree.delete(key);
			}

			reverseKeys.remove(userObj);
		}

		return imgFeatures;
	}

	private float distance(float[] descr1, float[] descr2) {
		float dsq = 0, diff;

		int d = descr1.length;

		for (int i = 0; i < d; i++) {
			diff = descr1[i] - descr2[i];
			dsq += diff * diff;
		}
		return dsq;
	}

	public List<ImageAnalyzerResult<UO>> findMatches(List<ImageFeature> features, int minMatchCount) throws InterruptedException {

		List<ImageAnalyzerResult<UO>> result = new ArrayList<>();
		if (kdtree.isEmpty())
			return result;

		final List<Future<?>> nearestTasks = new ArrayList<>(features.size());
		final Queue<Object[]> nearestResult = new ConcurrentLinkedQueue<>();
		final CountDownLatch taskLatch = new CountDownLatch(features.size());
		for (final ImageFeature testFeature : features) {

			final Future<?> nearestTask = exec.submit(new Runnable() {
				@Override
				public void run() {
					try {
						float[] key = testFeature.getDescriptor();

//						long start = System.nanoTime();
						Object[][] nbrs = kdtree.getnbrs(key, 1);
//						log.debug("search feature cost(micro-second): " + (System.nanoTime() - start) / 1000);

//						if ((double) nbrs[0][1] < (double) nbrs[1][1] * 0.64)
						if (nbrs[0][0] != null)
							nearestResult.add(nbrs[0]);
					} finally {
						taskLatch.countDown();
					}
				}
			});

			nearestTasks.add(nearestTask);
		}

		try {
			taskLatch.await();
		} catch (InterruptedException e) {
			for (Future<?> nearestTask : nearestTasks) {
				nearestTask.cancel(false);
			}
			throw e;
		}

		final Map<UO, ImageAnalyzerResult<UO>> matchResultMap = new HashMap<>();
		for (Object[] nearestR : nearestResult) {
			try {
				KDNode<UO> nearest = (KDNode<UO>) nearestR[0];
				float dist = (float) nearestR[1];

				UO userObj = nearest.v;
				ImageAnalyzerResult<UO> mr = matchResultMap.get(userObj);
				if (mr == null)
					mr = new ImageAnalyzerResult<>(userObj, 1, dist);
				else {
					mr.distSum += dist;
					mr.matches++;
				}
				matchResultMap.put(userObj, mr);

			} catch (Exception e) {
				//do nothing but work on next item
				log.debug("ignoring Exception when finding kd-nodes for features", e);
			}
		}

		for (Map.Entry<UO, ImageAnalyzerResult<UO>> matchEle : matchResultMap.entrySet()) {
			if (matchEle.getValue().matches >= minMatchCount)
				result.add(matchEle.getValue());
		}

		return result;
	}

	@Override
	public void setEachFeatureTimeout(long eachFeatureTimeout) {
		kdtree.setTimeout(eachFeatureTimeout);
	}

	@Override
	public void dispose() {
		exec.shutdown();
	}

	public List<ImageFeature> getFeatures(UO userObject) {
		return reverseKeys.get(userObject);
	}

}
