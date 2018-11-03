
package mysh.net.httpclient;

import okhttp3.CookieJar;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置VO.
 *
 * @author ZhangZhx
 */
public final class HttpClientConfig implements Cloneable {

	public static final String UA =
			"Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36";

	// http://user-agent-string.info/list-of-ua/bots
	public static final String UA_GOOGLE =
			"Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
	public static final String UA_BAIDU =
			"Mozilla/5.0 (compatible; Baiduspider/2.0; +http://www.baidu.com/search/spider.html)";
	public static final String UA_BING =
			"Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)";

	HashMap<String, Object> headers;

	/**
	 * Connection: keep-alive/close
	 */
	boolean isKeepAlive = true;

	/**
	 * 用户代理
	 */
	String userAgent = UA;

	/**
	 * 连接超时. in millisecond
	 */
	int connectionTimeout = 7_000;

	/**
	 * 取数据内容超时. in millisecond
	 */
	int soTimeout = 10_000;

	/**
	 * connection pool idol size
	 */
	int maxIdolConnections = 5;

	/**
	 * max connections of one route in pool
	 */
	int connPoolKeepAliveSec = 5 * 60;

	/** this will overwrite user defined <code>Cookie</code> header */
	CookieJar cookieJar = CookieJar.NO_COOKIES;

	public HttpClientConfig() {
	}

	public HttpClientConfig addHeaders(Map<String, ?> headers) {
		if (this.headers == null)
			this.headers = new HashMap<>();
		this.headers.putAll(headers);
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public HttpClientConfig clone() {
		HttpClientConfig c;
		try {
			c = (HttpClientConfig) super.clone();
			if (c.headers != null)
				c.headers = (HashMap<String, Object>) c.headers.clone();
		} catch (CloneNotSupportedException shouldNotHappen) {
			throw new InternalError(shouldNotHappen);
		}
		return c;
	}

	// getter and setter

	/**
	 * @see #cookieJar
	 */
	public HttpClientConfig setCookieJar(CookieJar cookieJar) {
		if (cookieJar != null) {
			this.cookieJar = cookieJar;
		}
		return this;
	}

	public boolean isKeepAlive() {
		return isKeepAlive;
	}

	public HttpClientConfig setKeepAlive(boolean keepAlive) {
		isKeepAlive = keepAlive;
		return this;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public HttpClientConfig setUserAgent(String userAgent) {
		this.userAgent = userAgent;
		return this;
	}

	/**
	 * @see #connectionTimeout
	 */
	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * @see #connectionTimeout
	 */
	public HttpClientConfig setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
		return this;
	}

	/**
	 * @see #soTimeout
	 */
	public int getSoTimeout() {
		return soTimeout;
	}

	/**
	 * @see #soTimeout
	 */
	public HttpClientConfig setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
		return this;
	}

	/**
	 * @see #maxIdolConnections
	 */
	public int getMaxIdolConnections() {
		return maxIdolConnections;
	}

	/**
	 * @see #maxIdolConnections
	 */
	public HttpClientConfig setMaxIdolConnections(int maxIdolConnections) {
		this.maxIdolConnections = maxIdolConnections;
		return this;
	}

	/**
	 * @see #connPoolKeepAliveSec
	 */
	public int getConnPoolKeepAliveSec() {
		return connPoolKeepAliveSec;
	}

	/**
	 * @see #connPoolKeepAliveSec
	 */
	public HttpClientConfig setConnPoolKeepAliveSec(int connPoolKeepAliveSec) {
		this.connPoolKeepAliveSec = connPoolKeepAliveSec;
		return this;
	}
}
