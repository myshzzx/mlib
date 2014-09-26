package mysh.crawler2;

import mysh.net.httpclient.HttpClientAssist;
import mysh.net.httpclient.HttpClientConfig;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * @author Mysh
 * @since 2014/9/25 15:37
 */
public class CrawlerTest {
	private static final Logger log = LoggerFactory.getLogger(CrawlerTest.class);

	@Test
	public void t1() throws InterruptedException {
		List<String> blockDomain = Arrays.asList("blogspot.com", "wordpress.com");

		CrawlerSeed s = new CrawlerSeed() {

			@Override
			public List<String> getSeeds() {
				return Arrays.asList("http://boards.vinylcollective.com/topic/73446-ass-appreciation-thread/");
			}

			@Override
			public boolean accept(String url) {
				return blockDomain.stream().filter(url::contains).count() == 0
								&& (
								url.startsWith("http://boards.vinylcollective.com/topic/73446-ass-appreciation-thread/")
												|| url.endsWith("jpg")
												|| url.endsWith("jpeg")
												|| url.endsWith("png")
												|| url.endsWith("gif")
				);
			}

			@Override
			public boolean onGet(HttpClientAssist.UrlEntity e) {
				try {
					if (e.isImage() && e.getContentLength() > 15_000) {
						File f = new File("l:/a", new File(e.getCurrentURL()).getName());
						try (OutputStream out = new FileOutputStream(f)) {
							e.bufWriteTo(out);
						} catch (Exception ex) {
							log.error("下载失败: " + e.getCurrentURL(), ex);
							return false;
						}
					}
					return true;
				} finally {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
					}
				}
			}

			@Override
			public int requestThreadSize() {
				return 40;
			}
		};

		HttpClientConfig hcc = new HttpClientConfig();
		hcc.setMaxConnPerRoute(5);
		hcc.setUseProxy(true);
		hcc.setProxyHost("127.0.0.1");
		hcc.setProxyPort(8087);

		Crawler c = new Crawler(s, hcc);
		c.start();

		while (c.getStatus() == Crawler.Status.RUNNING) {
			Thread.sleep(5000);
		}

		Thread.sleep(1000000);
	}

}
