package mysh.captcha;

import mysh.util.Tick;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * CaptchaTest
 *
 * @author mysh
 * @since 2015/7/25
 */
public class CaptchaTest {

	@Test
	public void common() throws Exception {
		File out = new File("l:/a.jpg");
		while (true) {
			String capText = captcha.createText(4);
			BufferedImage bi = captcha.createImage(capText);
			System.out.println(capText);
			ImageIO.write(bi, "jpg", out);
			Thread.sleep(3000);
		}
	}

	Captcha captcha = new Captcha(200, 65);

	{
		captcha.clearTextFonts();
		captcha.addTextFont("宋体");
	}

	@Test
	public void benchmark() throws Exception {
		int n = 1;
		String text = captcha.createText(4);

		Tick tick = Tick.tick("captcha");
		while (n-- > 0)
			captcha.createImage(text);
		tick.nipAndPrint();
	}
}
