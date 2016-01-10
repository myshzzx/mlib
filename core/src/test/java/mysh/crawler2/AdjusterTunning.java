package mysh.crawler2;

import mysh.net.httpclient.HttpClientAssist;
import mysh.net.httpclient.HttpClientConfig;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

/**
 * AdjusterTest
 *
 * @author mysh
 * @since 2016/1/10
 */
public class AdjusterTunning implements CrawlerSeed<UrlContext> {

	public static void main(String[] args) throws Exception {
		Crawler<UrlContext> c = new Crawler<>(new AdjusterTunning(), new HttpClientConfig(), 300);
		c.start();
		new CountDownLatch(1).await();
	}

	@Override
	public Stream<UrlCtxHolder<UrlContext>> getSeeds() {
		return UrlCtxHolder.ofAll(Arrays.asList("http://localhost/limit.jsp").stream());
	}

	@Override
	public boolean accept(String url, UrlContext urlContext) {
		return true;
	}

	@Override
	public boolean onGet(HttpClientAssist.UrlEntity ue, UrlContext urlContext) {
		return true;
	}
}
