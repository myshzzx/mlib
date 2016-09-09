package mysh.util;

import org.junit.Test;

/**
 * HtmlsTest
 *
 * @author mysh
 * @since 2016-09-07
 */
public class HtmlsTest {
	@Test
	public void urlDecode() throws Exception {
		System.out.println(Htmls.urlDecode("%2C1200&","gbk"));
	}
	@Test
	public void urlEncode() throws Exception {
		System.out.println(Htmls.urlEncode(",1200&","gbk"));
	}

}
