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
	 * after distilling urls from UrlEntity(text content),
	 * you can filter urls you need, and attach custom url context to each url.
	 */
	default Stream<UrlCtxHolder<CTX>> afterDistillingUrls(
					HttpClientAssist.UrlEntity parentUe, CTX parentCtx, Stream<String> distilledUrls) {
		return distilledUrls.map(url -> new UrlCtxHolder<>(url, parentCtx));
	}

	/**
	 * return html title (encapsulated by "title" tag).
	 * return blank string if fails.
	 */
	default String getHtmlTitle(String html) {
		int from = html.indexOf("<title>");
		if (from < 0) return "";
		int end = html.indexOf("</title>", from);
		if (end < 0) return "";
		return unEscapeXml(html.substring(from + 7, end));
	}

	/**
	 * unescape xml
	 */
	default String unEscapeXml(String xml) {
		return xml
						.replace("&amp;", "&")
						.replace("&lt;", "<")
						.replace("&gt;", ">")
						.replace("&quot;", "\"")
						.replace("&apos;", "'");
	}

	default String winFileNameEscape(String fileName, String replacer) {
		return fileName.replaceAll("[\\\\/:*?\"><|]", replacer);
	}
}
