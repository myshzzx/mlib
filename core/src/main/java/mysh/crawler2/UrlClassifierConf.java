package mysh.crawler2;

import mysh.net.httpclient.HttpClientAssist;

import java.util.Objects;

/**
 * url classifier config. <br/>
 * used to gen url classifier, should be reused, because every config is related to a unique url
 * classifier which is expensive to create.
 * NOTE: the class should not be Serializable, because hca will refer to uncontrollable resources.
 */
public final class UrlClassifierConf {

	public interface Factory<CTX extends UrlContext> {
		UrlClassifierConf get(String url, CTX ctx);
	}

	public interface BlockChecker {
		/**
		 * whether access is blocked.
		 * see <a href="http://zh.wikipedia.org/zh-cn/HTTP%E7%8A%B6%E6%80%81%E7%A0%81">status code</a>
		 */
		boolean isBlocked(HttpClientAssist.UrlEntity ue);
	}

	/**
	 * max accurate flow control per minute.
	 */
	static final int maxAccRatePM = 60_000;

	final String name;
	final int threadPoolSize;
	final int ratePerMinute;
	final HttpClientAssist hca;
	volatile boolean useAdjuster;
	volatile BlockChecker blockChecker;


	/**
	 * @param name           the name of url classifier.
	 * @param threadPoolSize thread pool size of the classifier.
	 * @param ratePerMinute  flow control. the max accurate controllable value is {@link #maxAccRatePM}.
	 *                       see {@link mysh.crawler2.Crawler.ClassifiedUrlCrawler#setRatePerMinute(int)}
	 * @param hca            hca used by crawler. Be VERY CAREFUL of reusing it, because it will be closed when crawler stopped.
	 */
	public UrlClassifierConf(String name, int threadPoolSize, int ratePerMinute, HttpClientAssist hca) {
		this.name = Objects.requireNonNull(name, "name can't be null");
		this.threadPoolSize = Math.max(1, threadPoolSize);
		this.ratePerMinute = ratePerMinute;
		this.hca = Objects.requireNonNull(hca, "http client assist can't be null");
	}

	/**
	 * classifier adjuster will be used automatically, if you don't want it, call this.
	 */
	public UrlClassifierConf doNotUseAdjuster() {
		this.useAdjuster = false;
		return this;
	}

	/**
	 * BlockChecker is used to check whether current access is blocked.
	 * crawler speed auto-adjust is enabled only if blockChecker is given.
	 */
	public UrlClassifierConf setBlockChecker(BlockChecker blockChecker) {
		this.blockChecker = blockChecker;
		this.useAdjuster = blockChecker != null;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		UrlClassifierConf that = (UrlClassifierConf) o;

		if (threadPoolSize != that.threadPoolSize) return false;
		if (ratePerMinute != that.ratePerMinute) return false;
		if (!name.equals(that.name)) return false;
		return hca.equals(that.hca);
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + threadPoolSize;
		result = 31 * result + ratePerMinute;
		return result;
	}
}
