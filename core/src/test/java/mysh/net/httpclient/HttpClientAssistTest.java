package mysh.net.httpclient;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Mysh
 * @since 13-10-6 上午11:47
 */
@Ignore
public class HttpClientAssistTest {
	HttpClientAssist hca = new HttpClientAssist(new HttpClientConfig());

	@Test
	public void httpsTest() throws Exception {
		String url = "https://raw.github.com/myshzzx/misc/master/lecai.update";
		HttpClientAssist hca = new HttpClientAssist(new HttpClientConfig());
		HttpClientAssist.UrlEntity page = hca.access(url);
		System.out.println(page.getCurrentURL());
	}

	@Test
	public void currentUrlTest() throws Exception {
		HttpClientConfig conf = new HttpClientConfig();
//		conf.setUseProxy(true);
		conf.setProxyHost("l");
		conf.setProxyPort(8080);
		HttpClientAssist hca = new HttpClientAssist(conf);
		long start = System.nanoTime();
		HttpClientAssist.UrlEntity ue = hca.access("http://baidu.com");
		start = System.nanoTime() - start;
		System.out.println(ue.getCurrentURL());
		System.out.println(ue.getEntityStr());
		System.out.println(start / 1000_000);
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
	public void reGetStr() throws IOException, InterruptedException {
		HttpClientAssist.UrlEntity ue = hca.access("http://baidu.com");
		System.out.println(ue.getEntityStr());
		System.out.println(ue.getEntityStr());
	}

	@Test
	public void exec() throws InterruptedException {
		ExecutorService e = Executors.newCachedThreadPool();
		e.execute(() -> {
			try {
				Thread.sleep(1000000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		});
		e.shutdown();

		Thread.sleep(10000);
	}

	@Test
	public void exec2() {
		ExecutorService e = Executors.newFixedThreadPool(1);
		e.execute(() -> {
			try {
				Thread.sleep(100000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		});
		e.execute(() -> {
			try {
				Thread.sleep(100000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		});
		System.out.println();
	}

	@Test
	public void multiOps() throws IOException, InterruptedException {
		while (true) {
			Thread.sleep(5000);
			HttpClientAssist.UrlEntity z = hca.access("http://baidu.com");
			System.out.println(z.isHtml());
			z.close();
//			Thread.sleep(2000);
//			z.getEntityStr();
		}

	}
}
