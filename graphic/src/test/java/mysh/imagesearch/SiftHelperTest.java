package mysh.imagesearch;

import de.neotos.imageanalyzer.*;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KDUtil;
import mysh.imagesearch.preproc.GrayScaleLimit;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Mysh
 * @since 13-12-23 下午4:49
 */
@Ignore
public class SiftHelperTest {
	private static final Logger log = LoggerFactory.getLogger(SiftHelperTest.class);

	private static String[] dirs = new String[]{
					"E:\\project\\texture\\picsRep\\sample"
//					"L:\\a"
	};

	private static String[] targets = new String[]{
//					"C:\\Users\\allen\\Desktop\\纹理\\picsTarget"
	};

	private static File resultDir = new File("l:/result");

	public static void main(String[] args) throws IOException, InterruptedException {
		SiftHelper.FeaMgrType feaMgrType = SiftHelper.FeaMgrType.KDTree;
		final SiftHelper<String> analyzer = new SiftHelper<>(String.class, feaMgrType);

		ImgPreProc samplePreProc = genImgPreProc(180);
		ImgPreProc testPreProc = genImgPreProc(150);

		analyzer.setImgPreProc(samplePreProc);
		analyzer.setMinMatchCount(2);


		ImageFeatureManager featureManager = restoreFeaManager(feaMgrType);
		if (featureManager != null) {
			analyzer.setFeaturesManager(featureManager);
		} else {
			final ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			for (String dir : dirs)
				FileUtil.recurDir(new File(dir), null, new FileUtil.FileTask() {
					@Override
					public void handle(final File f) {
						exec.submit(new Runnable() {
							@Override
							public void run() {
								try {
									analyzer.bindImage(f.getName(), ImageIO.read(f));
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
					}
				});
			exec.shutdown();
			exec.awaitTermination(1, TimeUnit.DAYS);
			saveFeaManager(analyzer.getFeatureManager());
		}

		analyzer.getFeatureManager().setEachFeatureTimeout(3000);

		checkUserObjNum(analyzer);

//		featureTimeoutBenchmark(analyzer, testPreProc);

		System.out.println("ready to search.");
		BufferedReader s = new BufferedReader(new InputStreamReader(System.in));
		String file;
		while ((file = s.readLine()) != null) {
			if ((file = file.trim()).length() < 1) continue;

			searchImg(analyzer, file, testPreProc, dirs[0], resultDir);

		}
	}

	private static String[] testTarget = new String[]{
//					"C:/Users/allen/Desktop/纹理/picsTarget/e (2)h.jpg",
//					"C:/Users/allen/Desktop/纹理/picsTarget/e (2)p.jpg",
					"C:/Users/allen/Desktop/纹理/picsTarget/e (2)s.jpg",
//					"C:/Users/allen/Desktop/纹理/picsTarget/e (2)v.jpg",
					"C:/Users/allen/Desktop/纹理/picsTarget/e (3)c.jpg"
//					"C:/Users/allen/Desktop/纹理/picsTarget/e (3)v.jpg",
//					"C:/Users/allen/Desktop/纹理/picsTarget/e (4)c.jpg",
//					"C:/Users/allen/Desktop/纹理/picsTarget/e (4)h.jpg",
//					"C:/Users/allen/Desktop/纹理/picsTarget/e (4)s.jpg"
	};
	private static String[] testAnswer = new String[]{
//					"SFR00003.JPG",
//					"SFR00003.JPG",
					"SFR00003.JPG",
//					"SFR00003.JPG",
					"SFR00085.jpg"
//					"SFR00085.jpg",
//					"STC00051.jpg",
//					"STC00051.jpg",
//					"STC00051.jpg"
	};

	private static void featureTimeoutBenchmark(SiftHelper<String> analyzer, ImgPreProc testPreProc) {
		System.out.println("feature timeout benchmark ...");

		long timeout = 10_000, step = 500;

		try {
			TOTAL_TEST:
			while ((timeout -= step) > step) {
				analyzer.getFeatureManager().setEachFeatureTimeout(timeout);
				SINGLE_TEST:
				for (int i = 0; i < testTarget.length; i++) {
					System.out.println("testing " + testTarget[i]);
					List<ImageFeature> features = testPreProc.process(ImageIO.read(new File(testTarget[i])));
					for (ImageAnalyzerResult<String> r : analyzer.findImage(features)) {
						if (r.getUserObj().equals(testAnswer[i]))
							continue SINGLE_TEST;
					}
					System.err.println("testing failed: " + testTarget[i]);
					break TOTAL_TEST;
				}
			}

			System.out.println("feature timeout benchmark result: " + timeout);
		} catch (Exception e) {
			System.err.println("feature timeout benchmark fail.");
			e.printStackTrace();
		}
	}

	/**
	 * 检查同一个特征值有多少UO对应.
	 */
	static void checkUserObjNum(SiftHelper<String> analyzer) {
		try {
			ImageFeatureManager fm = analyzer.getFeatureManager();
			if (fm instanceof KDTreeManager) {
				Field kdtree = fm.getClass().getDeclaredField("kdtree");
				kdtree.setAccessible(true);
				KDTree t = (KDTree) kdtree.get(fm);
				KDUtil.checkUserObjNum(t);
			}
		} catch (Exception e) {

		}
	}

	static void searchImg(SiftHelper<String> analyzer, String file, ImgPreProc preProc, String sampleDir,
	                      File resultDir) {
		try {
			List<ImageFeature> features = preProc.process(ImageIO.read(new File(file)));

			List<ImageAnalyzerResult<String>> res = analyzer.findImage(features);

			System.out.println(file + " Results: " + res.size());
			int i = 1;

			if (resultDir.exists())
				for (File child : resultDir.listFiles())
					if (child.isFile()) child.delete();

			for (ImageAnalyzerResult<String> result : res) {
				System.out.println(result);

				if (resultDir.exists()) {
					String dir = sampleDir + "/" + result.getUserObj();
					Files.copy(Paths.get(dir), Paths.get(resultDir.getPath() + "/" + (i++) + " " + result.getUserObj()));
				}
			}
			System.out.println();
		} catch (Exception e) {
			log.error("searchImg failed: " + file, e);
		}

		System.gc();
	}


	private static String saveName ="E:\\project\\texture\\picsRep\\data";// SiftHelper.class.getSimpleName();

	static void saveFeaManager(ImageFeatureManager mgr) {
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(saveName))) {
			mgr.storeToStream(out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static ImageFeatureManager restoreFeaManager(SiftHelper.FeaMgrType feaMgrType) {
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(saveName))) {
			switch (feaMgrType) {
				case KDTree:
					return KDTreeManager.restoreFromStream(in);
				case Parallel:
					return ParallelManager.restoreFromStream(in);
			}
			throw new RuntimeException("unknown feaMgr type: " + feaMgrType);
		} catch (Exception e) {
			return null;
		}
	}

	static ImgPreProc genImgPreProc(int scaleLimit) {
		GrayScaleLimit preProc = new GrayScaleLimit();
		ImageFeatureExtractor sift = new JavaSIFT();
		preProc.setFe(sift);
		preProc.setScaleLimit(scaleLimit);
		return preProc;
	}
}
