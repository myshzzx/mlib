package mysh.net.httpclient;

import com.google.common.collect.ImmutableMap;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

/**
 * @author Mysh
 * @since 13-10-6 上午11:47
 */
@Ignore
public class HttpClientAssistTest1 {
	HttpClientAssist hca = new HttpClientAssist();

	@Test
	public void timeout() throws IOException {
		HttpClientConfig hcc = new HttpClientConfig();
		hcc.setConnectionTimeout(20);
		HttpClientAssist hca = new HttpClientAssist(hcc);
		hca.access("http://baidu.com");
	}

	@Test
	public void httpsTest() throws Exception {
		String url = "https://raw.github.com/myshzzx/misc/master/lecai.update";
		HttpClientAssist hca = new HttpClientAssist(new HttpClientConfig());
		HttpClientAssist.UrlEntity page = hca.access(url);
		System.out.println(page.getCurrentURL());
	}

	@Test
	public void currentUrlTest() throws Exception {
		try (HttpClientAssist.UrlEntity ue = hca.access("http://baidu.com")) {
			System.out.println(ue.getCurrentURL());
			System.out.println(ue.getEntityStr());
		}
	}

	@Test
	public void reuseHcTest() throws InterruptedException, IOException {
		HttpClientConfig conf = new HttpClientConfig();
		conf.setKeepAlive(false);
		HttpClientAssist hca = new HttpClientAssist(conf);
		Thread.sleep(5000);
		System.out.println(hca.access("http://huodong.maimai.taobao.com/go/activities/cdn/page/1000187206/82579/viewPage.php?spm=a217r.7278833.1997593533-0.3.VEXuvq").getCurrentURL());
		Thread.sleep(3000);
		System.out.println(hca.access("http://baidu.cn").getCurrentURL());
		Thread.sleep(3000);
		System.out.println(hca.access("http://huodong.maimai.taobao.com/go/activities/cdn/page/1000187143/82680/viewPage.php?spm=a217r.7278833.1997593533-0.17.VEXuvq").getCurrentURL());
		Thread.sleep(3000);
		System.out.println(hca.access("http://huodong.maimai.taobao.com/go/activities/cdn/page/1000187219/82588/viewPage.php?spm=a217r.7278833.1997593533-0.7.VEXuvq").getCurrentURL());
		Thread.sleep(100000);
	}

	@Test
	public void content() throws Exception {
		HttpClientConfig conf = new HttpClientConfig();
//		conf.setKeepAlive(false);
		HttpClientAssist hca = new HttpClientAssist(conf);

		HttpClientAssist.UrlEntity b = hca.access("http://ec4.images-amazon.com/images/I/41y4f-S4vpL._AC_SS150_.jpg");
		HttpClientAssist.UrlEntity z = hca.access("http://z.cn");
		HttpClientAssist.UrlEntity z2 = hca.access("http://z.cn");

		String zs2 = z2.getEntityStr();
		String zs = z.getEntityStr();
		String bs = b.getEntityStr();

		System.out.println();
	}

	@Test
	public void multiOps() throws IOException, InterruptedException {
		while (true) {
			try (HttpClientAssist.UrlEntity z = hca.access("http://baidu.com/")) {
				System.out.println(z.isHtml());
				z.getEntityStr();
			}
			Thread.sleep(3000);
		}
	}

	@Test
	public void entityReadTest() throws IOException, InterruptedException {
		try (HttpClientAssist.UrlEntity bigFile =
						     hca.access("http://localhost/Adobe%20Acrobat%20XI.isz")) {
			System.out.println(bigFile.getContentLength());
		}
	}


	@Test
	public void post() throws IOException {
		HttpClientConfig hcc = new HttpClientConfig();
		hcc.setKeepAlive(false);
		HttpClientAssist hca = new HttpClientAssist(hcc);
		ImmutableMap<String, String> params = ImmutableMap.of("k", "v");
		try (HttpClientAssist.UrlEntity ue = hca.accessPost("http://l:8080/abc?key=value", null, params)) {
			System.out.println(ue.getReqUrl());
			System.out.println(ue.getCurrentURL());
		}
	}

	@Test
	public void urlTest2() throws IOException, InterruptedException {
		try (HttpClientAssist.UrlEntity ue = hca.access("http://codemacro.com/2014/10/12/diamond/")) {
			Pattern srcValueExp =
							Pattern.compile("(([Hh][Rr][Ee][Ff])|([Ss][Rr][Cc]))[\\s]*=[\\s]*[\"']([^\"'#]*)");
			System.out.println(ue.getEntityStr());
			System.out.println("==================");
			Matcher srcValueMatcher = srcValueExp.matcher(ue.getEntityStr());
			while (srcValueMatcher.find()) {
				System.out.println(srcValueMatcher.group(4));
			}
		}
	}

	@Test
	public void testGetShortURL() throws Exception {
		assertEquals("http://dfso.com/faf/fe/a", HttpClientAssist.getShortURL("http://dfso.com//faf//fe/////a"));
		assertEquals("http://dfso.com/a/", HttpClientAssist.getShortURL("http://dfso.com//./a/"));
		assertEquals("http://dfso.com/b", HttpClientAssist.getShortURL("http://dfso.com//a/../b"));
		assertEquals("http://dfso.com/b", HttpClientAssist.getShortURL("http://dfso.com//./a/../b"));
		assertEquals("http://dfso.com/b", HttpClientAssist.getShortURL("http://dfso.com/\\./a/../b"));
		assertEquals("http://dfso.com/b", HttpClientAssist.getShortURL("http://dfso.com\\./a/../b"));
		assertEquals("http://dfso.com:84/b", HttpClientAssist.getShortURL("http://dfso.com:84\\./a/../b"));
	}
}
