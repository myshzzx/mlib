package mysh.crawler2;

import mysh.annotation.Immutable;
import mysh.net.httpclient.HttpClientConfig;

import java.io.Serializable;
import java.util.Objects;

/**
 * url classifier config. <br/>
 * used to gen url classifier, should be reused, because every config is related to a unique url
 * classifier which is expensive to create.
 */
@Immutable
public final class UrlClassifierConf implements Serializable {
	private static final long serialVersionUID = 8722727367175019882L;

	public interface Factory<CTX extends UrlContext> extends Serializable {
		UrlClassifierConf get(String url, CTX ctx);
	}

	/**
	 * max accurate flow control per minute: 30*60.
	 */
	public static final int maxAccRatePM = 30 * 60;

	final String name;
	final int threadPoolSize;
	final int ratePerMinute;
	final HttpClientConfig hcc;

	/**
	 * @param name           the name of url classifier.
	 * @param threadPoolSize thread pool size of the classifier.
	 * @param ratePerMinute  flow control. the max accurate controllable value is {@link #maxAccRatePM}.
	 *                       see {@link mysh.crawler2.Crawler.UrlClassifier#setRatePerMinute(int)}
	 * @param hcc            http config. can be reused, but every url classifier it related to will
	 *                       gen a new client. NOTE that modifying this config after it being used
	 *                       will NOT effect crawler's behavior.
	 */
	public UrlClassifierConf(String name, int threadPoolSize, int ratePerMinute, HttpClientConfig hcc) {
		this.name = Objects.requireNonNull(name, "name can't be null");
		this.threadPoolSize = Math.max(1, threadPoolSize);
		this.ratePerMinute = ratePerMinute;
		this.hcc = Objects.requireNonNull(hcc, "http client config can't be null");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		UrlClassifierConf that = (UrlClassifierConf) o;

		if (threadPoolSize != that.threadPoolSize) return false;
		if (ratePerMinute != that.ratePerMinute) return false;
		if (!name.equals(that.name)) return false;
		return hcc == that.hcc;
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + threadPoolSize;
		result = 31 * result + ratePerMinute;
		result = 31 * result + hcc.hashCode();
		return result;
	}
}
