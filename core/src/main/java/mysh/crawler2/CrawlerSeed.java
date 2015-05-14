package mysh.crawler2;

import mysh.net.httpclient.HttpClientAssist;

import java.io.Serializable;
import java.util.Queue;
import java.util.stream.Stream;

/**
 * Crawler seed, where crawler start from.
 *
 * @author Mysh
 * @since 2014/9/24 21:07
 */
public interface CrawlerSeed<CTX extends UrlContext> extends Serializable {

	/**
	 * invoked by crawler immediately when crawler get this seed.
	 */
	default void init() throws Exception {
	}

	/**
	 * url seeds.
	 */
	Stream<UrlCtxHolder<CTX>> getSeeds();

	/**
	 * whether the url & ctx should be crawled NOW.
	 */
	boolean accept(String url, CTX ctx);

	/**
	 * on get url entity.
	 * <p>
	 * WARNING: the urlEntity content may not be readable out of the method,
	 * because its inputStream will be closed after this invoking, but cached content is still accessible.
	 *
	 * @return whether fetch the entity successfully. <code>false</code> indicate the entity
	 * need to be re-crawled.
	 */
	boolean onGet(HttpClientAssist.UrlEntity ue, CTX ctx);

	/**
	 * invoked by crawler after crawler being completely stopped.
	 */
	default void onCrawlerStopped(Queue<UrlCtxHolder<CTX>> unhandledTasks) {
	}

	/**
	 * make the crawler auto stop.
	 */
	default boolean autoStop() {
		return true;
	}

	/**
	 * crawler thread pool size.
	 */
	int requestThreadSize();

	/**
	 * whether the urls in UrlEntity(text content) needs to be distilled.
	 */
	default boolean needToDistillUrls(HttpClientAssist.UrlEntity ue, CTX ctx) {
		return true;
	}

	/**
	 * after distilling urls from UrlEntity(text content), the results can be re-handled.
	 */
	default Stream<String> afterDistillingUrls(
					HttpClientAssist.UrlEntity ue, CTX ctx, Stream<String> distilledUrls) {
		return distilledUrls;
	}

	/**
	 * add custom url context to each url if you need.
	 */
	default Stream<UrlCtxHolder<CTX>> distillUrlCtx(
					HttpClientAssist.UrlEntity parentUe, CTX parentCtx, Stream<String> distilledUrls) {
		return distilledUrls.map(url -> new UrlCtxHolder<>(url, parentCtx));
	}
}
