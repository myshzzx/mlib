
package mysh.net.httpclient;

import mysh.annotation.ThreadSafe;
import mysh.util.EncodingUtil;
import mysh.util.Strings;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * HTTP 客户端组件.
 *
 * @author ZhangZhx
 */
@ThreadSafe
public class HttpClientAssist implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(HttpClientAssist.class);

	private final CloseableHttpClient hc;

	public HttpClientAssist(HttpClientConfig conf) {
		if (conf == null) {
			throw new IllegalArgumentException();
		}

		HttpClientBuilder hcBuilder = HttpClientBuilder.create();

		Header connection = new BasicHeader("Connection", conf.isKeepAlive() ? "keep-alive" : "close");
		conf.addHeader(connection);
		hcBuilder.setDefaultHeaders(conf.headers);

		RequestConfig reqConf = RequestConfig.custom()
						.setConnectTimeout(conf.getConnectionTimeout() * 1000)
						.setSocketTimeout(conf.getSoTimeout() * 1000)
						.build();
		hcBuilder.setDefaultRequestConfig(reqConf);

		hcBuilder.setMaxConnPerRoute(conf.getMaxConnPerRoute());
		hcBuilder.setMaxConnTotal(conf.getMaxConnTotal());

		hcBuilder.setUserAgent(conf.getUserAgent());

		if (conf.isUseProxy()) {
			HttpHost proxyHost = new HttpHost(conf.getProxyHost(), conf.getProxyPort(),
							conf.getProxyType());
			hcBuilder.setProxy(proxyHost);

			if (!Strings.isEmpty(conf.getProxyAuthName())) {
				CredentialsProvider cp = new BasicCredentialsProvider();
				cp.setCredentials(new AuthScope(conf.getProxyHost(), conf.getProxyPort()),
								new UsernamePasswordCredentials(conf.getProxyAuthName(), conf.getProxyAuthPw()));
				hcBuilder.setDefaultCredentialsProvider(cp);
				hcBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
			}
		}

		hc = hcBuilder.build();
	}

	/**
	 * get url entity by get method.<br/>
	 * WARNING: the entity must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @throws InterruptedException 线程中断.
	 * @throws IOException          连接异常.
	 */
	public UrlEntity access(String url) throws InterruptedException, IOException {

		return access(new HttpGet(url), null);
	}


	/**
	 * get url entity by get method.<br/>
	 * WARNING: the entity must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @param headers request headers, can be null.
	 * @throws InterruptedException 线程中断.
	 * @throws IOException          连接异常.
	 */
	public UrlEntity access(String url, Map<String, String> headers)
					throws InterruptedException, IOException {

		return access(new HttpGet(url), headers);
	}

	/**
	 * get url entity by post method.<br/>
	 * WARNING: the entity must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @param charset encoding the name/value pairs be encoded with. can be null.
	 * @param headers request headers, can be null.
	 * @throws InterruptedException 线程中断.
	 * @throws IOException          连接异常.
	 */
	public UrlEntity access(String url, List<NameValuePair> postParams, Charset charset,
	                        Map<String, String> headers) throws IOException, InterruptedException {
		HttpPost post = new HttpPost(url);
		HttpEntity reqEntity = new UrlEncodedFormEntity(postParams, charset);
		post.setEntity(reqEntity);
		return access(post, headers);
	}

	/**
	 * get url entity.<br/>
	 * WARNING: the entity must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @param headers request headers, can be null.
	 * @throws InterruptedException 线程中断.
	 * @throws IOException          连接异常.
	 */
	public UrlEntity access(HttpUriRequest req, Map<String, String> headers)
					throws InterruptedException, IOException {
		// 响应中断
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException();
		}

		HttpContext ctx = new BasicHttpContext();
		if (headers != null) {
			for (Map.Entry<String, String> he : headers.entrySet()) {
				req.setHeader(he.getKey(), he.getValue());
			}
		}
		return new UrlEntity(req.getURI().toString(), hc.execute(req, ctx), ctx);
	}

	/**
	 * get input stream of the entity.
	 * WARNING: the InputStream must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @param headers request headers, can be null.
	 */
	public InputStream getInputStream(String url, Map<String, String> headers) throws IOException, InterruptedException {
		UrlEntity newEntity = access(url, headers);
		return newEntity.rsp.getEntity().getContent();
	}

	/**
	 * 将数据下载保存到文件.
	 *
	 * @param url            数据文件地址
	 * @param headers        请求头, 可为 null
	 * @param filePath       保存路径
	 * @param overwrite      是否覆盖原有文件
	 * @param downloadBufLen 下载缓存, 有效值为 100K ~ 10M 间
	 * @return 文件是否被下载写入
	 * @throws IOException          IO异常
	 * @throws InterruptedException 线程中断异常
	 */
	public boolean saveToFile(String url, Map<String, String> headers,
	                          String filePath, boolean overwrite, int downloadBufLen
	) throws InterruptedException, IOException {

		File file = new File(filePath);

		if (!overwrite && file.exists())
			return false;

		if (downloadBufLen < 100000 || downloadBufLen > 10000000) {
			downloadBufLen = 500000;
		}

		try (UrlEntity ue = this.access(url, headers)) {

			long fileLength = ue.rsp.getEntity().getContentLength();

			file.getParentFile().mkdirs();
			file.createNewFile();
			try (RandomAccessFile fileOut = new RandomAccessFile(file, "rw");
			     InputStream in = ue.rsp.getEntity().getContent()) {

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
		}
		return true;
	}

	@Override
	public void close() {
		try {
			this.hc.close();
		} catch (Exception e) {
			log.debug("http client close error.", e);
		}
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

	private static final byte[] EMPTY_BUF = new byte[0];

	@ThreadSafe
	public final class UrlEntity implements Closeable {

		private String reqUrl;
		private CloseableHttpResponse rsp;
		private HttpContext ctx;

		private String currentUrl;
		private String contentType;
		private byte[] entityBuf;
		private String entityStr;

		public UrlEntity(String reqUrl, CloseableHttpResponse rsp, HttpContext ctx) {
			this.reqUrl = reqUrl;
			this.rsp = rsp;
			this.ctx = ctx;

			int status = rsp.getStatusLine().getStatusCode();
			if (status >= 400)
				log.warn("access error, status=" + rsp.getStatusLine() + ", url=" + getCurrentURL());

			if (rsp.getEntity() != null && rsp.getEntity().getContentType() != null)
				this.contentType = rsp.getEntity().getContentType().getValue();
		}

		@Override
		public void close() {
			try {
				rsp.getEntity().getContent().close();
			} catch (Exception e) {
				log.debug("entity close error: " + getCurrentURL(), e);
			}
		}

		/**
		 * @return original request url.
		 */
		public String getReqUrl() {
			return reqUrl;
		}

		/**
		 * @return response protocol version.
		 */
		public ProtocolVersion getProtocol() {
			return this.rsp.getProtocolVersion();
		}

		/**
		 * @return request reqUrl may jump several times, get the real access url.
		 */
		public String getCurrentURL() {
			if (this.currentUrl == null) {
				HttpUriRequest currReq = (HttpUriRequest) ctx.getAttribute(HttpCoreContext.HTTP_REQUEST);
				HttpHost currHost = (HttpHost) ctx.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
				this.currentUrl = (currReq.getURI().isAbsolute()) ?
								currReq.getURI().toString() : (currHost.toURI() + currReq.getURI());
			}

			return this.currentUrl;
		}

		/**
		 * get response status.
		 */
		public StatusLine getStatusLine() {
			return rsp.getStatusLine();
		}

		public boolean isText() {
			return contentType != null && contentType.contains("text");
		}

		public boolean isHtml() {
			return contentType != null && contentType.contains("html");
		}

		public boolean isImage() {
			return contentType != null && contentType.contains("image");
		}

		public boolean isContentType(String type) {
			return contentType != null && contentType.contains(type);
		}

		/**
		 * content length in byte size. Pls notice that some entity may have a (-1) length.
		 */
		public long getContentLength() {
			return rsp.getEntity().getContentLength();
		}

		/**
		 * buf then convert to string. the buf is saved and can be reused.
		 */
		public synchronized String getEntityStr() throws IOException {
			if (entityStr != null) return entityStr;

			String enc = null;
			if (contentType != null) {
				String c = contentType.toUpperCase();
				enc = c.contains("UTF-8") ? "UTF-8" : enc;
				enc = c.contains("GBK") ? "GBK" : enc;
			}
			if (entityBuf == null) {
				entityBuf = EntityUtils.toByteArray(rsp.getEntity());
				entityBuf = entityBuf == null ? EMPTY_BUF : entityBuf;
			}
			if (enc == null) {
				enc = EncodingUtil.isUTF8Bytes(entityBuf) ? "UTF-8" : "GBK";
			}
			entityStr = new String(entityBuf, enc);

			return entityStr;
		}

		/**
		 * buf then write. the buf is saved and can be reused.
		 */
		public synchronized void bufWriteTo(OutputStream out) throws IOException {
			if (entityBuf == null) {
				entityBuf = EntityUtils.toByteArray(rsp.getEntity());
				entityBuf = entityBuf == null ? EMPTY_BUF : entityBuf;
			}
			out.write(entityBuf);
			out.flush();
		}

		@Override
		public String toString() {
			return getCurrentURL();
		}
	}
}
