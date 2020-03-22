package mysh.image;

import mysh.util.FilesUtil;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.processing.resize.BilinearInterpolation;

import java.io.File;
import java.io.IOException;

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
	
}
