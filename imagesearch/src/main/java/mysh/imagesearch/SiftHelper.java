package mysh.imagesearch;

import de.neotos.imageanalyzer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SIFT 图像搜索(线程安全).<br/>
 *
 * @author Mysh.
 * @since 13-12-23 上午11:19
 */
public class SiftHelper<UO> {
	private static final Logger log = LoggerFactory.getLogger(SiftHelper.class);

	public enum FeaMgrType {
		/**
		 * 使用 kd 树进行特征值搜索.
		 * 注意这里用的 kd 树实现使用 特征值->自定义对象 映射, 若两个图片存在某个相同的特征值,
		 * 后插入的 特征值->自定义对象 映射会失败.
		 */
		KDTree, Parallel
	}

	private static final int DIMENSION = 128;

	/**
	 * 最小匹配数.
	 */
	private volatile int minMatchCount = 2;

	/**
	 * 图像预处理.
	 */
	private ImgPreProc imgPreProc;

	/**
	 * 特征生成器.
	 */
	private ImageFeatureExtractor featuresExtractor = new JavaSIFT();

	/**
	 * 特征管理器.
	 */
	private ImageFeatureManager<UO> featuresManager;
	/**
	 * 保护 特征管理器 状态.
	 */
	private ReadWriteLock featuresLock = new ReentrantReadWriteLock();

	public SiftHelper(Class<UO> uoClass, FeaMgrType feaMgrType) {
		if (feaMgrType == FeaMgrType.KDTree)
			this.featuresManager = new KDTreeManager<>(DIMENSION);
		else
			this.featuresManager = new ParallelManager<>(DIMENSION, uoClass);
	}


	private List<ImageFeature> genFeatures(BufferedImage image) {
		if (this.imgPreProc != null)
			return this.imgPreProc.process(image);
		else
			return this.featuresExtractor.getFeatures(image);
	}

	/**
	 * bind image.
	 *
	 * @param key   custom key.
	 * @param image image.
	 */
	public void bindImage(UO key, BufferedImage image) {
		if (key == null || image == null)
			throw new NullPointerException(String.valueOf(key));

		this.bindImage(key, this.genFeatures(image));
	}

	/**
	 * bind image data.
	 *
	 * @param imageData image data.
	 * @param key       custom key.
	 * @throws IOException read image error.
	 */
	public void bindImage(UO key, byte[] imageData) throws IOException {
		InputStream in = new ByteArrayInputStream(imageData);
		BufferedImage img = ImageIO.read(in);
		this.bindImage(key, img);
	}

	/**
	 * bind imageFeatures.
	 *
	 * @param key      custom key.
	 * @param features features of the image.
	 */
	public void bindImage(UO key, List<ImageFeature> features) {
		if (key == null || features == null)
			throw new NullPointerException();

		this.featuresLock.writeLock().lock();
		try {
			this.featuresManager.putFeatures(features, key);
		} finally {
			this.featuresLock.writeLock().unlock();
		}
	}

	/**
	 * 移除数据. <br/>
	 * 注意这方法会导致内存泄露, 是因为 KDTree 的 remove 方法不是从树上移除节点, 而是标记删除.
	 *
	 * @see de.neotos.imageanalyzer.KDTreeManager#removeFeatures(Object)
	 * @see edu.wlu.cs.levy.CG.KDTree#delete(float[])
	 */
	public void unbindImage(UO key) {
		this.featuresLock.writeLock().lock();
		try {
			this.featuresManager.removeFeatures(key);
		} finally {
			this.featuresLock.writeLock().unlock();
		}
	}


	/**
	 * 搜索最匹配的图片.
	 *
	 * @throws InterruptedException 搜索超时.
	 */
	public List<ImageAnalyzerResult<UO>> findImage(BufferedImage image) throws InterruptedException {

		return this.findImage(this.genFeatures(image));
	}


	/**
	 * 搜索最匹配的图片.
	 *
	 * @throws InterruptedException 搜索超时.
	 */
	public List<ImageAnalyzerResult<UO>> findImage(byte[] image) throws IOException, InterruptedException {
		BufferedImage img = ImageIO.read(new ByteArrayInputStream(image));
		return this.findImage(img);
	}

	/**
	 * 搜索最匹配的图片.
	 *
	 * @throws InterruptedException 搜索超时.
	 */
	public List<ImageAnalyzerResult<UO>> findImage(List<ImageFeature> features) throws InterruptedException {
		List<ImageAnalyzerResult<UO>> res;
		this.featuresLock.readLock().lock();
		try {
			long start = System.currentTimeMillis();
			log.debug("Begin to search similiar Images...");
			res = this.featuresManager.findMatches(features, this.minMatchCount);
			log.debug("... took " + (System.currentTimeMillis() - start) + " ms");
		} finally {
			this.featuresLock.readLock().unlock();
		}

		Collections.sort(res);
		return res;
	}


	public ImageFeatureManager getFeatureManager() {
		return this.featuresManager;
	}

	public ImageFeatureExtractor getFeaturesExtractor() {
		return this.featuresExtractor;
	}

	public void setMinMatchCount(int minCount) {
		this.minMatchCount = minCount;
	}

	public void setImgPreProc(ImgPreProc imgPreProc) {
		this.imgPreProc = imgPreProc;
	}

	public void setFeaturesManager(ImageFeatureManager<UO> featuresManager) {
		this.featuresManager = featuresManager;
	}

	public void setFeaturesExtractor(ImageFeatureExtractor featuresExtractor) {
		this.featuresExtractor = featuresExtractor;
	}

	public void setFindMatchesTimeoutMin(int findMatchesTimeout) {
		this.featuresManager.setEachFeatureTimeout(findMatchesTimeout);
	}
}
