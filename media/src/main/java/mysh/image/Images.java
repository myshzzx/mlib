package mysh.image;

import org.openimaj.image.MBFImage;
import org.openimaj.image.processing.resize.BilinearInterpolation;

/**
 * Images
 *
 * @author mysh
 * @since 2019/1/26
 */
public class Images {
	public static MBFImage scale(MBFImage in, float scale) {
		int width = (int) (in.getWidth() * scale);
		int height = (int) (in.getHeight() * scale);
		BilinearInterpolation resize = new BilinearInterpolation(width, height, 1 / scale);
		return in.process(resize).normalise();
	}
}
