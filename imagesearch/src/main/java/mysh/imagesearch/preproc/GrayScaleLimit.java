package mysh.imagesearch.preproc;

import mysh.imagesearch.ImgPreProc;
import de.neotos.imageanalyzer.ImageFeature;
import de.neotos.imageanalyzer.ImageFeatureExtractor;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.util.List;

/**
 * 灰化后同比例缩小.
 *
 * @author Mysh
 * @since 14-1-3 下午2:35
 */
public class GrayScaleLimit implements ImgPreProc {

	private int scaleLimit = 180;
	private ImageFeatureExtractor fe;


	@Override
	public List<ImageFeature> process(BufferedImage img) {
		return fe.getFeatures(this.grayScaleImg(img));
	}

	Image grayScaleImg(BufferedImage img) {
		int w = img.getWidth(null);
		int h = img.getHeight(null);
		double ratio = w > h ? scaleLimit * 1.0 / w : scaleLimit * 1.0 / h;

		ColorConvertOp colorFilter = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

		if (ratio > 1)
			return colorFilter.filter(img, null);
		else
			return colorFilter.filter(img, null)
							.getScaledInstance((int) (w * ratio), (int) (h * ratio), Image.SCALE_AREA_AVERAGING);
	}

	/**
	 * 设置长宽像素尺度上限.
	 * default={@link #scaleLimit}
	 */
	public void setScaleLimit(int scaleLimit) {
		this.scaleLimit = scaleLimit;
	}

	public void setFe(ImageFeatureExtractor fe) {
		this.fe = fe;
	}
}
