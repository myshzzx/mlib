
package mysh.net.httpclient;

import mysh.util.PropConf;
import org.apache.http.Header;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置VO.
 *
 * @author ZhangZhx
 */
public final class HttpClientConfig implements Serializable, Cloneable {

	private static final long serialVersionUID = 9097930407282337575L;

	public static final String UA =
					"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36";

	// http://user-agent-string.info/list-of-ua/bots
	public static final String UA_GOOGLE =
					"Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
	public static final String UA_BAIDU =
					"Mozilla/5.0 (compatible; Baiduspider/2.0; +http://www.baidu.com/search/spider.html)";
	public static final String UA_BING =
					"Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)";

	List<Header> headers = new ArrayList<>();

	/**
	 * Connection: keep-alive/close
	 */
	private boolean isKeepAlive = true;

	/**
	 * 用户代理
	 */
	private String userAgent = UA;

	/**
	 * 连接超时.
	 */
	private int connectionTimeout = 5;

	/**
	 * 取数据内容超时.
	 */
	private int soTimeout = 10;

	/**
	 * 连接池单域最大连接数.
	 */
	private int maxConnPerRoute = 10;

	/**
	 * 连接池最大连接数.
	 */
	private int maxConnTotal = 30;

	/**
	 * 是否使用代理
	 */
	private boolean useProxy;

	/**
	 * 代理主机
	 */
	private String proxyHost;

	/**
	 * 代理端口
	 */
	private int proxyPort;

	/**
	 * 代理类型
	 */
	private String proxyType;

	/**
	 * 代理验证名
	 */
	private String proxyAuthName;

	/**
	 * 代理验证密码
	 */
	private String proxyAuthPw;


	public HttpClientConfig() {
	}

	/**
	 * 根据默认的属性生成此实例.<br/>
	 * 默认属性示例如下:<br/>
	 * <p>
	 * <pre>
	 *
	 * # 是否保持连接
	 * httpclient.isKeepAlive=true
	 *
	 * # 用户代理串
	 * httpclient.userAgent=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.120 Safari/537.36
	 *
	 * # http 连接超时 ( 秒 )
	 * httpclient.connectionTimeout=3
	 *
	 * # socket 响应超时 ( 秒 )
	 * httpclient.soTimeout=3
	 *
	 * # 连接池单域最大连接数
	 * httpclient.maxConnPerRoute=10
	 *
	 * # 连接池最大连接数
	 * httpclient.maxConnTotal=30
	 *
	 * # 是否使用代理. true 表示使用, 其他表示不用
	 * httpclient.useProxy=false
	 *
	 * # 代理主机地址
	 * httpclient.proxyHost=192.168.1.1
	 *
	 * # 代理主机服务端口
	 * httpclient.proxyPort=1234
	 *
	 * # 代理协议
	 * httpclient.proxyType=http
	 *
	 * # 代理验证名
	 * httpclient.proxyAuthName=zzx
	 *
	 * # 代理验证密码
	 * httpclient.proxyAuthPw=zzx
	 *
	 * </pre>
	 */
	public HttpClientConfig(PropConf conf) {
		this.setUseProxy(conf.getPropString("httpclient.isKeepAlive", "true").equals("true"));
		this.setUserAgent(conf.getPropString("httpclient.userAgent", UA));
		this.setConnectionTimeout(conf.getPropInt("httpclient.connectionTimeout", 5));
		this.setSoTimeout(conf.getPropInt("httpclient.soTimeout", 10));
		this.setMaxConnPerRoute(conf.getPropInt("httpclient.maxConnPerRoute", 10));
		this.setMaxConnTotal(conf.getPropInt("httpclient.maxConnTotal", 30));

		this.setUseProxy(conf.getPropString("httpclient.useProxy").equals("true"));
		this.setProxyHost(conf.getPropString("httpclient.proxyHost"));
		this.setProxyPort(conf.getPropInt("httpclient.proxyPort"));
		this.setProxyType(conf.getPropString("httpclient.proxyType", "http"));
		this.setProxyAuthName(conf.getPropString("httpclient.proxyAuthName"));
		this.setProxyAuthPw(conf.getPropString("httpclient.proxyAuthPw"));
	}

