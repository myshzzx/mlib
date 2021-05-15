package mysh.net.httpclient;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import mysh.collect.Colls;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mysh
 * @since 13-10-6 上午11:47
 */
// @Disabled
public class HttpClientAssistTest1 {
	HttpClientAssist hca = new HttpClientAssist();
	
	@Test
	public void demo() throws IOException {
		// create a header map from request head string
		Map<String, String> headers = HttpClientAssist.parseHeaders(
				"accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3\n" +
						"accept-encoding: gzip, deflate, br\n" +
						"accept-language: en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7\n" +
						"cookie: read_mode=day;\n" +
						"referer: https://www.jianshu.com/p/8c3d7fb09bce\n" +
						"upgrade-insecure-requests: 1\n" +
						"user-agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.90 Safari/537.36");
		
		// create a params map
		Map<String, String> params = Colls.ofHashMap(
				"key1", "v1",
				"key2", "v2"
		);
		
		// http://localhost/test?key1=v1&key2=v2
		try (HttpClientAssist.UrlEntity ue = hca.access("http://localhost/test", headers, params)) {
			String html = ue.getEntityStr();
			if (ue.isJson()) {
				JSONObject json = JSON.parseObject(html);
				System.out.println(json.getString("result"));
			}
		}
		
		// post to http://localhost/test with params
		try (HttpClientAssist.UrlEntity ue = hca.accessPostUrlEncodedForm("http://localhost/test", headers, params)) {
			System.out.println(ue.getEntityStr());
		}
	}
	
	@Test
	public void timeout() throws IOException {
		Assertions.assertThrows(SocketTimeoutException.class, () -> {
			HttpClientConfig hcc = new HttpClientConfig();
			hcc.setConnectionTimeout(20);
			HttpClientAssist hca = new HttpClientAssist(hcc);
			hca.access("http://baidu.com");
		});
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
		
		System.out.println(b.isImage());
	}
	
	@Test
	@Disabled
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
		Assertions.assertThrows(ConnectException.class, () -> {
			try (HttpClientAssist.UrlEntity bigFile =
					     hca.access("http://localhost/Adobe%20Acrobat%20XI.isz")) {
				System.out.println(bigFile.getContentLength());
			}
		});
	}
	
	@Test
	public void post() throws IOException {
		HttpClientConfig hcc = new HttpClientConfig();
		hcc.setKeepAlive(false);
		HttpClientAssist hca = new HttpClientAssist(hcc);
		ImmutableMap<String, String> params = ImmutableMap.of("k", "v");
		try (HttpClientAssist.UrlEntity ue = hca.accessPostMultipartForm("http://l:8080/abc?key=value", null, params)) {
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
		Assertions.assertEquals("http://dfso.com/faf/fe/a", HttpClientAssist.getShortURL("http://dfso.com//faf//fe/////a"));
		Assertions.assertEquals("http://dfso.com/faf/fe/a", HttpClientAssist.getShortURL("http://dfso.com//faf//fe/\\//a"));
		Assertions.assertEquals("http://dfso.com/a/", HttpClientAssist.getShortURL("http://dfso.com//./a/"));
		Assertions.assertEquals("http://dfso.com/b", HttpClientAssist.getShortURL("http://dfso.com//a/../b"));
		Assertions.assertEquals("http://dfso.com/b", HttpClientAssist.getShortURL("http://dfso.com//./a/../b"));
		Assertions.assertEquals("http://dfso.com/b", HttpClientAssist.getShortURL("http://dfso.com/\\./a/../b"));
		Assertions.assertEquals("http://dfso.com/b", HttpClientAssist.getShortURL("http://dfso.com\\./a/../b"));
		Assertions.assertEquals("http://dfso.com:84/b1", HttpClientAssist.getShortURL("http://dfso.com:84\\./a/../b1"));
		Assertions.assertEquals("http://dfso.com:84/b2", HttpClientAssist.getShortURL("http://dfso.com:84\\/./a/../b2"));
		Assertions.assertEquals("http://dfso.com:84/b3", HttpClientAssist.getShortURL("http://dfso.com:84\\/./a/../../b3"));
	}
	
}
