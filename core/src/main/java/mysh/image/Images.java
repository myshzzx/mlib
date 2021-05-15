package mysh.image;

import mysh.util.FilesUtil;
import mysh.util.Try;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.processing.resize.BilinearInterpolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.imageio.*;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Images
 *
 * @author mysh
 * @since 2019/1/26
 */
public class Images {
	private static final Logger log = LoggerFactory.getLogger(Images.class);
	
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
	
	/**
	 * compress using original format.
	 *
	 * @param quality [0,1], <code>0</code> represent to "most compressed", while <code>1</code> represent to best quality
	 * @see ImageWriteParam#setCompressionQuality(float)
	 */
	public static void compressImg(File source, File target, float quality) throws IOException {
		ImageReader reader = null;
		ImageWriter writer = null;
		File writeFile = FilesUtil.getWriteFile(target);
		writeFile.getParentFile().mkdirs();
		try (ImageInputStream in = ImageIO.createImageInputStream(source);
		     ImageOutputStream out = ImageIO.createImageOutputStream(writeFile)) {
			reader = ImageIO.getImageReaders(in).next();
			reader.setInput(in);
			writer = ImageIO.getImageWriter(reader);
			writer.setOutput(out);
			
			BufferedImage bufferedImage = reader.read(0);
			IIOImage image = new IIOImage(bufferedImage, null, reader.getImageMetadata(0));
			
			ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(quality);
			if (param instanceof JPEGImageWriteParam) {
				((JPEGImageWriteParam) param).setOptimizeHuffmanTables(true);
			}
			
			writer.write(null, image, param);
			
			if (writeFile.exists() && writeFile.length() > 0) {
				target.delete();
				writeFile.renameTo(target);
			}
		} catch (IOException e) {
			writeFile.delete();
			throw e;
		} finally {
			if (reader != null)
				reader.dispose();
			if (writer != null)
				writer.dispose();
		}
	}
	
	/**
	 * @param handler (src, dst)->{}
	 */
	public static void handleFolderImg(
			File sourceDir, @Nullable Predicate<Path> fileFilter, File targetDir, Try.ExpBiConsumer<File, File, Exception> handler) throws Exception {
		ExecutorService ex = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		Path src = sourceDir.toPath();
		Files.walk(src)
		     .filter(p -> Files.isRegularFile(p))
		     .filter(fileFilter == null ? p -> true : fileFilter)
		     .forEach(p -> {
			     ex.execute(() -> {
				     try {
					     File file = new File(targetDir, src.relativize(p).toString());
					     handler.accept(p.toFile(), file);
				     } catch (Exception e) {
					     log.error("handle fail, p={}", p.toString(), e);
				     }
			     });
		     });
		ex.shutdown();
		ex.awaitTermination(1, TimeUnit.HOURS);
	}
	
	public static void convertToJpg(File source, File target) throws IOException {
		BufferedImage image = ImageIO.read(source);
		BufferedImage result = new BufferedImage(
				image.getWidth(),
				image.getHeight(),
				BufferedImage.TYPE_INT_RGB);
		result.createGraphics().drawImage(image, 0, 0, Color.WHITE, null);
		ImageIO.write(result, "jpg", target);
	}
}
