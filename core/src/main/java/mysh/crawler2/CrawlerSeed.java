package mysh.crawler2;

import mysh.net.httpclient.HttpClientAssist;

import java.util.List;

/**
 * @author Mysh
 * @since 2014/9/24 21:07
 */
public interface CrawlerSeed {

	List<String> getSeeds();

	boolean isAcceptable(String url);

	void onGet(HttpClientAssist.UrlEntity ue);

	default boolean autoStop() {
		return true;
	}

	default CrawlerRepo getRepo() {
		return CrawlerRepo.getDefault();
	}

	default int requestThreadSize() {
		return 100;
	}

}
