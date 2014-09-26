package mysh.crawler2;

import mysh.net.httpclient.HttpClientAssist;
import mysh.net.httpclient.HttpClientConfig;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
	public void t0() throws InterruptedException, IOException {
		CrawlerSeed s = new CrawlerSeed() {
			@Override
			public List<String> getSeeds() {
				return Arrays.asList("http://www.cmd5.com/a");
			}

			@Override
			public boolean isAcceptable(String url) {
				return url.startsWith("http://www.cmd5.com/a");
			}

			@Override
			public void onGet(HttpClientAssist.UrlEntity e) {
			}
		};

		Crawler c = new Crawler(s, new HttpClientConfig());
		c.start();

		Thread.sleep(50000);
		c.stop();
	}

	@Test
	public void t1() throws InterruptedException {
		CrawlerSeed s = new CrawlerSeed() {
			@Override
			public List<String> getSeeds() {
				return Arrays.asList("http://g35driver.com/forums/lounge-off-topic/386248-official-hot-girls-w-glasses-thread-nsfw.html");
			}

			@Override
			public boolean isAcceptable(String url) {
				return url.startsWith("http://g35driver.com/forums/lounge-off-topic/386248-official-hot-girls-w-glasses-thread-nsfw")
								|| url.endsWith("jpg")
								|| url.endsWith("jpeg")
								|| url.endsWith("png")
								|| url.endsWith("gif")
								;
			}

			@Override
			public void onGet(HttpClientAssist.UrlEntity e) {
				if (e.isImage() && !e.getCurrentURL().contains("g35driver.com")) {
					File f = new File("l:/a", new File(e.getCurrentURL()).getName());
					try (OutputStream out = new FileOutputStream(f)) {
						e.bufWriteTo(out);
					} catch (Exception ex) {
						log.error("error download image: " + e.getCurrentURL(), ex);
					}
				}
			}

			@Override
			public int requestThreadSize() {
				return 20;
			}
		};

		Crawler c = new Crawler(s, new HttpClientConfig());
		c.start();

		Thread.sleep(120000000);
		System.out.println();
	}
}
