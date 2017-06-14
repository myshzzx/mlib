package mysh.util;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Images
 *
 * @author mysh
 * @since 2017/5/19
 */
public abstract class Images {

	/**
	 * check image file type in lower case name, such as jpeg/png/gif
	 *
	 * @throws IOException see {@link javax.imageio.ImageIO#createImageInputStream(java.lang.Object)}
	 */
	public static String imgType(File file) throws IOException {

		// create an image input stream from the specified file
		try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {

			// get all currently registered readers that recognize the image format
			Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);

			if (!iter.hasNext()) {
				return null;
			}

			// get the first reader
			ImageReader reader = iter.next();
			return reader.getFormatName().toLowerCase();
		}
	}
}
