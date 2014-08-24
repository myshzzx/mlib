package de.neotos.imageanalyzer;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.*;

/**
 * Aparapi GPU Acceleration.
 *
 * @author Mysh
 * @since 14-1-11 下午11:20
 */
public class ParallelManager<UO> implements ImageFeatureManager<UO> {
	private static final Logger log = LoggerFactory.getLogger(ParallelManager.class);

	private static final int LINE_SIZE = 10_000;

	private int dimension;
	private Class<UO> uoClass;
	private List<float[]> fs;
	private int fsCount = 0;
	private List<UO[]> uos;

//	private Map<UO, List<ImageFeature>> reverseKeys;

	private long eachFeatureTimeout = 1;
	private final ExecutorService exec =
					Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 3);
	private final ThreadLocal<ParaEngine> engines = new ThreadLocal<>();
	private final Queue<ParaEngine> createdEngines = new ConcurrentLinkedQueue<>();
	private final Range paraRange = Range.create(LINE_SIZE);
	private Range lastLineRange;

	private Thread disposeGPUEngines = new Thread() {
		@Override
		public void run() {
			for (ParaEngine e : createdEngines) {
				e.dispose();
			}
			createdEngines.clear();
		}
	};

	private ParallelManager() {
	}

	public ParallelManager(int d, Class<UO> uoClass) {
		this.dimension = d;
		this.uoClass = uoClass;
		fs = new ArrayList<>();
		uos = new ArrayList<>();

		Runtime.getRuntime().addShutdownHook(disposeGPUEngines);
	}

	private class ParaEngine extends Kernel {
		private int dimension;
		private float[] sample;
		private float[] target, dist;

		@Override
		public void run() {
			int i = getGlobalId();
			dist[i] = 0;

			float diff;
			for (int j = 0; j < dimension; j++) {
				diff = sample[i * dimension + j] - target[j];
				dist[i] += diff * diff;
			}
		}
	}

	@Override
	public List<ImageAnalyzerResult<UO>> findMatches(
					List<ImageFeature> fList,
					int minMatchCount) throws InterruptedException {

		final CountDownLatch taskLatch = new CountDownLatch(fList.size() * fs.size());
		final float[] nearestDist = new float[fList.size()];
		Arrays.fill(nearestDist, Float.MAX_VALUE);
		final int[] nearestUOIndex = new int[fList.size()];

		for (int i = 0; i < fs.size(); i++) {
			final float[] fsLine = fs.get(i);
			final Range useRange = i == fs.size() - 1 ? lastLineRange : paraRange;
			final int startIndex = i * LINE_SIZE;

			for (int fIndex = 0; fIndex < fList.size(); fIndex++) {
				final float[] target = fList.get(fIndex).getDescriptor();
				final int targetIndex = fIndex;

				exec.execute(() -> {
					try {
						ParaEngine eng = engines.get();
						if (eng == null) {
							eng = new ParaEngine();
							eng.dimension = dimension;
							eng.dist = new float[LINE_SIZE];
							engines.set(eng);
							createdEngines.add(eng);
						}

						eng.sample = fsLine;
						eng.target = target;
						eng.execute(useRange);

						synchronized (target) {
							int lineLen = startIndex + LINE_SIZE > fsCount ? fsCount % LINE_SIZE : LINE_SIZE;
							for (int i1 = 0; i1 < lineLen; i1++)
								if (eng.dist[i1] < nearestDist[targetIndex]) {
									nearestDist[targetIndex] = eng.dist[i1];
									nearestUOIndex[targetIndex] = startIndex + i1;
								}
						}
					} finally {
//							System.out.println(taskLatch.getCount());
						taskLatch.countDown();
					}
				});
			}
		}

		taskLatch.await(this.eachFeatureTimeout * fList.size(), TimeUnit.MICROSECONDS);

		Map<UO, ImageAnalyzerResult<UO>> resultMap = new HashMap<>(fList.size());
		for (int i = 0; i < nearestDist.length; i++) {
			UO uo = uos.get(nearestUOIndex[i] / LINE_SIZE)[nearestUOIndex[i] % LINE_SIZE];

			ImageAnalyzerResult<UO> result = resultMap.get(uo);
			if (result == null) {
				resultMap.put(uo, new ImageAnalyzerResult<UO>(uo, 1, nearestDist[i]));
			} else {
				result.matches++;
				result.distSum += nearestDist[i];
			}
		}

		List<ImageAnalyzerResult<UO>> r = new ArrayList<>();
		for (Map.Entry<UO, ImageAnalyzerResult<UO>> e : resultMap.entrySet()) {
			if (e.getValue().matches >= minMatchCount)
				r.add(e.getValue());
		}

		return r;
	}

	/**
	 * 从流恢复状态.
	 */
	public static ParallelManager restoreFromStream(ObjectInputStream in) throws IOException, ClassNotFoundException {
		ParallelManager mgr = new ParallelManager();
		mgr.dimension = (int) in.readObject();
		mgr.uoClass = (Class) in.readObject();
		mgr.fs = (List) in.readObject();
		mgr.fsCount = (int) in.readObject();
		mgr.uos = (List) in.readObject();
		mgr.eachFeatureTimeout = (int) in.readObject();
		mgr.lastLineRange = Range.create(mgr.fsCount % LINE_SIZE);
		return mgr;
	}

	@Override
	public void storeToStream(ObjectOutputStream out) throws IOException {
		out.writeObject(this.dimension);
		out.writeObject(this.uoClass);
		out.writeObject(this.fs);
		out.writeObject(this.fsCount);
		out.writeObject(this.uos);
		out.writeObject(this.eachFeatureTimeout);
	}


	@Override
	public void putFeatures(List<ImageFeature> flist, UO userObject) {
		int index = fsCount % LINE_SIZE;
		float[] modFs = fs.size() > 0 ? fs.get(fs.size() - 1) : null;
		UO[] modUOs = uos.size() > 0 ? uos.get(uos.size() - 1) : null;

		for (ImageFeature f : flist) {
			if (index == 0) {
				modFs = new float[LINE_SIZE * dimension];
				fs.add(modFs);
				modUOs = (UO[]) Array.newInstance(uoClass, LINE_SIZE);
				uos.add(modUOs);
			}

			System.arraycopy(f.getDescriptor(), 0, modFs, index * dimension, dimension);
			modUOs[index] = userObject;

			index = (index + 1) % LINE_SIZE;
		}

		fsCount += flist.size();
		lastLineRange = Range.create(fsCount % LINE_SIZE);
		log.debug("Inserted " + flist.size() + " Features into ParallelMgr with Value " + userObject);
	}

	@Override
	public List<ImageFeature> removeFeatures(UO userObject) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<ImageFeature> getFeatures(UO userObject) {
		throw new UnsupportedOperationException();
	}


	@Override
	public void setEachFeatureTimeout(long timeout) {
		this.eachFeatureTimeout = timeout;
	}

	@Override
	public void dispose() {
		exec.shutdown();
		this.disposeGPUEngines.run();
	}
}
