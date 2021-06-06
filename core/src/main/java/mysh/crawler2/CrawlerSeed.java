package mysh.crawler2;

import mysh.net.httpclient.HttpClientAssist;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
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
	 * before access url. the urlCtxHolder is mutable.
	 */
	default void beforeAccess(UrlCtxHolder<CTX> urlCtxHolder) {
	}
	
	/**
	 * whether the url & ctx should be crawled NOW.<br/>
	 * need a EFFICIENT implementation.
	 *
	 * @param url analyzed url, e.g. <b>/../a</b> in page will be transformed to <b>http://root/a</b>
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
	 * @throws Exception if an exception thrown, the task will be pushed to unhandled tasks,
	 *                   and will not be recrawled in this run.
	 */
	boolean onGet(HttpClientAssist.UrlEntity ue, CTX ctx);
	
	/**
	 * invoked by crawler after crawler being completely stopped.
	 */
	default void onCrawlerStopped(Collection<UrlCtxHolder<CTX>> unhandledTasks) {
	}
	
	/**
	 * make the crawler auto stop (when tasks finished).
	 */
	default boolean autoStop() {
		return true;
	}
	
	/**
	 * whether the urls in UrlEntity(text content) needs to be distilled.
	 */
	default boolean needToDistillUrls(HttpClientAssist.UrlEntity ue, CTX ctx) {
		return true;
	}
	
	/**
	 * besides http url, src/href properties, user can distill extended url/uri by special rules
	 */
	@Nullable
	default Stream<String> enhancedDistillUrl(HttpClientAssist.UrlEntity ue, CTX ctx) {
		return null;
	}
	
	/**
	 * after distilling urls from UrlEntity(text content),
	 * you can filter urls you need, and attach custom url context to each url.
	 * <p>
	 * to change crawling priority, rewrite {@link UrlContext#compareTo}
	 */
	default Stream<UrlCtxHolder<CTX>> afterDistillingUrls(
			HttpClientAssist.UrlEntity parentUe, CTX parentCtx, Stream<String> distilledUrls) throws IOException {
		return distilledUrls.map(url -> new UrlCtxHolder<>(url, parentCtx));
	}
	
	default String winFileNameEscape(String fileName, String replacer) {
		return fileName.replaceAll("[\\\\/:*?\"><|]", replacer);
	}
	
	default boolean isNetworkIssue(Throwable t) {
		return Crawler.isNetworkIssue(t);
	}
	
}
