package mysh.crawler2;

import mysh.annotation.GuardedBy;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Web Crawler. Give it a seed, it will hunt for anything you need.<p/>
 * create a crawler, to start, invoke {@link #start()}. <br/>
 * WARNING: <br/>
 * 1) thread pool size and connection pool size should not be a big diff,
 * which will cause race condition in connection resource,
 * and prevent interrupted thread from leaving wait state in time when shutting down thread pool.
 * <br/>
 * 2) crawler only start daemon threads, so it will stop if there's no non-daemon thread running.
 *
 * @author Mysh
 * @since 2014/9/24 12:50
 * @version 3.0
 */
@ThreadSafe
public class Crawler<CTX extends UrlContext> {
	private final Logger log;

	private final CrawlerSeed<CTX> seed;
	private final String name;

	private final AtomicReference<Status> status = new AtomicReference<>(Status.INIT);
	private final UrlClassifierConf.Factory<CTX> ccf;

	private final Queue<UrlCtxHolder<CTX>> unhandledTasks = new ConcurrentLinkedQueue<>();

	public Crawler(CrawlerSeed<CTX> seed, HttpClientConfig hcc) throws Exception {
		this(seed, (url, ctx) -> new UrlClassifierConf("default", hcc.getMaxConnTotal(), Integer.MAX_VALUE, hcc));
	}

	public Crawler(CrawlerSeed<CTX> seed, UrlClassifierConf.Factory<CTX> ccf) throws Exception {
		Objects.requireNonNull(seed, "need seed");
		Objects.requireNonNull(ccf, "need classifier config factory");

		this.seed = seed;
		this.seed.init();

		String seedName = seed.getClass().getName();
		int dotIdx = seedName.lastIndexOf('.');
		this.name = "Crawler(" +
						(dotIdx > -1 ? seedName.substring(dotIdx + 1) : seedName)
						+ ")";

		this.log = LoggerFactory.getLogger(this.name);
		this.ccf = ccf;
	}

