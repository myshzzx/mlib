package mysh.crawler2;

import mysh.annotation.GuardedBy;
import mysh.annotation.Immutable;
import mysh.annotation.ThreadSafe;
import mysh.net.httpclient.HttpClientAssist;
import mysh.net.httpclient.HttpClientConfig;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
 * @version 3.0
 * @since 2014/9/24 12:50
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
		this(seed,
						(url, ctx) -> new UrlClassifierConf("default", hcc.getMaxConnTotal(), Integer.MAX_VALUE, hcc));
	}

	public Crawler(CrawlerSeed<CTX> seed, UrlClassifierConf.Factory<CTX> ccf) throws Exception {
		this.seed = Objects.requireNonNull(seed, "need seed");
		this.ccf = Objects.requireNonNull(ccf, "need classifier config factory");

		this.name = "Crawler(" + this.seed.getClass().getSimpleName() + ")";
		this.log = LoggerFactory.getLogger(this.name);

		this.seed.init();
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

	private final CountDownLatch waitStopLatch = new CountDownLatch(1);

	/**
	 * stop the crawler, which release all the resources it took. BLOCKED until entire crawler stopped.<br/>
	 * WARNING: can't restart after stopping.
	 */
	@SuppressWarnings("unchecked")
	public void stop() {
		Status oldStatus = status.get();
		if (oldStatus == Status.STOPPED || oldStatus == Status.STOPPING)
			return;

		oldStatus = status.getAndSet(Status.STOPPING);
		if (oldStatus == Status.STOPPING) {
			return;
		} else if (oldStatus == Status.STOPPED) {
			status.set(Status.STOPPED);
			return;
		}

		try {
			classifiers.values().forEach(UrlClassifier::stop);
			classifiers.values().forEach(c -> c.awaitTermination(2, TimeUnit.MINUTES));
		} finally {
			try {
				log.info(this.name + " stopped.");
				seed.onCrawlerStopped(unhandledTasks);
			} finally {
				status.set(Status.STOPPED);
				waitStopLatch.countDown();
			}
		}
	}

	/**
	 * wait for stop. block until stopped or interrupted
	 *
	 * @throws InterruptedException
	 */
	public void waitForStop() throws InterruptedException {
		waitStopLatch.await();
	}


	/**
	 * wait for stop. block until stopped or interrupted
	 *
	 * @throws InterruptedException
	 */
	public boolean waitForStop(long timeout, TimeUnit unit) throws InterruptedException {
		return waitStopLatch.await(timeout, unit);
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
					if (!seed.accept(ue.getCurrentURL(), this.ctx))
						return;
					if (status.get() == Status.STOPPED) {
						unhandledTasks.offer(new UrlCtxHolder<>(url, ctx));
						return;
					}

					log.debug("onGet= " + ue.getCurrentURL() + ", reqUrl= " + ue.getReqUrl());

					if (!seed.onGet(ue, this.ctx))
						classifier.recrawlWhenFail(this, null);

					if (ue.isText() && seed.needToDistillUrls(ue, this.ctx)) {
						seed.afterDistillingUrls(ue, this.ctx, distillUrl(ue))
										.filter(h -> seed.accept(h.url, h.ctx))
										.forEach(h -> classify(h.url, h.ctx));
					}
				}
			} catch (InterruptedIOException | SocketException ex) {
				if (classifier.recrawlWhenFail(this, ex))
					log.debug("recrawl scheduled: " + ex.toString() + " - " + this.url);
				else
					unhandledTasks.offer(new UrlCtxHolder<>(url, ctx));
			} catch (UnknownHostException | InterruptedException ex) {
				unhandledTasks.offer(new UrlCtxHolder<>(url, ctx));
			} catch (Exception ex) {
				if (!isMalformedUrl(ex))
					unhandledTasks.offer(new UrlCtxHolder<>(url, ctx));
				else
					log.error("malformed url will be ignored: " + this.url, ex);
			} finally {
				classifier.afterAccess();
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
				String tValue, tUrl;
				String protocolPrefix = ue.getProtocol().getProtocol().toLowerCase() + ":";
				while (srcValueMatcher.find()) {
					tValue = srcValueMatcher.group(4);
					if (tValue == null || tValue.length() == 0 || tValue.startsWith("#")) {
						continue;
					} else if (tValue.startsWith("http:") || tValue.startsWith("https:")) {
						tUrl = tValue;
					} else if (tValue.startsWith("//")) {
						tUrl = protocolPrefix + tValue;
					} else if (tValue.startsWith("/")) {
						tUrl = currentRoot.substring(0, currentRoot.indexOf('/', 9)) + tValue;
					} else {
						tUrl = currentRoot + tValue;
					}
					urls.add(HttpClientAssist.getShortURL(tUrl));
				}
			} catch (Exception e) {
				log.error("分析页面链接时异常: " + ue.getCurrentURL(), e);
			}

			return urls.stream().map(
							url -> url.replace('\\', '/')
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

		final UrlClassifierConf ucc = ccf.get(url, ctx);
		if (ucc == null)
			throw new RuntimeException("get null classifier config with param: url=" + url + ", ctx=" + ctx);

		UrlClassifier classifier = classifiers.get(ucc);
		if (classifier == null) {
			/** synchronized here because: {@link #useUrlClassifierAdjuster} */
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (ucc) {
				classifier = classifiers.computeIfAbsent(ucc, UrlClassifier::new);
			}
		}
		classifier.crawl(url, ctx);
	}

	public void useUrlClassifierAdjuster(UrlClassifierConf ucc, boolean useOrNot) {
		// synchronized here because: this method will fail when ucc's prop hasn't been changed, and
		// the classifier.initer has read ucc's old prop, but hasn't completed initialization
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (ucc) {
			ucc.useAdjuster = useOrNot;
			UrlClassifier urlClassifier = classifiers.get(ucc);
			if (urlClassifier != null)
				urlClassifier.useAdjuster = useOrNot;
		}
	}

	private static final AtomicLong classifierThreadCount = new AtomicLong(0);

	private class UrlClassifier {
		private final String name;

		private volatile int milliSecStep;
		private final HttpClientAssist hca;

		private volatile boolean useAdjuster;
		private final UrlClassifierAdjuster adjuster = new UrlClassifierAdjuster(this);
		private final UrlClassifierConf.BlockChecker blockChecker;

		private final BlockingQueue<Runnable> wq = new LinkedBlockingQueue<>();
		private final ThreadPoolExecutor exec;

		@SuppressWarnings("unchecked")
		UrlClassifier(UrlClassifierConf conf) {
			Objects.requireNonNull(conf, "conf should not be null");
			this.name = Objects.requireNonNull(conf.name, "name can't be null");

			setRatePerMinute(conf.ratePerMinute);
			Objects.requireNonNull(conf.hcc, "hcc should not be null");
			this.hca = new HttpClientAssist(conf.hcc);

			this.useAdjuster = conf.useAdjuster;
			this.blockChecker = conf.blockChecker;

			exec = new ThreadPoolExecutor(conf.threadPoolSize, conf.threadPoolSize, 15L, TimeUnit.SECONDS, wq,
							r -> {
								Thread t = new Thread(r, this.name + "-UrlClassifier-T-" + classifierThreadCount.incrementAndGet());
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
		public void setRatePerMinute(int rate) {
			rate = rate < 1 ? Integer.MAX_VALUE : rate;
			rate = rate > UrlClassifierConf.maxAccRatePM ? Integer.MAX_VALUE : rate;
			milliSecStep = 60_000 / rate;
		}

		/**
		 * get accurate rate per minute, or {@link Integer#MAX_VALUE} if unlimited.<br/>
		 * see {@link #setRatePerMinute(int)}
		 */
		public int getRatePerMinute() {
			return milliSecStep == 0 ? Integer.MAX_VALUE : 60_000 / milliSecStep;
		}

		public void setThreadPoolSize(int size) {
			this.exec.setMaximumPoolSize(size);
		}

		public int getThreadPoolSize() {
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
		public void crawl(String url, CTX ctx) {
			exec.execute(new Worker(url, ctx, this));
		}

		private final ConcurrentHashMap<String, AtomicInteger> recrawlCount = new ConcurrentHashMap<>();

		/**
		 * schedule a recrawl.
		 *
		 * @return true when scheduled, false when rejected.
		 */
		public boolean recrawlWhenFail(Worker worker, IOException ex) {
			AtomicInteger count = recrawlCount.get(worker.url);
			if (count == null || count.get() < 3) {
				if (count == null)
					recrawlCount.computeIfAbsent(worker.url, u -> new AtomicInteger(1));
				else
					count.incrementAndGet();

				if (useAdjuster && ex != null)
					adjuster.onException(ex);
				exec.execute(worker);
				return true;
			} else
				return false;
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

			if (useAdjuster)
				adjuster.beforeAccess();
			HttpClientAssist.UrlEntity ue = this.hca.access(url);
			if (useAdjuster && blockChecker != null && blockChecker.isBlocked(ue))
				adjuster.onBlocked(ue);
			return ue;
		}

		public void afterAccess() {
			if (useAdjuster)
				adjuster.afterAccess();
		}
	}

	private static final long adjPeriod = 60_000;

	private class UrlClassifierAdjuster {
		private UrlClassifier classifier;
		private volatile long periodStart = System.currentTimeMillis();

		public UrlClassifierAdjuster(UrlClassifier classifier) {
			this.classifier = classifier;
		}

		private final Queue<Long> accessRec = new ConcurrentLinkedQueue<>();

		public void beforeAccess() {
			accessRec.add(System.currentTimeMillis());
		}

		private final Semaphore analyzeRes = new Semaphore(1);
		private final AtomicLong totalMilliSec = new AtomicLong(0);
		private final AtomicInteger totalCount = new AtomicInteger(0);
		private final AtomicInteger networkIssueCount = new AtomicInteger(0);
		private final AtomicInteger blockCount = new AtomicInteger(0);

		public void afterAccess() {
			Long accTime = accessRec.poll();
			long now = System.currentTimeMillis();
			if (accTime != null) {
				totalMilliSec.addAndGet(now - accTime);
				totalCount.incrementAndGet();
			}

			if (periodStart + adjPeriod < now && analyzeRes.tryAcquire()) {
				try {
					long totals = totalCount.get();
					if (totals > 0) {
						periodStart = now;
						// such Counts are not guarded, and may lose data here, but it's OK,
						// analyzer needs to be lock free
						long accAveMilli = totalMilliSec.get() / totals;
						int networkIssues = networkIssueCount.get();
						int blocks = blockCount.get();
						totalMilliSec.set(0);
						totalCount.set(0);
						networkIssueCount.set(0);
						blockCount.set(0);

						analyze(totals, accAveMilli, networkIssues, blocks);
					}
				} finally {
					analyzeRes.release();
				}
			}
		}

		private void analyze(long totals, long accAveMilli, int networkIssues, int blocks) {

		}

		public void onBlocked(HttpClientAssist.UrlEntity ue) {
			blockCount.incrementAndGet();
		}

		private boolean isNetworkIssue(Throwable t) {
			return t instanceof InterruptedIOException
							|| t instanceof SocketException
							|| t instanceof UnknownHostException;
		}

		public void onException(IOException ex) {
			if (isNetworkIssue(ex))
				networkIssueCount.incrementAndGet();
		}
	}

	/**
	 * crawler state.
	 */
	public enum Status {
		INIT, RUNNING, PAUSED, STOPPING, STOPPED
	}
}
