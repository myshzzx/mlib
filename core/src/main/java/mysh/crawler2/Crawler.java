package mysh.crawler2;

import mysh.annotation.Immutable;
import mysh.annotation.ThreadSafe;
import mysh.net.httpclient.HttpClientAssist;
import mysh.net.httpclient.HttpClientConfig;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ConnectTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.max;

/**
 * @author Mysh
 * @since 2014/9/24 12:50
 */
@ThreadSafe
public class Crawler {
	private final Logger log;

	private final HttpClientAssist hca;
	private final CrawlerSeed seed;
	private final String name;

	private final AtomicReference<Status> status = new AtomicReference<>(Status.INIT);

	private ThreadPoolExecutor e;
	private final BlockingQueue<Runnable> wq = new LinkedBlockingQueue<>();
	private final Queue<String> unhandledUrls = new ConcurrentLinkedQueue<>();

	/**
	 * create a crawler, to start, invoke {@link #start()}. <br/>
	 * WARNING: thread pool size and connection pool size should not be a big diff,
	 * which will cause race condition in connection resource,
	 * and prevent interrupted thread from leaving wait state in time when shutting down thread pool.
	 */
	public Crawler(CrawlerSeed seed, HttpClientConfig hcc) throws Exception {
		Objects.requireNonNull(seed, "need seed");
		Objects.requireNonNull(hcc, "need http client config");

		this.seed = seed;
		this.seed.init();

		String seedName = seed.getClass().getName();
		int dotIdx = seedName.lastIndexOf('.');
		this.name = "Crawler[" +
						(dotIdx > -1 ? seedName.substring(dotIdx + 1) : seedName)
						+ "]";

		this.log = LoggerFactory.getLogger(this.name);

		this.hca = new HttpClientAssist(hcc);
	}

