package mysh.image;

import mysh.util.FilesUtil;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.processing.resize.BilinearInterpolation;

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
 * @since 2019/1/26
 */
public class Images {
	public static MBFImage scale(MBFImage in, float scale) {
		int width = (int) (in.getWidth() * scale);
		int height = (int) (in.getHeight() * scale);
		BilinearInterpolation resize = new BilinearInterpolation(width, height, 1 / scale);
		return in.process(resize).normalise();
	}
	
	public static MBFImage read(File imgFile) throws IOException {
		return ImageUtilities.readMBF(imgFile);
	}
	
	public static void write(MBFImage img, File dstFile) throws IOException {
		File writeFile = FilesUtil.getWriteFile(dstFile);
		writeFile = new File(writeFile.getAbsolutePath() + "." + FilesUtil.getFileExtension(dstFile));
		ImageUtilities.write(img, writeFile);
		dstFile.delete();
		writeFile.renameTo(dstFile);
	}
	
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
