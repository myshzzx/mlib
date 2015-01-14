package mysh.ui;

import junit.framework.TestCase;

public class CaptchaGetterTest extends TestCase {

	public void testGetCaptcha() throws Exception {
		CaptchaGetter cg = new CaptchaGetter("http://www.renrendai.com/image_https.jsp", null, null, null);
		System.out.println(cg.getCaptcha());
	}
}
