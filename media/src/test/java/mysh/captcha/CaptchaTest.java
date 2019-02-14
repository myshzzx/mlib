package mysh.captcha;

import mysh.util.Tick;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * CaptchaTest
 *
 * @author mysh
 * @since 2015/7/25
 */
@Ignore
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
	public void gen() throws IOException {
		String dir = "D:\\OtherProj\\JavaVerify\\download";
		int n = 100;
		while (n-- > 0) {
			String capText = captcha.createText(0);
			BufferedImage bi = captcha.createImage(capText);
			ImageIO.write(bi, "jpg", new File(dir, capText + ".jpg"));
		}
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
		System.out.println("renderWord " +tick.nip());

		n = count;
		tick.reset();
		while (n-- > 0)
			captcha.makeNoise(bi, .1f, .1f, .9f, .9f);
		System.out.println("makeNoise " +tick.nip());

		n = count;
		tick.reset();
		while (n-- > 0)
			captcha.getDistortedImage(bi);
		System.out.println("getDistortedImage " +tick.nip());

		n = count;
		tick.reset();
		while (n-- > 0)
			captcha.addBackground(bi);
		System.out.println("addBackground " +tick.nip());

		System.out.println(tick.nipsTotal());
	}
}
