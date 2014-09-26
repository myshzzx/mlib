package mysh.crawler2;

import mysh.net.httpclient.HttpClientAssist;

import java.util.List;

/**
 * @author Mysh
 * @since 2014/9/24 21:07
 */
public interface CrawlerSeed {

	/**
	 * url seeds.
	 */
	List<String> getSeeds();

	/**
	 * does the url should be crawled.
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
	 * make the crawler auto stop.
	 */
	default boolean autoStop() {
		return true;
	}

	default CrawlerRepo getRepo() {
		return CrawlerRepo.getDefault();
	}

	/**
	 * crawler thread pool size.
	 */
	default int requestThreadSize() {
		return 100;
	}

}