	private void startAutoStopChk() {
		new Thread(name + "-autoStopChk") {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(5000);
						log.debug("wq.size=" + wq.size() + ", e.actCount=" + e.getActiveCount()
										+ ", unhandled.size=" + unhandledUrls.size());
						if (wq.size() == 0 && e.getActiveCount() == 0) {
							Crawler.this.stop();
							return;
						}
					} catch (Exception ex) {
						log.error("error on stop check", ex);
					}
				}
			}
		}.start();
	}

	public void start() {
		if (!status.compareAndSet(Status.INIT, Status.RUNNING)) {
			throw new RuntimeException(toString() + " can't be started, current status=" + status.get());
		}

		int threadSize = seed.requestThreadSize();
		int maxThreadSize = Runtime.getRuntime().availableProcessors() * 50;
		threadSize = Math.min(max(1, threadSize), maxThreadSize);
		log.info(toString() + " max thread size:" + threadSize);

		AtomicInteger threadCount = new AtomicInteger(0);
		e = new ThreadPoolExecutor(threadSize, threadSize, 15, TimeUnit.SECONDS, wq,
						r -> {
							Thread t = new Thread(r, name + "-" + threadCount.incrementAndGet());
							t.setDaemon(true);
							return t;
						},
						(r, executor) -> unhandledUrls.add(((Worker) r).url)
		);
		e.allowCoreThreadTimeOut(true);

		log.info(this.name + " started.");

		seed.getSeeds().stream()
						.forEach(url -> e.execute(new Worker(url)));

		if (seed.autoStop())
			startAutoStopChk();
	}

	public void stop() {
		Status oldStatus = status.getAndSet(Status.STOPPED);
		if (oldStatus == Status.STOPPED) return;

		try {
			e.shutdownNow().stream()
							.forEach(r -> unhandledUrls.add(((Worker) r).url));
			e.awaitTermination(2, TimeUnit.MINUTES);
		} catch (InterruptedException ex) {
			log.debug(this.name + " stopping interrupted.", ex);
		} finally {
			log.info(this.name + " stopped.");
			hca.close();
			seed.onCrawlerStopped(unhandledUrls);
		}
	}

	public Status getStatus() {
		return status.get();
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return this.name;
	}

	private static final Pattern httpExp =
					Pattern.compile("[Hh][Tt][Tt][Pp][Ss]?:[^\"'<>\\s#]+");
	private static final Pattern srcValueExp =
					Pattern.compile("(([Hh][Rr][Ee][Ff])|([Ss][Rr][Cc]))[\\s]*=[\\s]*[\"']([^\"'#]*)");

	@Immutable
	private class Worker implements Runnable {
		private final String url;

		public Worker(String url) {
			this.url = url;
		}

		@Override
		public void run() {
			if (!seed.accept(this.url)) return;

			try (HttpClientAssist.UrlEntity ue = hca.access(url)) {
				if (!seed.accept(ue.getCurrentURL()) || !seed.accept(ue.getReqUrl())) return;
				if (status.get() == Status.STOPPED) {
					unhandledUrls.offer(url);
					return;
				}

				log.debug("on page: " + ue.getCurrentURL());

				if (!seed.onGet(ue))
					e.execute(this);

				if (ue.isText() && seed.needDistillUrl(ue)) {
					Set<String> distilledUrl = distillUrl(ue);
					seed.afterDistillingUrl(ue, distilledUrl)
									.filter(seed::accept)
									.forEach(dUrl -> e.execute(new Worker(dUrl)));
				}
			} catch (SocketTimeoutException | SocketException | ConnectTimeoutException | UnknownHostException ex) {
				e.execute(this);
				log.debug(ex.toString() + " - " + url);
			} catch (Exception ex) {
				if (!isMalformedUrl(ex))
					unhandledUrls.offer(url);
				log.error("on error handling url: " + this.url, ex);
			}
		}

		private Set<String> distillUrl(HttpClientAssist.UrlEntity ue) throws IOException {
			ProtocolVersion protocol = ue.getProtocol();
			String pageUrl = ue.getCurrentURL();
			String pageContent = ue.getEntityStr();

			Set<String> urls = new HashSet<>();

			try {
				// 查找 http 开头的数据
				Matcher httpExpMatcher = httpExp.matcher(pageContent);
				while (httpExpMatcher.find()) {
					urls.add(HttpClientAssist.getShortURL(httpExpMatcher.group()));
				}

				// 取得当前根目录
				String currentRoot = pageUrl;
				int sepIndex;
				// 取 ? 前面的串
				if ((sepIndex = currentRoot.lastIndexOf('?')) != -1) {
					currentRoot = currentRoot.substring(0, sepIndex);
				}
				// 取 # 前面的串
				if ((sepIndex = currentRoot.lastIndexOf('#')) != -1) {
					currentRoot = currentRoot.substring(0, sepIndex);
				}
				// 取根目录
				currentRoot = currentRoot.substring(0, currentRoot.lastIndexOf('/') + 1);

				// 查找 href 指向的地址
				Matcher srcValueMatcher = srcValueExp.matcher(pageContent);
				String tValue;
				String protocolPrefix = protocol.getProtocol().toLowerCase() + ":";
				while (srcValueMatcher.find()) {
					tValue = srcValueMatcher.group(4);
					if (tValue == null || tValue.length() == 0 || tValue.startsWith("#")) {
						continue;
					} else if (tValue.startsWith("http:") || tValue.startsWith("https:")) {
						urls.add(tValue);
					} else if (tValue.startsWith("//")) {
						urls.add(protocolPrefix + tValue);
					} else if (tValue.startsWith("/")) {
						urls.add(currentRoot.substring(0, currentRoot.indexOf('/', 9)) + tValue);
					} else {
						urls.add(HttpClientAssist.getShortURL(currentRoot + tValue));
					}
				}
			} catch (Exception e) {
				log.error("分析页面链接时异常: " + pageUrl, e);
			}


			Set<String> unEscapedUrls = new HashSet<>();
			for (String url : urls) {
				unEscapedUrls.add(url.replace('\\', '/')
								.replace("&amp;", "&")
								.replace("&lt;", "<")
								.replace("&gt;", ">")
								.replace("&quot;", "\""));
			}

			return unEscapedUrls;
		}

		private boolean isMalformedUrl(Exception ex) {
			return ex.getCause() instanceof URISyntaxException
							|| ex instanceof ClientProtocolException
							|| ex instanceof MalformedURLException
							;
		}
	}

	public static enum Status {
		INIT, RUNNING, STOPPED
	}
}
