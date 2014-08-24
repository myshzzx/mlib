package mysh.imagesearch;

import de.neotos.imageanalyzer.ImageFeature;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * 图像预处理.
 *
 * @author Mysh
 * @since 14-1-3 下午2:32
 */
public interface ImgPreProc {
	List<ImageFeature> process(BufferedImage img);
}
