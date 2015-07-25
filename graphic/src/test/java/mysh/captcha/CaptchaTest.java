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
			String capText = captcha.createText(0);
			BufferedImage bi = captcha.createImage(capText);
			System.out.println(capText);
			ImageIO.write(bi, "jpg", out);
			Thread.sleep(3000);
		}
	}

	Captcha captcha = new Captcha(100, 45).setUseNoise(false);

	{
//		captcha.clearTextFonts();
//		captcha.addTextFont("宋体");
	}

	@Test
	public void benchmark() throws Exception {
		int n;
		int count = 3_000;
		BufferedImage bi = null;
		Tick tick = Tick.tick("captcha");

		n = count;
		tick.reset();
		while (n-- > 0)
			bi = captcha.renderWord("faiej");
		tick.nipAndPrint("renderWord");

		n = count;
		tick.reset();
		while (n-- > 0)
			captcha.makeNoise(bi, .1f, .1f, .9f, .9f);
		tick.nipAndPrint("makeNoise");

		n = count;
		tick.reset();
		while (n-- > 0)
			captcha.getDistortedImage(bi);
		tick.nipAndPrint("getDistortedImage");

		n = count;
		tick.reset();
		while (n-- > 0)
			captcha.addBackground(bi);
		tick.nipAndPrint("addBackground");

		tick.totalAndPrint();
	}
}
