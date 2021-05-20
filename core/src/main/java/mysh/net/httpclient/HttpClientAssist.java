package mysh.net.httpclient;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.google.common.net.HttpHeaders;
import mysh.collect.Colls;
import mysh.util.*;
import okhttp3.*;
import org.apache.commons.compress.compressors.brotli.BrotliCompressorInputStream;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.net.ProxySelector;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

/**
 * HTTP 客户端组件.
 *
 * @author ZhangZhx
 */
@ThreadSafe
public class HttpClientAssist implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(HttpClientAssist.class);
	private static final HttpClientConfig defaultHcc = new HttpClientConfig();
	private HttpClientConfig hcc;
	private OkHttpClient client;
	private AtomicBoolean closeFlag = new AtomicBoolean(false);
	
	public HttpClientAssist() {
		this(null, null);
	}
	
	public HttpClientAssist(@Nullable HttpClientConfig hcc) {
		this(hcc, null);
	}
	
	public HttpClientAssist(@Nullable HttpClientConfig conf, @Nullable ProxySelector proxySelector) {
		hcc = conf == null ? defaultHcc : conf.clone();
		
		final OkHttpClient.Builder builder = new OkHttpClient.Builder()
				.connectTimeout(hcc.connectionTimeout, TimeUnit.MILLISECONDS)
				.readTimeout(hcc.soTimeout, TimeUnit.MILLISECONDS)
				.connectionPool(new ConnectionPool(hcc.maxIdolConnections, hcc.connPoolKeepAliveSec, TimeUnit.SECONDS))
				.proxySelector(ObjectUtils.firstNonNull(proxySelector, ProxySelector.getDefault()))
				.cookieJar(hcc.cookieJar);
		if (hcc.eventListener != null)
			builder.eventListener(hcc.eventListener);
		client = builder.build();
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
	 * @param headers request headers, can be null. use header name in {@link com.google.common.net.HttpHeaders}
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
	 * @param headers request headers, can be null. use header name in {@link com.google.common.net.HttpHeaders}
	 * @param params  request params, can be null.
	 * @throws IOException 连接异常.
	 */
	public UrlEntity access(String url, Map<String, ?> headers, Map<String, ?> params) throws IOException {
		if (params != null && params.size() > 0) {
			StringBuilder usb = new StringBuilder(url);
			if (!url.contains("?")) {
				usb.append('?');
			} else {
				usb.append('&');
			}
			for (Map.Entry<String, ?> pe : params.entrySet()) {
				usb.append(pe.getKey()).append('=').append(pe.getValue()).append('&');
			}
			if (usb.charAt(usb.length() - 1) == '&') {
				usb.deleteCharAt(usb.length() - 1);
			}
			url = usb.toString();
		}
		
		Request.Builder rb = new Request.Builder().url(url);
		return access(rb, headers);
	}
	
	/**
	 * get url entity by post form data.<br/>
	 * WARNING: the entity must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @param headers request headers, can be null. use header name in {@link com.google.common.net.HttpHeaders}
	 * @param params  request params. upload type: multipart/form-data, support files
	 * @throws IOException 连接异常.
	 */
	public UrlEntity accessPostMultipartForm(
			String url, @Nullable Map<String, ?> headers, @Nullable Map<String, ?> params) throws IOException {
		return accessPostMultipartForm(url, headers, params, null);
	}
	
	/**
	 * get url entity by post form data.<br/>
	 * WARNING: the entity must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @param headers request headers, can be null. use header name in {@link com.google.common.net.HttpHeaders}
	 * @param params  request params. upload type: multipart/form-data, support files
	 * @param enc     param value encoding
	 * @throws IOException 连接异常.
	 */
	public UrlEntity accessPostMultipartForm(
			String url, @Nullable Map<String, ?> headers, @Nullable Map<String, ?> params,
			@Nullable Charset enc) throws IOException {
		Request.Builder rb = new Request.Builder().url(url);
		if (Colls.isNotEmpty(params)) {
			MultipartBody.Builder mb = new MultipartBody.Builder();
			for (Map.Entry<String, ?> e : params.entrySet()) {
				String name = e.getKey();
				Object value = e.getValue();
				
				if (value instanceof File) {
					File file = (File) value;
					mb.addFormDataPart(name, file.getName(), RequestBody.create(null, file));
				} else {
					mb.addFormDataPart(name, String.valueOf(value));
				}
			}
			rb.post(mb.build());
		}
		return access(rb, headers);
	}
	
	/**
	 * @see #accessPostUrlEncodedForm(String, Map, Map, Charset)
	 */
	public UrlEntity accessPostUrlEncodedForm(
			String url, @Nullable Map<String, ?> headers, @Nullable Map<String, ?> params) throws IOException {
		return accessPostUrlEncodedForm(url, headers, params, null);
	}
	
	/**
	 * get url entity by post url encoded data.<br/>
	 * WARNING: the entity must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @param headers request headers, can be null. use header name in {@link com.google.common.net.HttpHeaders}
	 * @param params  request params. upload type: multipart/form-data, support files
	 * @param enc     param value encoding
	 * @throws IOException 连接异常.
	 */
	public UrlEntity accessPostUrlEncodedForm(
			String url, @Nullable Map<String, ?> headers, @Nullable Map<String, ?> params,
			@Nullable Charset enc) throws IOException {
		Request.Builder rb = new Request.Builder().url(url);
		if (Colls.isNotEmpty(params)) {
			FormBody.Builder fb = new FormBody.Builder(enc);
			for (Map.Entry<String, ?> e : params.entrySet()) {
				fb.add(e.getKey(), String.valueOf(e.getValue()));
			}
			rb.post(fb.build());
		}
		return access(rb, headers);
	}
	
	/**
	 * post raw content. content type can be set using header: {@link HttpHeaders#CONTENT_TYPE}<br/>
	 * get url entity by post method.<br/>
	 * WARNING: the entity must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @param headers request headers. highly recommend to use CONTENT_TYPE header to indicate content.
	 * @throws IOException 连接异常.
	 */
	public UrlEntity accessPostBytes(
			String url, @Nullable Map<String, String> headers, @Nullable byte[] buf) throws IOException {
		Request.Builder rb = new Request.Builder().url(url);
		if (buf != null) {
			String contentTypeHeader = Htmls.MIME_STREAM;
			if (headers != null)
				contentTypeHeader = headers.entrySet().stream()
				                           .filter(e -> HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(e.getKey()))
				                           .map(Map.Entry::getValue)
				                           .findAny().orElse(contentTypeHeader);
			rb.post(RequestBody.Companion.create(buf, MediaType.get(contentTypeHeader)));
		}
		return access(rb, headers);
	}
	
	/**
	 * get url entity.<br/>
	 * WARNING: the entity must be closed in time,
	 * because an unclosed entity will hold a connection from connection-pool.
	 *
	 * @param headers request headers, can be null. use header name in {@link com.google.common.net.HttpHeaders}
	 * @throws IOException 连接异常.
	 */
	private UrlEntity access(Request.Builder rb, Map<String, ?> headers) throws IOException {
		// 响应中断
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedIOException("access interrupted: " + rb.build());
		}
		
		if (Colls.isNotEmpty(hcc.headers) || Colls.isNotEmpty(headers)) {
			if (Colls.isEmpty(hcc.headers))
				for (Map.Entry<String, ?> e : headers.entrySet()) {
					rb.addHeader(e.getKey(), String.valueOf(e.getValue()));
				}
			else if (Colls.isEmpty(headers))
				for (Map.Entry<String, Object> e : hcc.headers.entrySet()) {
					rb.addHeader(e.getKey(), String.valueOf(e.getValue()));
				}
			else {
				Map<String, Object> hm = new HashMap<>(hcc.headers);
				hm.putAll(headers);
				for (Map.Entry<String, Object> e : hm.entrySet()) {
					rb.addHeader(e.getKey(), String.valueOf(e.getValue()));
				}
			}
		}
		
		rb.addHeader(HttpHeaders.CONNECTION, hcc.isKeepAlive ? "Keep-Alive" : "close");
		rb.addHeader(HttpHeaders.USER_AGENT, hcc.userAgent);
		
		return new UrlEntity(rb);
	}
	
	public boolean isClosed() {
		return closeFlag.get();
	}
	
	@Override
	public void close() {
		if (closeFlag.compareAndSet(false, true)) {
			try {
				client.connectionPool().evictAll();
				Cache cache = client.cache();
				if (cache != null)
					cache.delete();
			} catch (Exception e) {
				log.error("hca close error", e);
			}
		}
	}
	
	/**
	 * download big resource and save to file.
	 * support breakpoint resume.
	 *
	 * @param headers   can be null
	 * @param overwrite overwrite exist file or rename new file
	 * @param stopChk   check stop download or not: check(retryTimes).
	 * @return write successfully or not
	 */
	public boolean saveDirectlyToFile(
			String url, @Nullable Map<String, ?> headers, File file, boolean overwrite, @Nullable Function<Integer, Boolean> stopChk
	) {
		File writableFile = overwrite ? file : FilesUtil.getWritableFile(file);
		File writeFile = new File(writableFile.getPath() + ".~write~");
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
		
		Map<String, Object> accHeaders = headers == null ? new HashMap<>() : new HashMap<>(headers);
		boolean supportResume = true, result = false;
		int retryTimes = 0;
		Thread thread = Thread.currentThread();
		while (!thread.isInterrupted()) {
			if (stopChk != null && Objects.equals(Boolean.TRUE, stopChk.apply(retryTimes))) {
				break;
			}
			accHeaders.put(HttpHeaders.RANGE, "bytes=" + writeFile.length() + "-");
			
			try (UrlEntity ue = this.access(url, accHeaders)) {
				String rhAcceptRanges = ue.rsp.header(HttpHeaders.ACCEPT_RANGES);
				String rhContentRange = ue.rsp.header(HttpHeaders.CONTENT_RANGE);
				supportResume = Strings.isNotBlank(rhAcceptRanges) && !"none".equals(rhAcceptRanges) || Strings.isNotBlank(rhContentRange);
				
				if (ue.getStatusCode() >= 400) {
					log.error("download-file-fail, status={}, url={}", ue.getStatusCode(), url);
				} else
					result = ue.downloadDirectlyToFile(writeFile, retryTimes, stopChk);
				
				break;
			} catch (Exception e) {
				retryTimes++;
				
				if (writeFile.length() == 0) {
					log.error("download-file-with-exp, exp={}, url={}", e, url);
					break;
				} else if (supportResume) {
					log.info("resume-breakpoint, exp={}, url={}", e, url);
				} else {
					log.error("download-file-exp,no-breakpoint-resume-support, exp={}, url={}", e, url);
					break;
				}
			}
			
			Times.sleepNoExp(5000);
		}
		
		if (result) {
			writableFile.delete();
			writeFile.renameTo(writableFile);
		} else {
			log.error("download-file-failed, file={}, url={}", writeFile.getAbsolutePath(), url);
		}
		return result;
	}
	
	/**
	 * download resource to memory then save to file.
	 *
	 * @param url       数据文件地址
	 * @param headers   请求头, 可为 null
	 * @param overwrite overwrite exist file or rename new file
	 * @return whether file is overwritten
	 * @throws Exception IO异常
	 */
	public boolean saveToFile(
			String url, @Nullable Map<String, ?> headers, File file, boolean overwrite
	) throws IOException {
		try (UrlEntity ue = this.access(url, headers)) {
			return ue.saveToFile(file, overwrite);
		}
	}
	
	/**
	 * 将带有 ./ 或 ../ 或 // 或 \ 的 URI 转换成简短的 URL 形式.
	 * uriString needs schema
	 */
	public static String getShortURL(String uriString) {
		
		if (Strings.isBlank(uriString)) {
			return "";
		}
		
		uriString = uriString.replace('\\', '/');
		if (!uriString.contains("./") && uriString.indexOf("//", 8) < 0) {
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
					&& !("http".equalsIgnoreCase(uri.getScheme()) && uri.getPort() == 80)
					&& !("https".equalsIgnoreCase(uri.getScheme()) && uri.getPort() == 443)) {
				url.append(":");
				url.append(uri.getPort());
			}
		}
		
		// 处理 URL Path 中的 . 和 ..
		String[] tPath = uri.getRawPath().split("/");
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
					if (!lastUnNullBlankIndex.isEmpty())
						tPath[lastUnNullBlankIndex.pop()] = null;
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
		if (uri.getRawQuery() != null) {
			url.append("?");
			url.append(uri.getRawQuery());
		}
		
		// 处理形如 g.cn/search?q=2 的情况
		if (uri.getScheme() == null && !uri.getRawPath().startsWith("/")) {
			url.delete(0, 1);
		}
		
		return url.toString();
	}
	
	/**
	 * parse headers string in lines.
	 * ignore <code>accept-encoding</code> to avoid unusual data (e.g. gzip) return
	 */
	public static Map<String, String> parseHeaders(String headerStr) {
		if (Strings.isNotBlank(headerStr)) {
			Map<String, String> hm = Maps.newHashMap();
			for (String line : headerStr.trim().split("[\\r\\n]+")) {
				String[] header = line.split(": *", 2);
				if (!header[0].equalsIgnoreCase("accept-encoding"))
					hm.put(header[0], header[1]);
			}
			return hm;
		} else {
			return Collections.emptyMap();
		}
	}
	
	/**
	 * parse params string like key=value&k=v
	 */
	public static Map<String, String> parseParams(String paramStr) {
		if (Strings.isNotBlank(paramStr)) {
			Map<String, String> pm = new HashMap<>();
			for (String kv : paramStr.split("&")) {
				String[] kav = kv.split("=", 2);
				pm.put(kav[0], kav[1]);
			}
			return pm;
		} else {
			return Collections.emptyMap();
		}
	}
	
	private static final byte[] EMPTY_BUF = new byte[0];
	private static final int DOWNLOAD_BUF_LEN = 100_000;
	private static final ThreadLocal<byte[]> threadDownloadBuf = new ThreadLocal<>();
	
	private static byte[] getDownloadBuf() {
		byte[] buf = threadDownloadBuf.get();
		if (buf == null) {
			threadDownloadBuf.set(buf = new byte[DOWNLOAD_BUF_LEN]);
		}
		return buf;
	}
	
	@NotThreadSafe
	public final class UrlEntity implements Closeable {
		
		private final Request req;
		private final String reqUrl;
		private final Call call;
		private final Response rsp;
		private String currentUrl;
		private MediaType contentType;
		private byte[] entityBuf;
		private String entityStr;
		private Charset entityEncoding;
		private boolean closed;
		
		public UrlEntity(Request.Builder rb) throws IOException {
			try {
				req = rb.build();
				// can't lazy init, because it changes after rb.execute()
				reqUrl = req.url().toString();
				
				call = client.newCall(req);
				rsp = call.execute();
				
				int statusCode = rsp.code();
				if (statusCode >= 400) {
					log.warn("access unsuccessful, status={}, msg={}, req={}, curr={}",
							statusCode, rsp.message(), this.reqUrl, this.getCurrentURL());
				}
				contentType = rsp.body().contentType();
			} catch (IOException e) {
				this.close();
				throw e;
			}
		}
		
		/**
		 * close the connection immediately, unfinished download will be aborted.
		 */
		@Override
		public void close() {
			if (closed) return;
			try {
				closed = true;
				if (rsp != null)
					rsp.close();
			} catch (Exception e) {
				log.debug("close connection error. " + e);
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
			return rsp.request().url().scheme();
		}
		
		/**
		 * @return request reqUrl may jump several times, get the real access url.
		 */
		public String getCurrentURL() {
			if (this.currentUrl == null) {
				this.currentUrl = rsp.request().url().toString();
			}
			
			return this.currentUrl;
		}
		
		/**
		 * get response status.
		 */
		public int getStatusCode() {
			return rsp.code();
		}
		
		/**
		 * get response header
		 *
		 * @param name see {@link com.google.common.net.HttpHeaders}
		 */
		public String getRspHeader(String name) {
			return rsp.header(name);
		}
		
		/**
		 * a request is text by default if the header(content-type) is not given.
		 * see {@link #isContentType(String)}
		 */
		public boolean isText() {
			return contentType == null || Objects.equals(contentType.type(), "text") || isJson();
		}
		
		/**
		 * <b>IMPORTANT:</b> see {@link #isContentType(String)}
		 */
		public boolean isJson() {
			return isContentType("json");
		}
		
		/**
		 * <b>IMPORTANT:</b> see {@link #isContentType(String)}
		 */
		public boolean isHtml() {
			return contentType != null && Objects.equals(contentType.subtype(), "html");
		}
		
		/**
		 * <b>IMPORTANT:</b> see {@link #isContentType(String)}
		 */
		public boolean isImage() {
			return contentType != null && Objects.equals(contentType.type(), "image");
		}
		
		/**
		 * content-type(FROM response header) check.<br/>
		 * if the header is not given or incorrect, the judgement will be incorrect.
		 * So use this ONLY if file extension judgement can not work.
		 */
		public boolean isContentType(String type) {
			return contentType != null && (
					contentType.type().equalsIgnoreCase(type) || contentType.subtype().equalsIgnoreCase(type)
			);
		}
		
		/**
		 * content length in byte size. return -1 if length not given (response header).
		 */
		public long getContentLength() {
			return rsp.body().contentLength();
		}
		
		/**
		 * buf then convert to string. the buf is saved and can be reused.
		 */
		public synchronized String getEntityStr() throws IOException {
			if (entityStr != null) {
				return entityStr;
			}
			
			downloadEntityAndParseEncoding();
			entityStr = new String(entityBuf, entityEncoding);
			if (isJson())
				entityStr = JSON.parse(entityStr).toString();
			
			return entityStr;
		}
		
		public synchronized JSONObject getJsonObj() throws IOException {
			if (entityStr == null) {
				downloadEntityAndParseEncoding();
				entityStr = new String(entityBuf, entityEncoding);
			}
			
			return JSON.parseObject(entityStr);
		}
		
		public synchronized JSONArray getJsonArray() throws IOException {
			if (entityStr == null) {
				downloadEntityAndParseEncoding();
				entityStr = new String(entityBuf, entityEncoding);
			}
			
			return JSON.parseArray(entityStr);
		}
		
		private void downloadEntityAndParseEncoding() throws IOException {
			downloadEntity2Buf();
			
			if (this.entityEncoding == null) {
				
				Charset enc = null;
				if (contentType != null) {
					enc = contentType.charset();
				}
				if (enc == null) {
					enc = Encodings.findHtmlEncoding(entityBuf);
				}
				if (enc == null) {
					enc = Encodings.isUTF8Bytes(entityBuf) ? Encodings.UTF_8 : Encodings.GBK;
				}
				this.entityEncoding = enc;
			}
		}
		
		/**
		 * download entity content then parse encoding. useful in text content.
		 */
		public Charset getEntityEncoding() throws IOException {
			this.downloadEntityAndParseEncoding();
			return this.entityEncoding;
		}
		
		private ThreadLocal<byte[]> decompressBuf = ThreadLocal.withInitial(() -> new byte[100_000]);
		
		/**
		 * download entire entity to memory. download will run only once.
		 */
		public synchronized void downloadEntity2Buf() throws IOException {
			if (entityBuf == null) {
				try {
					String contentEncoding = rsp.header(HttpHeaders.CONTENT_ENCODING);
					if (rsp.body() != null) {
						if (contentEncoding == null) {
							entityBuf = rsp.body().bytes();
						} else {
							InputStream bodyStream = rsp.body().byteStream();
							boolean decompress = true;
							switch (contentEncoding) {
								case "gzip":
									bodyStream = new GZIPInputStream(bodyStream);
									break;
								case "deflate":
									bodyStream = new DeflaterInputStream(bodyStream);
									break;
								case "br":
									bodyStream = new BrotliCompressorInputStream(bodyStream);
									break;
								default:
									decompress = false;
									entityBuf = rsp.body().bytes();
							}
							
							if (decompress) {
								byte[] buf = decompressBuf.get();
								int len;
								ByteArrayOutputStream out = new ByteArrayOutputStream();
								while ((len = bodyStream.read(buf)) > 0) {
									out.write(buf, 0, len);
								}
								entityBuf = out.toByteArray();
							}
						}
					}
					
					entityBuf = entityBuf == null ? EMPTY_BUF : entityBuf;
				} catch (IllegalStateException e) {
					throw new SocketException(e.toString());
				}
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
		 * <b>WARNING</b>: make sure entire entity can be save to vm heap, or <code>OutOfMemoryError</code> will be
		 * thrown
		 *
		 * @return whether file is overwritten
		 * @throws IOException
		 */
		public synchronized boolean saveToFile(File file, boolean overwrite) throws IOException {
			if (!overwrite && file.exists()) {
				return false;
			}
			
			downloadEntity2Buf();
			if (!file.getParentFile().exists())
				file.getParentFile().mkdirs();
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
		
		public String getCookies() {
			return ObjectUtils.firstNonNull(rsp.header("Cookie"), rsp.header("cookie"));
		}
		
		@Override
		public String toString() {
			return getCurrentURL();
		}
		
		/**
		 * download resource directly to file, without cache to entityBuf.
		 * WARNING: after downloading the content directly to file, the ue content can't be read again.
		 *
		 * @param writeFile file writing will be create/append
		 * @param stopChk   check stop download or not: check(retryTimes).
		 * @return write successfully or not
		 */
		private synchronized boolean downloadDirectlyToFile(
				File writeFile, int retryTimes, @Nullable Function<Integer, Boolean> stopChk) throws Exception {
			if (stopChk != null && Objects.equals(Boolean.TRUE, stopChk.apply(retryTimes))) {
				return false;
			}
			
			try (OutputStream out = Files.newOutputStream(writeFile.toPath(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
			     InputStream is = rsp.body().byteStream()) {
				Thread thread = Thread.currentThread();
				
				byte[] buf = getDownloadBuf();
				int rl;
				while (!thread.isInterrupted() && (rl = is.read(buf)) > -1) {
					out.write(buf, 0, rl);
					if (stopChk != null && Objects.equals(Boolean.TRUE, stopChk.apply(retryTimes))) {
						call.cancel();
						return false;
					}
				}
				if (thread.isInterrupted()) {
					throw new InterruptedIOException("download interrupted: " + reqUrl);
				}
			}
			return true;
		}
	}
}