	public HttpClientConfig addHeader(Header header) {
		this.headers.add(header);
		return this;
	}

	public boolean isKeepAlive() {
		return isKeepAlive;
	}

	public HttpClientConfig setKeepAlive(boolean isKeepAlive) {
		this.isKeepAlive = isKeepAlive;
		return this;
	}

	/**
	 * @return the userAgent
	 */
	public String getUserAgent() {

		return userAgent;
	}

	/**
	 * @param userAgent the userAgent to set
	 */
	public HttpClientConfig setUserAgent(String userAgent) {

		this.userAgent = userAgent;
		return this;
	}

	/**
	 * @return the connectionTimeout in seconds
	 */
	public int getConnectionTimeout() {

		return connectionTimeout;
	}

	/**
	 * @param connectionTimeout the connectionTimeout to set in seconds
	 */
	public HttpClientConfig setConnectionTimeout(int connectionTimeout) {

		this.connectionTimeout = connectionTimeout;
		return this;
	}

	/**
	 * @return the soTimeout in seconds
	 */
	public int getSoTimeout() {

		return soTimeout;
	}

	/**
	 * @param soTimeout the soTimeout to set in seconds
	 */
	public HttpClientConfig setSoTimeout(int soTimeout) {

		this.soTimeout = soTimeout;
		return this;
	}

	/**
	 * @return the useProxy
	 */
	public boolean isUseProxy() {

		return useProxy;
	}

	/**
	 * @param useProxy the useProxy to set
	 */
	public HttpClientConfig setUseProxy(boolean useProxy) {

		this.useProxy = useProxy;
		return this;
	}

	/**
	 * @return the proxyHost
	 */
	public String getProxyHost() {

		return proxyHost;
	}

	/**
	 * @param proxyHost the proxyHost to set
	 */
	public HttpClientConfig setProxyHost(String proxyHost) {

		this.proxyHost = proxyHost;
		return this;
	}

	/**
	 * @return the proxyPort
	 */
	public int getProxyPort() {

		return proxyPort;
	}

	/**
	 * @param proxyPort the proxyPort to set
	 */
	public HttpClientConfig setProxyPort(int proxyPort) {

		this.proxyPort = proxyPort;
		return this;
	}

	/**
	 * @return the proxyType
	 */
	public String getProxyType() {

		return proxyType;
	}

	/**
	 * @param proxyType the proxyType to set
	 */
	public HttpClientConfig setProxyType(String proxyType) {

		this.proxyType = proxyType;
		return this;
	}

	/**
	 * @return the proxyAuthName
	 */
	public String getProxyAuthName() {

		return proxyAuthName;
	}

	/**
	 * @param proxyAuthName the proxyAuthName to set
	 */
	public HttpClientConfig setProxyAuthName(String proxyAuthName) {

		this.proxyAuthName = proxyAuthName;
		return this;
	}

	/**
	 * @return the proxyAuthPw
	 */
	public String getProxyAuthPw() {

		return proxyAuthPw;
	}

	/**
	 * @param proxyAuthPw the proxyAuthPw to set
	 */
	public HttpClientConfig setProxyAuthPw(String proxyAuthPw) {

		this.proxyAuthPw = proxyAuthPw;
		return this;
	}

	public int getMaxConnPerRoute() {
		return maxConnPerRoute;
	}

	public HttpClientConfig setMaxConnPerRoute(int maxConnPerRoute) {
		if (maxConnPerRoute > 0)
			this.maxConnPerRoute = maxConnPerRoute;
		return this;
	}

	public int getMaxConnTotal() {
		return Math.max(maxConnTotal, this.maxConnPerRoute);
	}

	public HttpClientConfig setMaxConnTotal(int maxConnTotal) {
		if (maxConnTotal > 0)
			this.maxConnTotal = Math.max(maxConnTotal, this.maxConnPerRoute);
		return this;
	}
}
