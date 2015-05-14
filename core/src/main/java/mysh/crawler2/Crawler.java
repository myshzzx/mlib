package mysh.crawler2;

import mysh.annotation.Immutable;
import mysh.annotation.ThreadSafe;
import mysh.net.httpclient.HttpClientAssist;
import mysh.net.httpclient.HttpClientConfig;
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
import java.util.stream.Stream;

import static java.lang.Math.max;

/**
 * Web Crawler. Give it a seed, it will hunt for anything you need.
 *
 * @author Mysh
 * @since 2014/9/24 12:50
 */
@ThreadSafe
public class Crawler<CTX extends UrlContext> {
	private final Logger log;

	private final HttpClientAssist hca;
	private final CrawlerSeed<CTX> seed;
	private final String name;

	private final AtomicReference<Status> status = new AtomicReference<>(Status.INIT);

	private ThreadPoolExecutor e;
	private final BlockingQueue<Runnable> wq = new LinkedBlockingQueue<>();
	private final Queue<UrlCtxHolder<CTX>> unhandledTasks = new ConcurrentLinkedQueue<>();

	/**
	 * create a crawler, to start, invoke {@link #start()}. <br/>
	 * WARNING: thread pool size and connection pool size should not be a big diff,
	 * which will cause race condition in connection resource,
	 * and prevent interrupted thread from leaving wait state in time when shutting down thread pool.
	 */
	public Crawler(CrawlerSeed<CTX> seed, HttpClientConfig hcc) throws Exception {
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

	/**
	 * start auto-check-stop thread. if there's nothing to crawl, it will stop the crawler.
	 */
	private void startAutoStopChk() {
		new Thread(name + "-autoStopChk") {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(5000);
						log.debug("wq.size=" + wq.size() + ", e.actCount=" + e.getActiveCount()
										+ ", unhandled.size=" + unhandledTasks.size());
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

	/**
	 * start the crawler. a crawler can't be restarted.
	 */
	@SuppressWarnings("unchecked")
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
						(work, executor) -> {
							Worker w = (Worker) work;
							unhandledTasks.add(new UrlCtxHolder<>(w.url, w.ctx));
						}
		);
		e.allowCoreThreadTimeOut(true);

		log.info(this.name + " started.");

		seed.getSeeds()
						.forEach(ctxHolder -> e.execute(new Worker(ctxHolder)));

		if (seed.autoStop())
			startAutoStopChk();
	}

	/**
	 * pause, until resumed. effect only in Running state.
	 */
	public void pause() {
		this.status.compareAndSet(Status.RUNNING, Status.PAUSED);
	}

	/**
	 * resume to running state. effect only in pause state.
	 */
	public void resume() {
		this.status.compareAndSet(Status.PAUSED, Status.RUNNING);
	}

	/**
	 * stop the crawler, which release all the resources it took.
	 * WARNING: can't restart after stopping.
	 */
	@SuppressWarnings("unchecked")
	public void stop() {
		Status oldStatus = status.getAndSet(Status.STOPPED);
		if (oldStatus == Status.STOPPED) return;

		try {
			e.shutdownNow().stream()
							.forEach(r -> {
								Worker work = (Worker) r;
								unhandledTasks.add(new UrlCtxHolder<>(work.url, work.ctx));
							});
			e.awaitTermination(2, TimeUnit.MINUTES);
		} catch (InterruptedException ex) {
			log.debug(this.name + " stopping interrupted.", ex);
		} finally {
			log.info(this.name + " stopped.");
			hca.close();
			seed.onCrawlerStopped(unhandledTasks);
		}
	}

	/**
	 * get current crawler state.
	 */
	public Status getStatus() {
		return status.get();
	}

	/**
	 * get crawler name.
	 */
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
		private final CTX ctx;

		public Worker(UrlCtxHolder<CTX> holder) {
			this.url = holder.url;
			this.ctx = holder.ctx;
		}

		@Override
		public void run() {
			if (!seed.accept(this.url, this.ctx)) return;

			try {
				while (Crawler.this.status.get() == Status.PAUSED) {
					Thread.sleep(50);
				}

				try (HttpClientAssist.UrlEntity ue = hca.access(url)) {
					if (!seed.accept(ue.getCurrentURL(), this.ctx) || !seed.accept(ue.getReqUrl(), this.ctx))
						return;
					if (status.get() == Status.STOPPED) {
						unhandledTasks.offer(new UrlCtxHolder<>(url, ctx));
						return;
					}

					log.debug("onGet= " + ue.getCurrentURL() + ", reqUrl= " + ue.getReqUrl());

					if (!seed.onGet(ue, this.ctx))
						e.execute(this);

					if (ue.isText() && seed.needToDistillUrls(ue, this.ctx)) {
						Stream<String> distilledUrls = seed.afterDistillingUrls(ue, this.ctx, distillUrl(ue));
						seed.distillUrlCtx(ue, this.ctx, distilledUrls)
										.filter(h -> seed.accept(h.url, h.ctx))
										.forEach(h -> e.execute(new Worker(h)));
					}
				}
			} catch (SocketTimeoutException | SocketException | ConnectTimeoutException | UnknownHostException ex) {
				e.execute(this);
				log.debug(ex.toString() + " - " + this.url);
			} catch (InterruptedException ex) {
				unhandledTasks.offer(new UrlCtxHolder<>(url, ctx));
			} catch (Exception ex) {
				if (!isMalformedUrl(ex))
					unhandledTasks.offer(new UrlCtxHolder<>(url, ctx));
				log.error("on error handling url: " + this.url, ex);
			}
		}

		private Stream<String> distillUrl(HttpClientAssist.UrlEntity ue) throws IOException {
			String pageContent = ue.getEntityStr();

			Set<String> urls = new HashSet<>();

			try {
				// 查找 http 开头的数据
				Matcher httpExpMatcher = httpExp.matcher(pageContent);
				while (httpExpMatcher.find()) {
					urls.add(HttpClientAssist.getShortURL(httpExpMatcher.group()));
				}

				// 取得当前根目录
				String currentRoot = ue.getCurrentURL();
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
				String protocolPrefix = ue.getProtocol().getProtocol().toLowerCase() + ":";
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
				log.error("分析页面链接时异常: " + ue.getCurrentURL(), e);
			}

			return urls.stream().map(
							url -> url.replace('\\', '/')
											.replace("////", "//")
											.replace("&amp;", "&")
											.replace("&lt;", "<")
											.replace("&gt;", ">")
											.replace("&quot;", "\""));
		}

		private boolean isMalformedUrl(Exception ex) {
			return ex.getCause() instanceof URISyntaxException
							|| ex instanceof ClientProtocolException
							|| ex instanceof MalformedURLException
							;
		}
	}

	/**
	 * crawler state.
	 */
	public enum Status {
		INIT, RUNNING, PAUSED, STOPPED
	}
}
