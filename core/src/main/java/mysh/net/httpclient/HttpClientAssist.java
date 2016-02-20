
package mysh.net.httpclient;

import com.google.api.client.http.*;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.util.Key;
import mysh.util.Encodings;
import mysh.util.FilesUtil;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.lang.reflect.Field;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;

/**
 * HTTP 客户端组件.
 *
 * @author ZhangZhx
 */
@ThreadSafe
public class HttpClientAssist implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(HttpClientAssist.class);
	private static final HttpClientConfig defaultHcc = new HttpClientConfig();

	public HttpClientAssist() {
		this(null, null);
	}

	public HttpClientAssist(HttpClientConfig hcc) {
		this(hcc, null);
	}

	private final HttpRequestFactory reqFactory;
	private final HttpTransport httpTransport;

	public HttpClientAssist(@Nullable HttpClientConfig conf, @Nullable ProxySelector proxySelector) {
		HttpClientConfig hcc = conf == null ? defaultHcc : conf.clone();

//		httpTransport = new NetHttpTransport.Builder()
//						.setConnectionFactory(url -> {
//							Proxy proxy = hcc.proxy;
//							if (proxyPicker != null)
//								proxy = proxyPicker.pick();
//							HttpURLConnection conn = (HttpURLConnection) (proxy == null ? url.openConnection() : url.openConnection(proxy));
//							return conn;
//						}).build();
		ApacheHttpTransport.Builder apacheBuilder = new ApacheHttpTransport.Builder().setProxySelector(proxySelector);
		HttpParams httpParams = apacheBuilder.getHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, hcc.soTimeout);
		HttpConnectionParams.setConnectionTimeout(httpParams, hcc.connectionTimeout);
		ConnManagerParams.setMaxTotalConnections(httpParams, hcc.maxTotalConnections);
		ConnManagerParams.setMaxConnectionsPerRoute(httpParams, new ConnPerRouteBean(hcc.maxConnectionsPerRoute));

		httpTransport = apacheBuilder.build();

		reqFactory = httpTransport.createRequestFactory(req -> {
			req.setConnectTimeout(hcc.connectionTimeout)
							.setReadTimeout(hcc.soTimeout)
							.setSuppressUserAgentSuffix(true)
							.setThrowExceptionOnExecuteError(false)
			;

			if (hcc.headers != null)
				req.getHeaders().putAll(hcc.headers);
			req.getHeaders()
							.setUserAgent(hcc.userAgent)
							.put(HttpHeadersName.Connection, hcc.isKeepAlive ? "keep-alive" : "close")
			;
		});
	}

	/**
	 * get url entity by get method.<br/>
	 * WARNING: the entity must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @throws IOException 连接异常.
	 */
	public UrlEntity access(String url) throws IOException {
		return access(url, null, null);
	}

	/**
	 * get url entity by get method.<br/>
	 * WARNING: the entity must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @param headers request headers, can be null.
	 * @throws IOException 连接异常.
	 */
	public UrlEntity access(String url, Map<String, ?> headers) throws IOException {
		return access(url, headers, null);
	}

	/**
	 * get url entity by get method.<br/>
	 * WARNING: the entity must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @param headers request headers, can be null.
	 * @param params  request params, can be null.
	 * @throws IOException 连接异常.
	 */
	public UrlEntity access(String url, Map<String, ?> headers, Map<String, ?> params) throws IOException {
		if (params != null && params.size() > 0) {
			StringBuilder usb = new StringBuilder(url);
			if (!url.contains("?"))
				usb.append('?');
			else
				usb.append('&');
			for (Map.Entry<String, ?> pe : params.entrySet()) {
				usb.append(pe.getKey()).append('=').append(pe.getValue()).append('&');
			}
			if (usb.charAt(usb.length() - 1) == '&') usb.deleteCharAt(usb.length() - 1);
			url = usb.toString();
		}

		HttpRequest req = reqFactory.buildGetRequest(new GenericUrl(url));
		return access(req, headers);
	}

	/**
	 * get url entity by post method.<br/>
	 * WARNING: the entity must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @param headers request headers.
	 * @param params  request params. upload type: multipart/form-data, support files
	 * @throws IOException 连接异常.
	 */
	public UrlEntity accessPost(
					String url, @Nullable Map<String, ?> headers, @Nullable Map<String, ?> params) throws IOException {
		return accessPost(url, headers, params, null);
	}

	/**
	 * get url entity by post method.<br/>
	 * WARNING: the entity must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @param headers request headers.
	 * @param params  request params. upload type: multipart/form-data, support files
	 * @param enc     param value encoding
	 * @throws IOException 连接异常.
	 */
	public UrlEntity accessPost(
					String url, @Nullable Map<String, ?> headers, @Nullable Map<String, ?> params,
					@Nullable Charset enc) throws IOException {
		MultipartContent content = null;
		if (params != null && params.size() > 0) {
			content = new MultipartContent().setMediaType(
							new HttpMediaType("multipart/form-data")
											.setParameter("boundary", "__END_OF_PART__"));

			if (enc == null) enc = Charset.defaultCharset();
			for (Map.Entry<String, ?> entry : params.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();

				MultipartContent.Part part;
				if (value instanceof File) {
					File file = (File) value;
					FileContent fileContent = new FileContent(null, file);
					part = new MultipartContent.Part(fileContent);
					part.setHeaders(new HttpHeaders().set(
									"Content-Disposition",
									String.format("form-data; name=\"%s\"; filename=\"%s\"", key, file.getName())));
				} else {
					if (value == null) value = "";
					part = new MultipartContent.Part(
									new ByteArrayContent(null, String.valueOf(value).getBytes(enc)));
					part.setHeaders(new HttpHeaders().set(
									"Content-Disposition", String.format("form-data; name=\"%s\"", key)));
				}
				content.addPart(part);
			}
		}

		HttpRequest req = reqFactory.buildPostRequest(new GenericUrl(url), content);
		return access(req, headers);
	}

	/**
	 * get url entity by post method.<br/>
	 * WARNING: the entity must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @param headers request headers.
	 * @throws IOException 连接异常.
	 */
	public UrlEntity accessSendContent(
					String url, @Nullable Map<String, ?> headers, @Nullable byte[] buf) throws IOException {
		ByteArrayContent content = null;
		if (buf != null) {
			content = new ByteArrayContent("application/octet-stream", buf);
		}

		HttpRequest req = reqFactory.buildPostRequest(new GenericUrl(url), content);
		return access(req, headers);
	}

	public static final Set<String> httpHeadersListFields = new HashSet<>();

	static {
		for (Field field : HttpHeaders.class.getDeclaredFields()) {
			Key headerKey = field.getAnnotation(Key.class);
			if (headerKey != null) {
				httpHeadersListFields.add(headerKey.value());
			}
		}
	}

	/**
	 * get url entity.<br/>
	 * WARNING: the entity must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @param headers request headers, can be null.
	 * @throws IOException 连接异常.
	 */
	private UrlEntity access(HttpRequest req, Map<String, ?> headers) throws IOException {
		// 响应中断
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedIOException("access interrupted: " + req.getUrl().build());
		}

		if (headers != null) {
			HttpHeaders reqHeaders = req.getHeaders();
			for (Map.Entry<String, ?> he : headers.entrySet()) {
				String name = he.getKey();
				Object value = he.getValue();
				if (httpHeadersListFields.contains(name)) {
					if (value != null) {
						value = new ArrayList<>();
						((ArrayList) value).add(he.getValue());
					}
				}
				reqHeaders.put(name, value);
			}
		}

		return new UrlEntity(req);
	}

	@Override
	public void close() {
		try {
			httpTransport.shutdown();
		} catch (IOException e) {
			log.debug("hca close error", e);
		}
	}

	/**
	 * download big resource and save to file.
	 *
	 * @param url       数据文件地址
	 * @param headers   请求头, 可为 null
	 * @param overwrite 是否覆盖原有文件
	 * @return 文件是否被下载写入
	 * @throws IOException IO异常
	 */
	public boolean saveToFile(
					String url, Map<String, ?> headers, File file, boolean overwrite
	) throws IOException {

		if (!overwrite && file.exists())
			return false;

		try (UrlEntity ue = this.access(url, headers)) {
			File writeFile = new File(file.getPath() + ".~write~");
			file.getParentFile().mkdirs();
			try (OutputStream os = Files.newOutputStream(writeFile.toPath());
			     InputStream in = ue.rsp.getContent()) {
				byte[] buf = getDownloadBuf();

				Thread thread = Thread.currentThread();
				int readLen;
				while (!thread.isInterrupted() && (readLen = in.read(buf)) > -1) {
					os.write(buf, 0, readLen);
				}
				if (thread.isInterrupted())
					throw new InterruptedIOException("download interrupted: " + url);
			}
			file.delete();
			writeFile.renameTo(file);
		}
		return true;
	}

	/**
	 * 将带有 ./ 和 ../ 的 URI 转换成简短的 URL 形式.
	 */
	public static String getShortURL(String uriString) {

		if (uriString == null) {
			return "";
		}

		if (!uriString.contains("./") && uriString.indexOf("//", 7) < 0) {
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
	private static final int DOWNLOAD_BUF_LEN = 100_000;
	private static final ThreadLocal<byte[]> threadDownloadBuf = new ThreadLocal<>();

	private static byte[] getDownloadBuf() {
		byte[] buf = threadDownloadBuf.get();
		if (buf == null)
			threadDownloadBuf.set(buf = new byte[DOWNLOAD_BUF_LEN]);
		return buf;
	}

	@ThreadSafe
	public final class UrlEntity implements Closeable {

		private final HttpRequest req;
		private final String reqUrl;
		private final HttpResponse rsp;
		private String currentUrl;
		private String contentType;
		private byte[] entityBuf;
		private String entityStr;

		public UrlEntity(HttpRequest req) throws IOException {
			this.req = req;
			// can't lazy init, because it changes after req.execute()
			this.reqUrl = req.getUrl().build();

			rsp = req.execute();

			int statusCode = rsp.getStatusCode();
			if (statusCode >= 400)
				log.warn("access unsuccessful, status={}, msg={}, url={}",
								statusCode, rsp.getStatusMessage(), this.reqUrl);
			this.contentType = rsp.getContentType();
		}

		/**
		 * close the connection immediately, unfinished download will be aborted.
		 */
		@Override
		public void close() {
			try {
//				rsp.ignore();
				rsp.disconnect();
			} catch (Exception e) {
				log.debug("abort req error. " + e);
			}
		}

		/**
		 * @return original request url.
		 */
		public final String getReqUrl() {
			return reqUrl;
		}

		/**
		 * @return response protocol version. e.g. https
		 */
		public String getProtocol() {
			return this.req.getUrl().getScheme();
		}

		/**
		 * @return request reqUrl may jump several times, get the real access url.
		 */
		public String getCurrentURL() {
			if (this.currentUrl == null)
				this.currentUrl = req.getUrl().build();

			return this.currentUrl;
		}

		/**
		 * get response status.
		 */
		public int getStatusCode() {
			return rsp.getStatusCode();
		}

		/**
		 * a request is text by default if the header(content-type) is not given.
		 * see {@link #isContentType(String)}
		 */
		public boolean isText() {
			return contentType == null || contentType.contains("text");
		}

		/**
		 * <b>IMPORTANT:</b> see {@link #isContentType(String)}
		 */
		public boolean isHtml() {
			return contentType != null && contentType.contains("html");
		}

		/**
		 * <b>IMPORTANT:</b> see {@link #isContentType(String)}
		 */
		public boolean isImage() {
			return contentType != null && contentType.contains("image");
		}

		/**
		 * content-type(FROM response header) check.<br/>
		 * if the header is not given or incorrect, the judgement will be incorrect.
		 * So use this ONLY if file extension judgement can not work.
		 */
		public boolean isContentType(String type) {
			return contentType != null && contentType.contains(type);
		}

		/**
		 * content length in byte size. return -1 if length not given (response header).
		 */
		public long getContentLength() {
			Long len = rsp.getHeaders().getContentLength();
			return len == null ? -1 : len;
		}

		/**
		 * buf then convert to string. the buf is saved and can be reused.
		 */
		public synchronized String getEntityStr() throws IOException {
			if (entityStr != null) return entityStr;

			Charset enc = null;
			if (contentType != null) {
				String c = contentType.toUpperCase();
				if (c.contains("UTF-8"))
					enc = Encodings.UTF_8;
				else if (c.contains("GBK"))
					enc = Encodings.GBK;
			}

			downloadEntity2Buf();
			if (enc == null) {
				enc = Encodings.isUTF8Bytes(entityBuf) ? Encodings.UTF_8 : Encodings.GBK;
			}
			entityStr = new String(entityBuf, enc);

			return entityStr;
		}

		/**
		 * download entire entity to memory. download will run only once.
		 */
		public synchronized void downloadEntity2Buf() throws IOException {
			if (entityBuf == null) {
				InputStream is = rsp.getContent();
				Thread thread = Thread.currentThread();

				ByteArrayOutputStream os = new ByteArrayOutputStream();
				byte[] buf = getDownloadBuf();
				int rl;
				while (!thread.isInterrupted() && (rl = is.read(buf)) > -1)
					os.write(buf, 0, rl);
				if (thread.isInterrupted())
					throw new InterruptedIOException("download interrupted: " + reqUrl);
				entityBuf = os.toByteArray();
				entityBuf = entityBuf == null ? EMPTY_BUF : entityBuf;
			}
		}

		/**
		 * buf entire entity then write. the buf is saved and can be reused.
		 */
		public synchronized void bufWriteTo(OutputStream out) throws IOException {
			downloadEntity2Buf();
			out.write(entityBuf);
			out.flush();
		}

		/**
		 * buf entire entity then save to file. the buf is saved and can be reused.<br>
		 * <b>WARNING</b>: make sure entire entity can be save to vm heap, or <code>OutOfMemoryError</code> will be thrown
		 *
		 * @return whether file is overwritten
		 * @throws IOException
		 */
		public synchronized boolean saveToFile(File file, boolean overwrite) throws IOException {
			if (!overwrite && file.exists())
				return false;

			downloadEntity2Buf();
			FilesUtil.writeFile(file, entityBuf);
			return true;
		}

		/**
		 * get entity buf. NOTICE: the buf is not copied, so it should be READ-ONLY.
		 */
		public byte[] getEntityBuf() throws IOException {
			downloadEntity2Buf();
			return this.entityBuf;
		}

		@Override
		public String toString() {
			return getCurrentURL();
		}
	}
}
