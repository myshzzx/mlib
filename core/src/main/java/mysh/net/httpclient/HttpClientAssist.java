
package mysh.net.httpclient;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * HTTP 客户端组件.
 *
 * @author ZhangZhx
 */
public class HttpClientAssist {

	private final HttpClientConfig conf;

	/**
	 * 请求头
	 */
	private final Header[] headers;

	/**
	 * 代理主机
	 */
	private final HttpHost proxy;

	public HttpClientAssist(HttpClientConfig conf) {

		if (conf == null) {
			throw new IllegalArgumentException();
		}
		this.conf = conf;

		Header connection = new BasicHeader("Connection", this.conf.isKeepAlive() ? "keep-alive" : "close");
//		Header proxyConnection = new BasicHeader("Proxy-Connection", "close");
		Header userAgent = new BasicHeader("User-Agent", this.conf.getUserAgent());
		Header charSet = new BasicHeader("Accept-Charset", "iso-8859-1, utf-8");
		this.headers = new Header[]{connection, userAgent, charSet};

		if (this.conf.isUseProxy()) {
			this.proxy = new HttpHost(this.conf.getProxyHost(), this.conf.getProxyPort(),
							this.conf.getProxyType());
		} else {
			this.proxy = null;
		}
	}

	/**
	 * 取 URL 响应报文.
	 *
	 * @throws InterruptedException 线程中断.
	 * @throws IOException          连接异常.
	 */
	private RoutableHttpClient.RoutableHttpResponse getResp(String url)
					throws InterruptedException, IOException {

		RoutableHttpClient client = new RoutableHttpClient();
		HttpConnectionParams.setConnectionTimeout(client.getParams(),
						this.conf.getConnectionTimeout() * 1000);
		HttpConnectionParams
						.setSoTimeout(client.getParams(), this.conf.getSoTimeout() * 1000);

		if (this.conf.isUseProxy()) {
			client.getCredentialsProvider()
							.setCredentials(
											new AuthScope(this.conf.getProxyHost(),
															this.conf.getProxyPort()),
											new UsernamePasswordCredentials(this.conf
															.getProxyAuthName(), this.conf
															.getProxyAuthPw()));
			client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, this.proxy);
		}

		HttpUriRequest req = new HttpGet(url);

		req.setHeaders(this.headers);

