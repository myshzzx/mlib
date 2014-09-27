package mysh.crawler2;

import mysh.net.httpclient.HttpClientAssist;

import java.io.Serializable;
import java.util.List;
import java.util.Queue;

/**
 * @author Mysh
 * @since 2014/9/24 21:07
 */
public interface CrawlerSeed extends Serializable {

	/**
	 * invoked by crawler immediately when crawler get this seed.
	 */
	default void init() {
	}

	/**
	 * url seeds.
	 */
	List<String> getSeeds();

	/**
	 * whether the url should be crawled NOW.
	 */
	boolean accept(String url);

	/**
	 * on get url entity.
	 * <p>
	 * WARNING: the urlEntity content may not be readable out of the method,
	 * because its inputStream will be closed after this invoking.
	 *
	 * @return whether fetch entity successfully. false indicate the entity need to be re-crawled.
	 */
	boolean onGet(HttpClientAssist.UrlEntity ue);

	/**
	 * invoked by crawler after crawler being completely stopped.
	 */
	default void onCrawlerStopped(Queue<String> unhandledSeeds) {
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
	default int requestThreadSize() {
		return 100;
	}

}
