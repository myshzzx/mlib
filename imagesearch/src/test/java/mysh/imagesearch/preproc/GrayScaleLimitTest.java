package mysh.imagesearch.preproc;

import de.neotos.imageanalyzer.JavaSIFT;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author Mysh
 * @since 14-1-7 下午2:59
 */
@Ignore
public class GrayScaleLimitTest {
	@Test
	public void testGrayScaleImg() throws Exception {

		String file = "L:\\STR00151-1.jpg";

		GrayScaleLimit gs = new GrayScaleLimit();
		gs.setFe(new JavaSIFT());

		Image image = gs.grayScaleImg(ImageIO.read(new File(file)));
		BufferedImage bufImg = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_BGR);
		bufImg.getGraphics().drawImage(image, 0, 0, null);
		File img = File.createTempFile(getClass().getSimpleName(), ".png");
		ImageIO.write(bufImg, "png", img);
		Desktop.getDesktop().open(img);
	}
}