	/**
	 * start auto-check-stop thread. if there's nothing to crawl, it will stop the crawler.
	 */
	private void startAutoStopChk() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				Crawler.this.stop();
			}
		});

		Thread t = new Thread(name + "-autoStopChk") {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(5000);

						StringBuilder ucStatus = new StringBuilder("crawler current status: " + name);
						AtomicBoolean crawlerRunning = new AtomicBoolean(false);
						classifiers.values().stream()
										.forEach(uc -> {
											ucStatus.append("\n{Classifier:");
											ucStatus.append(uc.name);
											ucStatus.append(", queueSize=");
											ucStatus.append(uc.wq.size());
											ucStatus.append(", actCount=");
											ucStatus.append(uc.exec.getActiveCount());
											ucStatus.append("}");
											if (uc.wq.size() > 0 || uc.exec.getActiveCount() > 0)
												crawlerRunning.set(true);
										});
						ucStatus.append("\nunhandled.size=");
						ucStatus.append(unhandledTasks.size());
						log.debug(ucStatus.toString());

						if (!crawlerRunning.get()) {
							Crawler.this.stop();
							return;
						}
					} catch (Exception ex) {
						log.error("error on stop check", ex);
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

	/**
	 * start the crawler. a crawler can't be restarted.
	 */
	@SuppressWarnings("unchecked")
	public void start() {
		if (!this.status.compareAndSet(Status.INIT, Status.RUNNING)) {
			throw new RuntimeException(toString() + " can't be started, current status=" + status.get());
		}

		log.info(this.name + " started.");

		seed.getSeeds().forEach(ctxHolder -> classify(ctxHolder.url, ctxHolder.ctx));

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
	 * stop the crawler, which release all the resources it took. BLOCKED until entire crawler stopped.<br/>
	 * WARNING: can't restart after stopping.
	 */
	@SuppressWarnings("unchecked")
	public void stop() {
		Status oldStatus = status.getAndSet(Status.STOPPED);
		if (oldStatus == Status.STOPPED) return;

		try {
			classifiers.values().forEach(UrlClassifier::stop);
			classifiers.values().forEach(c -> c.awaitTermination(2, TimeUnit.MINUTES));
		} finally {
			log.info(this.name + " stopped.");
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
		private final UrlClassifier classifier;

		public Worker(String url, CTX ctx, UrlClassifier classifier) {
			this.url = url;
			this.ctx = ctx;
			this.classifier = classifier;
		}

		@Override
		public void run() {
			if (!seed.accept(this.url, this.ctx)) return;

			try {
				while (status.get() == Status.PAUSED) {
					Thread.sleep(50);
				}

				try (HttpClientAssist.UrlEntity ue = classifier.access(url)) {
					if (!seed.accept(ue.getCurrentURL(), this.ctx) || !seed.accept(ue.getReqUrl(), this.ctx))
						return;
					if (status.get() == Status.STOPPED) {
						unhandledTasks.offer(new UrlCtxHolder<>(url, ctx));
						return;
					}

					log.debug("onGet= " + ue.getCurrentURL() + ", reqUrl= " + ue.getReqUrl());

					if (!seed.onGet(ue, this.ctx))
						classifier.crawl(this);

					if (ue.isText() && seed.needToDistillUrls(ue, this.ctx)) {
						seed.afterDistillingUrls(ue, this.ctx, distillUrl(ue))
										.filter(h -> seed.accept(h.url, h.ctx))
										.forEach(h -> classify(h.url, h.ctx));
					}
				}
			} catch (SocketTimeoutException | SocketException | ConnectTimeoutException | UnknownHostException ex) {
				classifier.crawl(this);
				log.debug("network problem: " + ex.toString() + " - " + this.url);
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

	private final Map<UrlClassifierConf, UrlClassifier> classifiers = new ConcurrentHashMap<>();

	/**
	 * classify the url and put it into working queue.
	 */
	private void classify(String url, CTX ctx) {
		if (this.status.get() == Status.STOPPED)
			throw new RuntimeException("crawler has been stopped");

		UrlClassifierConf ucConf = ccf.get(url, ctx);
		if (ucConf == null)
			throw new RuntimeException("get null classifier config with param: url=" + url + ", ctx=" + ctx);
		UrlClassifier classifier = classifiers.computeIfAbsent(ucConf, UrlClassifier::new);
		classifier.crawl(url, ctx);
	}

	private static final AtomicLong classifierThreadCount = new AtomicLong(0);

	class UrlClassifier {
		private final String name;

		private final BlockingQueue<Runnable> wq = new LinkedBlockingQueue<>();
		private final ThreadPoolExecutor exec;

		private volatile int milliSecStep;
		private final HttpClientAssist hca;

		@SuppressWarnings("unchecked")
		UrlClassifier(UrlClassifierConf conf) {
			Objects.requireNonNull(conf, "conf should not be null");

			this.name = Objects.requireNonNull(conf.name, "name can't be null");
			Objects.requireNonNull(conf.hcc, "hcc should not be null");
			this.hca = new HttpClientAssist(conf.hcc);

			setRatePerMinute(conf.ratePerMinute);

			exec = new ThreadPoolExecutor(conf.threadPoolSize, conf.threadPoolSize, 15L, TimeUnit.SECONDS, wq,
							r -> {
								Thread t = new Thread(r, this.name + "-T-" + classifierThreadCount.incrementAndGet());
								t.setDaemon(true);
								return t;
							},
							(work, executor) -> {
								Worker w = (Worker) work;
								unhandledTasks.add(new UrlCtxHolder<>(w.url, w.ctx));
							}
			);
			exec.allowCoreThreadTimeOut(true);
		}

		/**
		 * @param rate url max handle rate per minute. (the max accurate value
		 *             is {@link UrlClassifierConf#maxAccRatePM}. if given value is larger than that,
		 *             it will be regard as {@link Integer#MAX_VALUE}, because such a rate can't be
		 *             accurately controlled)
		 */
		private void setRatePerMinute(int rate) {
			rate = rate < 1 ? Integer.MAX_VALUE : rate;
			rate = rate > UrlClassifierConf.maxAccRatePM ? Integer.MAX_VALUE : rate;
			milliSecStep = 60_000 / rate;
		}

		/**
		 * get accurate rate per minute, or {@link Integer#MAX_VALUE} if unlimited.<br/>
		 * see {@link #setRatePerMinute(int)}
		 */
		private int getRatePerMinute() {
			return milliSecStep == 0 ? Integer.MAX_VALUE : 60_000 / milliSecStep;
		}

		private void setThreadPoolSize(int size) {
			this.exec.setMaximumPoolSize(size);
		}

		private int getThreadPoolSize() {
			return this.exec.getMaximumPoolSize();
		}

		/**
		 * stop classifier. release all resources, to wait for termination, call {@link #awaitTermination}
		 */
		@SuppressWarnings("unchecked")
		public void stop() {
			hca.close();
			exec.shutdownNow().stream()
							.forEach(r -> {
								Worker work = (Worker) r;
								unhandledTasks.add(new UrlCtxHolder<>(work.url, work.ctx));
							});
		}

		/**
		 * wait for termination.
		 */
		public void awaitTermination(int time, TimeUnit timeUnit) {
			try {
				exec.awaitTermination(time, timeUnit);
			} catch (InterruptedException e) {
				log.debug("interrupted when wait for termination of Classifier: " + this.name);
			}
		}

		/**
		 * crawl give url.
		 */
		private void crawl(String url, CTX ctx) {
			this.crawl(new Worker(url, ctx, this));
		}

		public void crawl(Worker worker) {
			exec.execute(worker);
		}

		/**
		 * last access time, guarded by "this" monitor.
		 */
		@GuardedBy("this")
		private long lastAccess;

		/**
		 * access url, with flow rate control.<br/>
		 * see {@link HttpClientAssist#access(String)}
		 */
		public HttpClientAssist.UrlEntity access(String url) throws IOException, InterruptedException {
			long timeStep = this.milliSecStep;
			if (timeStep > 0) {
				long waitTime;
				synchronized (this) {
					lastAccess += timeStep;
					long now = System.currentTimeMillis();
					lastAccess = Math.max(now, lastAccess);
					waitTime = lastAccess - now;
				}
				if (waitTime > 0) Thread.sleep(waitTime);
			}

			return this.hca.access(url);
		}
	}

	/**
	 * crawler state.
	 */
	public enum Status {
		INIT, RUNNING, PAUSED, STOPPED
	}
}
