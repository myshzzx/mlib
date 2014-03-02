package mysh.imagesearch.preproc;

import mysh.imagesearch.ImgPreProc;
import de.neotos.imageanalyzer.ImageFeature;
import de.neotos.imageanalyzer.ImageFeatureExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.util.List;

/**
 * 图像灰化并缩小, 然后取特征值, 使返回的特征值数量在给定范围内.
 * 不建议使用, 因为对于内容复杂的图片, 给定特征值数量上限会使图片重要信息丢失.
 *
 * @author Mysh
 * @since 13-12-30 下午2:00
 */
public class GrayScaleFeaturesLimit implements ImgPreProc {
	private static final Logger log = LoggerFactory.getLogger(GrayScaleFeaturesLimit.class);

	private ImageFeatureExtractor fe;
	private int fLimit = 250;
	private int initPx = 150;

	public List<ImageFeature> process(BufferedImage img) {

		ColorConvertOp colorFilter = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
		BufferedImage grayImg = colorFilter.filter(img, null);

		int w = img.getWidth(null);
		int h = img.getHeight(null);

		double ratio;
		if (initPx > w && initPx > h)
			ratio = 1;
		else
			ratio = w > h ? initPx * 1.0 / w : initPx * 1.0 / h;

		List<ImageFeature> features;
		int fSize;
		while (true) {
			features = fe.getFeatures(
							grayImg.getScaledInstance((int) (w * ratio), (int) (h * ratio), Image.SCALE_AREA_AVERAGING));
			fSize = features.size();
			if (fSize < fLimit) break;

			ratio /= 1.2;
		}
		log.debug((int) (w * ratio) + "*" + (int) (h * ratio) + ", features: " + features.size());

		return features;
	}

	/**
	 * 特征提取器.
	 */
	public void setFe(ImageFeatureExtractor fe) {
		this.fe = fe;
	}

	/**
	 * 特征值数量上限.
	 * default={@link #fLimit}.
	 */
	public void setfLimit(int fLimit) {
		if (fLimit < 10 || fLimit > 10000) throw new IllegalArgumentException();
		this.fLimit = fLimit;
	}

	/**
	 * 起始像素尺度. 第一次检查缩小到这个尺度来检查特征值数量. 一般图片建议 150px.
	 * default={@link #initPx}.
	 */
	public void setInitPx(int initPx) {
		if (initPx < 10) throw new IllegalArgumentException();
		this.initPx = initPx;
	}
}
