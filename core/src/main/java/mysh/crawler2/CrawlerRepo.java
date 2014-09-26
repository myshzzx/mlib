package mysh.crawler2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mysh
 * @since 2014/9/24 21:08
 */
public interface CrawlerRepo {

	void put(String url);

	boolean contains(String url);

	void unhandledSeeds(Queue<String> seeds);

	static CrawlerRepo getDefault() {
		return new CrawlerRepo() {
			final Logger log = LoggerFactory.getLogger(CrawlerRepo.class);

			private Object v = new Object();
			private Map<String, Object> r = new ConcurrentHashMap<>(4000);

			@Override
			public void put(String url) {
				if (contains(url)) return;
				r.put(url, v);
			}

			@Override
			public boolean contains(String url) {
				return r.containsKey(url);
			}

			@Override
			public void unhandledSeeds(Queue<String> seeds) {
				log.debug("unhandledSeeds count: " + seeds.size());
			}
		};
	}
}
