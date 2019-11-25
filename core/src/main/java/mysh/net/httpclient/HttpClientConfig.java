
package mysh.net.httpclient;

import com.google.common.net.HttpHeaders;
import mysh.collect.Colls;
import okhttp3.CookieJar;
import okhttp3.EventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置VO.
 *
 * @author ZhangZhx
 */
public final class HttpClientConfig implements Cloneable {
	
	public static final String UA =
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36";
	
	// http://user-agent-string.info/list-of-ua/bots
	public static final String UA_GOOGLE_BOT =
			"Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
	
	Map<String, Object> headers = Colls.ofHashMap(
			HttpHeaders.ACCEPT, "*/*"
			, HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br"
	);
	
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
	
	/**
	 * this will overwrite user defined <code>Cookie</code> header
	 */
	CookieJar cookieJar = CookieJar.NO_COOKIES;
	
	EventListener eventListener;
	
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
				c.headers = (Map<String, Object>) ((HashMap) c.headers).clone();
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
	
	/**
	 * @see #eventListener
	 */
	public EventListener getEventListener() {
		return eventListener;
	}
	
	/**
	 * @see #eventListener
	 */
	public HttpClientConfig setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
		return this;
	}
}
