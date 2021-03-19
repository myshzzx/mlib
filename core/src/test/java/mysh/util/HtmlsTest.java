package mysh.util;

import mysh.collect.Colls;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * HtmlsTest
 *
 * @author mysh
 * @since 2016-09-07
 */
public class HtmlsTest extends Assertions {
	@Test
	public void urlDecode() throws Exception {
		System.out.println(Htmls.urlDecode("%2C1200&", Encodings.GBK));
	}
	
	@Test
	public void urlEncode() throws Exception {
		System.out.println(Htmls.urlEncode(",1200&", "gbk"));
	}
	
	@Test
	public void parseQuery() {
		Map<String, String> params = Htmls.parseQuery("k1=v1&k2=v2%20v2&k3=v3%26%20v3", Encodings.UTF_8);
		assertEquals(Colls.ofHashMap("k1", "v1", "k2", "v2 v2", "k3", "v3& v3"), params);
	}
	
	@Test
	public void getMimeType() {
		assertEquals("text/html", Htmls.getMimeType("html", Htmls.MIME_TEXT));
		assertEquals(Htmls.MIME_TEXT, Htmls.getMimeType("abc", Htmls.MIME_TEXT));
	}
}