		// 响应中断
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException();
		}

		return (RoutableHttpClient.RoutableHttpResponse) client.execute(req);
	}

	/**
	 * 从 url 地址取页面数据. 可响应中断.<br/>
	 * 线程安全.
	 *
	 * @param url 页面地址
	 * @return 含真实地址的页面内容
	 * @throws IOException                   连接异常
	 * @throws InterruptedException          线程中断异常
	 * @throws HCExceptions.GetPageException 取页面返回代码不是200, 或取得的页面不是文本
	 */
	public Page getPage(String url)
					throws IOException, InterruptedException, HCExceptions.GetPageException {

		RoutableHttpClient.RoutableHttpResponse resp = this.getResp(url);

		// 内容异常
		Header[] type = resp.getHeaders("Content-Type");
		if (resp.getStatusLine().getStatusCode() != 200
						|| (type.length != 0 && !type[0].getValue().startsWith("text")
						&& !type[0].getValue().startsWith("xml") && !type[0].getValue().startsWith("json"))) {
			throw new HCExceptions.GetPageException(url);
		}

		// 大部分页面的编码信息不会出现在响应报文中, 需要先取得页面, 才能知道页面的编码
		String content = EntityUtils.toString(resp.getEntity(), Page.DefaultEncoding);

		// 处理编码, 默认 utf-8
		String charSet;
		int charsetBegin = content.indexOf("charset=");

		Page page = new Page(resp.getCurrentURL());
		if (charsetBegin != -1) {
			int charsetEnd = content.indexOf("\"", charsetBegin);
			charSet = content.substring(charsetBegin + 8, charsetEnd);
			if (charSet.toLowerCase().contains("gb")) charSet = "GBK";
			page.setContent(new String(content.getBytes(Page.DefaultEncoding), charSet));
		} else {
			page.setContent(content);
		}

		return page;
	}

	/**
	 * 将数据下载保存到文件.
	 *
	 * @param url            数据文件地址
	 * @param filePath       保存路径
	 * @param overwrite      是否覆盖原有文件
	 * @param downloadBufLen 下载缓存, 有效值为 100K ~ 10M 间
	 * @return 文件是否被下载写入
	 * @throws IOException          IO异常
	 * @throws InterruptedException 线程中断异常
	 */
	public boolean saveToFile(String url, String filePath, boolean overwrite, int downloadBufLen)
					throws InterruptedException, IOException {

		File file = new File(filePath);

		if (!overwrite && file.exists())
			return false;

		if (downloadBufLen < 100000 || downloadBufLen > 10000000) {
			downloadBufLen = 500000;
		}

		RoutableHttpClient.RoutableHttpResponse resp = this.getResp(url);

		long fileLength = resp.getEntity().getContentLength();

		file.getParentFile().mkdirs();
		file.createNewFile();
		try (RandomAccessFile fileOut = new RandomAccessFile(file, "rw");
		     InputStream in = resp.getEntity().getContent()) {

			fileOut.setLength(fileLength);

			byte[] buf = new byte[downloadBufLen];
			int readLen;
			while (fileLength > 0) {
				readLen = in.read(buf);
				if (readLen == -1) {
					break;
				}
				fileOut.write(buf, 0, readLen);
				fileLength -= readLen;
			}
		}
		return true;
	}

	/**
	 * 取地址输入流.
	 *
	 * @param url 页面地址.
	 * @throws IOException          IO异常
	 * @throws InterruptedException 线程中断异常
	 */
	public InputStream readContent(String url) throws IOException, InterruptedException {

		RoutableHttpClient.RoutableHttpResponse resp = this.getResp(url);
		return resp.getEntity().getContent();
	}

	/**
	 * 将带有 ./ 和 ../ 的 URI 转换成简短的 URL 形式.
	 */
	public static String getShortURL(String uriString) {

		if (uriString == null) {
			return "";
		}

		if (!uriString.contains("./")) {
			return uriString;
		}

		URI uri;
		try {
			uri = new URI(uriString);
		} catch (URISyntaxException e) {
			return uriString;
		}

		StringBuilder url = new StringBuilder();
		if (uri.getScheme() != null) {
			url.append(uri.getScheme());
			url.append("://");
		}
		if (uri.getHost() != null) {
			url.append(uri.getHost());
			if (uri.getPort() != -1
							&& !(uri.getScheme().equals("http") && uri.getPort() == 80)
							&& !(uri.getScheme().equals("https") && uri.getPort() == 443)) {
				url.append(":");
				url.append(uri.getPort());
			}
		}

		// 处理 URL Path 中的 . 和 ..
		String[] tPath = uri.getPath().split("/");
		// int lastUnNullBlankIndex = -1;
		Deque<Integer> lastUnNullBlankIndex = new LinkedList<>();
		for (int index = 0; index < tPath.length; index++) {
			switch (tPath[index]) {
				case "":
				case ".":
					tPath[index] = null;
					break;
				case "..":
					tPath[index] = null;
					try {
						tPath[lastUnNullBlankIndex.pop()] = null;
					} catch (NoSuchElementException e) {
						// String msg = "URI 简化失败: " + uriString;
						// Exception ex = new Exception(msg);
						return uriString;
					}
					break;
				default:
					lastUnNullBlankIndex.push(index);
					break;
			}
		}

		for (String aTPath : tPath) {
			if (aTPath != null) {
				url.append("/");
				url.append(aTPath);
			}
		}
		if (uri.getPath().endsWith("/")) {
			url.append("/");
		}

		// 处理参数
		if (uri.getQuery() != null) {
			url.append("?");
			url.append(uri.getQuery());
		}

		// 处理形如 g.cn/search?q=2 的情况
		if (uri.getScheme() == null && !uri.getPath().startsWith("/")) {
			url.delete(0, 1);
		}

		return url.toString();
	}
}