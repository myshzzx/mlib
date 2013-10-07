
package mysh.net.httpclient;

import mysh.util.PropConf;

import java.io.Serializable;

/**
 * 配置VO.
 *
 * @author ZhangZhx
 */
public class HttpClientConfig implements Serializable {

	private static final long serialVersionUID = 9097930407282337575L;

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

	/**
	 * 用户代理
	 */
	private String userAgent;

	/**
	 * 连接超时.
	 */
	private int connectionTimeout;

	/**
	 * 取数据内容超时.
	 */
	private int soTimeout;

	/**
	 * 取默认配置.
	 */
	public static HttpClientConfig getDefault() {

		HttpClientConfig conf = new HttpClientConfig();
		conf.connectionTimeout = 10;
		conf.soTimeout = 10;
		return conf;
	}

	/**
	 * 根据默认的属性生成此实例.<br/>
	 * 默认属性示例如下:<br/>
	 * <p/>
	 * <pre>
	 *
	 * #是否使用代理. true 表示使用, 其他表示不用
	 * httpclient.useProxy=true
	 *
	 * #代理主机地址
	 * httpclient.proxyHost=192.168.1.1
	 *
	 * #代理主机服务端口
	 * httpclient.proxyPort=1234
	 *
	 * #代理协议
	 * httpclient.proxyType=http
	 *
	 * #代理验证名
	 * httpclient.proxyAuthName=zzx
	 *
	 * #代理验证密码
	 * httpclient.proxyAuthPw=zzx
	 *
	 * #用户代理串
	 * httpclient.userAgent=Myshbot 1.4
	 *
	 * #http 连接超时 ( 秒 )
	 * httpclient.connectionTimeout=3
	 *
	 * </pre>
	 */
	public static HttpClientConfig genDefaultConfig(PropConf conf) {

		HttpClientConfig httpClientconf = new HttpClientConfig();
		httpClientconf.setConnectionTimeout(conf.getPropInt("httpclient.connectionTimeout",
						10));
		httpClientconf.setProxyAuthName(conf.getPropString("httpclient.proxyAuthName"));
		httpClientconf.setProxyAuthPw(conf.getPropString("httpclient.proxyAuthPw"));
		httpClientconf.setProxyHost(conf.getPropString("httpclient.proxyHost"));
		httpClientconf.setProxyPort(conf.getPropInt("httpclient.proxyPort"));
		httpClientconf.setProxyType(conf.getPropString("httpclient.proxyType", "http"));
		httpClientconf.setSoTimeout(conf.getPropInt("httpclient.soTimeout", 10));
		httpClientconf.setUseProxy(conf.getPropString("httpclient.useProxy").equals("true"));
		httpClientconf.setUserAgent(conf.getPropString("httpclient.userAgent", "Myshbot 1.4"));
		return httpClientconf;
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
	 * @return the connectionTimeout
	 */
	public int getConnectionTimeout() {

		return connectionTimeout;
	}

	/**
	 * @param connectionTimeout the connectionTimeout to set
	 */
	public HttpClientConfig setConnectionTimeout(int connectionTimeout) {

		this.connectionTimeout = connectionTimeout;
		return this;
	}

	/**
	 * @return the soTimeout
	 */
	public int getSoTimeout() {

		return soTimeout;
	}

	/**
	 * @param soTimeout the soTimeout to set
	 */
	public HttpClientConfig setSoTimeout(int soTimeout) {

		this.soTimeout = soTimeout;
		return this;
	}

}
