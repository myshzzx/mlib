package mysh.ui;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class CaptchaGetterTest {

	@Test
	public void testGetCaptcha() throws Exception {
		CaptchaGetter cg = new CaptchaGetter("http://www.renrendai.com/image_https.jsp", null, null, null);
		System.out.println(cg.getCaptcha());
	}
}
