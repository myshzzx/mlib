package mysh.crawler2;

import com.google.common.collect.Streams;
import mysh.net.httpclient.HttpClientAssist;
import mysh.net.httpclient.HttpClientConfig;
import mysh.util.Htmls;
import mysh.util.Range;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
	private static final Random rand = ThreadLocalRandom.current();
	private final Logger log;
	
	private final CrawlerSeed<CTX> seed;
	private final String name;
	
	private final AtomicReference<Status> status = new AtomicReference<>(Status.INIT);
	private final UrlClassifierConf.Factory<CTX> ccf;
	
	private final Collection<UrlCtxHolder<CTX>> unhandledTasks = new ConcurrentLinkedQueue<>();
	
	/**
	 * create a crawler with seed , http-client config and maximum access rate per minute.
	 * speed auto adjusting is enabled by default.
	 */
	public Crawler(CrawlerSeed<CTX> seed, @Nullable HttpClientConfig hcc, int ratePerMin) throws Exception {
		this(seed, hcc, ratePerMin, 5);
	}
	
	/**
	 * create a crawler with seed , http-client config and maximum access rate per minute.
	 * speed auto adjusting is enabled by default.
	 */
	public Crawler(CrawlerSeed<CTX> seed, @Nullable HttpClientConfig hcc,
	               int ratePerMin, int threadPoolSize) throws Exception {
		this(seed, hcc, null, ratePerMin, threadPoolSize);
	}
	
	/**
	 * create a crawler with seed , http-client config and maximum access rate per minute.
	 * speed auto adjusting is enabled by default.
	 */
	public Crawler(CrawlerSeed<CTX> seed, @Nullable HttpClientConfig hcc, @Nullable ProxySelector proxySelector,
	               int ratePerMin, int threadPoolSize) throws Exception {
		this(seed, new UrlClassifierConf.Factory<CTX>() {
			UrlClassifierConf ucc = new UrlClassifierConf(
					seed.getClass().getSimpleName() + "-default",
					threadPoolSize,
					Range.within(1, Integer.MAX_VALUE, ratePerMin),
					new HttpClientAssist(hcc, proxySelector)
			).setBlockChecker(new DefaultBlockChecker());
			
			@Override
			public UrlClassifierConf get(String url, CTX ctx) {
				return ucc;
			}
		});
	}
	
	/**
	 * create a crawler with seed and url-classifier factory.
	 * using the factory you can classify urls to groups and customize different strategies for them.
	 */
	public Crawler(CrawlerSeed<CTX> seed, UrlClassifierConf.Factory<CTX> ccf) throws Exception {
		this.seed = Objects.requireNonNull(seed, "need seed");
		this.ccf = Objects.requireNonNull(ccf, "need classifiedUrlCrawler config factory");
		
		this.name = "Crawler(" + this.seed.getClass().getSimpleName() + ")";
		this.log = LoggerFactory.getLogger(this.name);
		
		this.seed.init();
	}
	
	private Thread autoStopChkThread;
	
	/**
	 * start auto-check-stop thread. if there's nothing to crawl, it will stop the crawler.
	 */
	private synchronized void startAutoStopChk() {
		if (autoStopChkThread != null) return;
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				Crawler.this.stop();
			}
		});
		
		autoStopChkThread = new Thread(name + "-autoStopChk") {
			@Override
			public void run() {
				Thread thread = Thread.currentThread();
				while (!thread.isInterrupted()) {
					try {
						Thread.sleep(10000);
						
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
		autoStopChkThread.setDaemon(true);
		autoStopChkThread.start();
	}
	
	/**
	 * start the crawler. a crawler can't be restarted.
	 */
	@SuppressWarnings("unchecked")
	public Crawler<CTX> start() {
		if (!this.status.compareAndSet(Status.INIT, Status.RUNNING)) {
			throw new RuntimeException(toString() + " can't be started, current status=" + status.get());
		}
		
		log.info(this.name + " started.");
		
		seed.getSeeds().forEach(ctxHolder -> classify(ctxHolder.url, ctxHolder.ctx));
		
		if (seed.autoStop())
			startAutoStopChk();
		
		return this;
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
			classifiers.values().forEach(ClassifiedUrlCrawler::stop);
			classifiers.values().forEach(c -> c.awaitTermination(2, TimeUnit.MINUTES));
		} finally {
			try {
				List<UrlCtxHolder<CTX>> tasks = unhandledTasks.stream().distinct().collect(Collectors.toList());
				log.info("{} stopped. unhandledTasks={}", this.name, tasks.size());
				seed.onCrawlerStopped(tasks);
			} finally {
				status.set(Status.STOPPED);
				waitStopLatch.countDown();
				if (autoStopChkThread != null)
					autoStopChkThread.interrupt();
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
	
	static boolean isNetworkIssue(Throwable t) {
		return t instanceof InterruptedIOException
				|| t instanceof SocketException
				|| t instanceof UnknownHostException;
	}
	
	private void storeUnhandledTask(String url, CTX ctx, Throwable t) {
		unhandledTasks.add(new UrlCtxHolder<>(url, ctx));
		log.error("store unhandled task: " + url, t);
	}
	
	private static final Pattern httpExp =
			Pattern.compile("[Hh][Tt][Tt][Pp][Ss]?:[^\"'<>\\s#]+");
	private static final Pattern srcHrefExp =
			Pattern.compile("(([Hh][Rr][Ee][Ff])|([Ss][Rr][Cc]))[\\s]*=[\\s]*[\"']([^\"'#]*)");
	
	private class Worker extends UrlCtxHolder<CTX> implements Runnable {
		private static final long serialVersionUID = 4173253887553156741L;
		private final ClassifiedUrlCrawler classifier;
		
		Worker(String url, CTX ctx, ClassifiedUrlCrawler classifier) {
			super(url, ctx);
			this.classifier = classifier;
		}
		
		@Override
		public void run() {
			try {
				seed.beforeAccess(this);
				if (!seed.accept(this.url, this.ctx))
					return;
				
				while (status.get() == Status.PAUSED) {
					Thread.sleep(50);
				}
				
				try (HttpClientAssist.UrlEntity ue = classifier.access(url)) {
					if (ue == null) {
						// blocked
						classifier.recrawlWhenFail(this, null);
						return;
					}
					
					if (!seed.accept(this.url, this.ctx))
						return;
					if (status.get() == Status.STOPPED) {
						storeUnhandledTask(url, ctx, null);
						return;
					}
					
					log.debug("onGet={}, reqUrl={}", ue.getCurrentURL(), ue.getReqUrl());
					
					if (seed.onGet(ue, this.ctx)) {
						classifier.onGetSuccess(url);
						if (ue.isText() && seed.needToDistillUrls(ue, this.ctx)) {
							seed.afterDistillingUrls(ue, this.ctx, distillUrl(ue, this.ctx))
							    .filter(h -> seed.accept(h.url, h.ctx))
							    .forEach(h -> classify(h.url, h.ctx));
						}
					} else
						classifier.recrawlWhenFail(this, null);
				}
			} catch (InterruptedIOException | SocketException ex) {
				classifier.recrawlWhenFail(this, ex);
			} catch (UnknownHostException | InterruptedException ex) {
				storeUnhandledTask(url, ctx, ex);
			} catch (Exception ex) {
				if (!isMalformedUrl(ex))
					storeUnhandledTask(url, ctx, ex);
				else
					log.error("malformed url will be ignored: " + this.url, ex);
			} finally {
				classifier.afterAccess();
			}
		}
		
		private Stream<String> distillUrl(HttpClientAssist.UrlEntity ue, CTX ctx) throws IOException {
			String pageContent = ue.getEntityStr();
			
			Set<String> urls = new HashSet<>();
			
			try {
				// 1. 查找 http 开头的地址
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
				
				// 2. 查找 href 指向的地址
				Matcher srcHrefMatcher = srcHrefExp.matcher(pageContent);
				Iterator<String> srcHrefUrls = new Iterator<String>() {
					@Override
					public boolean hasNext() {
						return srcHrefMatcher.find();
					}
					
					@Override
					public String next() {
						return srcHrefMatcher.group(4);
					}
				};
				
				// 3. seed 定义的扩展提取地址
				Stream<String> extendedUrls = ObjectUtils.firstNonNull(seed.enhancedDistillUrl(ue, ctx), Stream.empty());
				
				String protocolPrefix = ue.getProtocol() + ":", root = currentRoot;
				
				Streams.concat(Streams.stream(srcHrefUrls), extendedUrls).forEach(value -> {
					String tUrl;
					if (value == null || value.length() == 0 || value.startsWith("#")
							|| value.startsWith("mailto:") || value.startsWith("javascript:")) {
						return;
					} else if (value.startsWith("http:") || value.startsWith("https:")) {
						tUrl = value;
					} else if (value.startsWith("//")) {
						tUrl = protocolPrefix + value;
					} else if (value.startsWith("/")) {
						tUrl = root.substring(0, root.indexOf('/', 9)) + value;
					} else {
						tUrl = root + value;
					}
					urls.add(HttpClientAssist.getShortURL(tUrl));
				});
			} catch (Exception e) {
				log.error("分析页面链接时异常: " + ue.getCurrentURL(), e);
			}
			
			Charset enc = ue.getEntityEncoding();
			return urls.stream()
			           .filter(url -> url.length() > 0)
			           .map(url -> Htmls.urlDecode(url
					           .replace("&amp;", "&")
					           .replace("&lt;", "<")
					           .replace("&gt;", ">")
					           .replace("&quot;", "\""), enc));
		}
		
		private boolean isMalformedUrl(Exception ex) {
			return ex.getCause() instanceof URISyntaxException
					|| ex instanceof MalformedURLException
					|| ex instanceof IllegalArgumentException
					;
		}
	}
	
	private final Map<UrlClassifierConf, ClassifiedUrlCrawler> classifiers = new ConcurrentHashMap<>();
	
	/**
	 * classify the url and put it into working queue.
	 */
	private void classify(String url, CTX ctx) {
		if (this.status.get() == Status.STOPPED)
			throw new RuntimeException("crawler has been stopped");
		
		final UrlClassifierConf ucc = ccf.get(url, ctx);
		if (ucc == null)
			throw new RuntimeException("get null classifiedUrlCrawler config with param: url=" + url + ", ctx=" + ctx);
		
		ClassifiedUrlCrawler classifier = classifiers.get(ucc);
		if (classifier == null) {
			/** synchronized here because: {@link #useUrlClassifierAdjuster} */
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (ucc) {
				classifier = classifiers.computeIfAbsent(ucc, ClassifiedUrlCrawler::new);
			}
		}
		classifier.crawl(url, ctx);
	}
	
	public void useUrlClassifierAdjuster(UrlClassifierConf ucc, boolean useOrNot) {
		// synchronized here because: this method will fail when ucc's prop hasn't been changed, and
		// the classifiedUrlCrawler.initer has read ucc's old prop, but hasn't completed initialization
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (ucc) {
			ucc.useAdjuster = useOrNot;
			ClassifiedUrlCrawler classifiedUrlCrawler = classifiers.get(ucc);
			if (classifiedUrlCrawler != null)
				classifiedUrlCrawler.useAdjuster = useOrNot;
		}
	}
	
	private static final AtomicLong classifierThreadCount = new AtomicLong(0);
	
	/**
	 * crawl a set of urls that were classified.
	 */
	private class ClassifiedUrlCrawler {
		private final String name;
		
		private volatile int milliSecStep;
		private final HttpClientAssist hca;
		
		private volatile boolean useAdjuster;
		private final UrlClassifierAdjuster adjuster = new UrlClassifierAdjuster(this);
		private final UrlClassifierConf.BlockChecker blockChecker;
		
		private final BlockingQueue<Runnable> wq = new LinkedBlockingQueue<>();
		private final ThreadPoolExecutor exec;
		
		@SuppressWarnings("unchecked")
		ClassifiedUrlCrawler(UrlClassifierConf conf) {
			Objects.requireNonNull(conf, "conf should not be null");
			this.name = Objects.requireNonNull(conf.name, "name can't be null");
			
			setRatePerMinute(conf.ratePerMinute);
			this.hca = Objects.requireNonNull(conf.hca, "hca should not be null");
			
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
						storeUnhandledTask(w.url, w.ctx, null);
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
		void setRatePerMinute(int rate) {
			milliSecStep = 60_000 / Range.within(1, UrlClassifierConf.maxAccRatePM + 1, rate);
		}
		
		/**
		 * get accurate rate per minute, or {@link Integer#MAX_VALUE} if unlimited.<br/>
		 * see {@link #setRatePerMinute(int)}
		 */
		int getRatePerMinute() {
			return milliSecStep == 0 ? Integer.MAX_VALUE :
					Range.within(1, UrlClassifierConf.maxAccRatePM, 60_000 / milliSecStep);
		}
		
		void setThreadPoolSize(int size) {
			this.exec.setMaximumPoolSize(size);
		}
		
		int getThreadPoolSize() {
			return this.exec.getMaximumPoolSize();
		}
		
		/**
		 * stop classifiedUrlCrawler. release all resources, to wait for termination, call {@link #awaitTermination}
		 */
		@SuppressWarnings("unchecked")
		void stop() {
			hca.close();
			exec.shutdownNow().stream()
			    .forEach(r -> {
				    Worker work = (Worker) r;
				    storeUnhandledTask(work.url, work.ctx, null);
			    });
		}
		
		/**
		 * wait for termination.
		 */
		void awaitTermination(int time, TimeUnit timeUnit) {
			try {
				exec.awaitTermination(time, timeUnit);
			} catch (InterruptedException e) {
				log.debug("interrupted when wait for termination of Classifier: " + this.name);
			}
		}
		
		/**
		 * crawl give url.
		 */
		void crawl(String url, CTX ctx) {
			exec.execute(new Worker(url, ctx, this));
		}
		
		private final Map<String, AtomicInteger> recrawlCount = new ConcurrentHashMap<>();
		
		/**
		 * schedule a recrawl.
		 *
		 * @return true when scheduled, false when rejected.
		 */
		void recrawlWhenFail(Worker worker, IOException ex) {
			AtomicInteger count = recrawlCount.computeIfAbsent(worker.url, u -> new AtomicInteger(0));
			if (count.getAndIncrement() < 3) {
				if (useAdjuster && ex != null)
					adjuster.onException(ex);
				exec.execute(worker);
				log.debug("recrawl: {}/3, ex={}, url={}", count.get(), ex, worker.url);
			} else {
				storeUnhandledTask(worker.url, worker.ctx, ex);
			}
		}
		
		void onGetSuccess(String url) {
			recrawlCount.remove(url);
		}
		
		/**
		 * last access time, guarded by "this" monitor.
		 */
		@GuardedBy("this")
		private long lastAccess;
		
		/**
		 * access url, with flow rate control.<br/>
		 * see {@link HttpClientAssist#access(String)}
		 *
		 * @return <code>null</code> if this access blocked
		 */
		HttpClientAssist.UrlEntity access(String url) throws IOException, InterruptedException {
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
			if (!Objects.equals(ue.getReqUrl(), ue.getCurrentURL()))
				log.warn("url-jumped: {} -> {}", ue.getReqUrl(), ue.getCurrentURL());
			
			if (blockChecker != null && blockChecker.isBlocked(ue)) {
				if (useAdjuster)
					adjuster.onBlocked(ue);
				log.warn("access-blocked: status={}, req={}, current={}", ue.getStatusCode(), ue.getReqUrl(), ue.getCurrentURL());
				return null;
			} else
				return ue;
		}
		
		void afterAccess() {
			if (useAdjuster)
				adjuster.afterAccess();
		}
	}
	
	/**
	 * adjuster checking interval (milli-second).
	 */
	private static final long adjPeriod = 60_000;
	
	private class UrlClassifierAdjuster {
		private ClassifiedUrlCrawler cuCrawler;
		
		UrlClassifierAdjuster(ClassifiedUrlCrawler cuCrawler) {
			this.cuCrawler = cuCrawler;
		}
		
		private final Queue<Long> accessRec = new ConcurrentLinkedQueue<>();
		
		void beforeAccess() {
			accessRec.add(System.currentTimeMillis());
		}
		
		private volatile long periodStart = System.currentTimeMillis();
		private volatile long periodIdx = 1;
		private final Semaphore analyzeRes = new Semaphore(1);
		private final AtomicLong totalMilliSec = new AtomicLong(0);
		private final AtomicInteger totalCount = new AtomicInteger(0);
		private final AtomicInteger networkIssueCount = new AtomicInteger(0);
		private final AtomicInteger blockCount = new AtomicInteger(0);
		
		void afterAccess() {
			Long accTime = accessRec.poll();
			long now = System.currentTimeMillis();
			if (accTime != null) {
				totalMilliSec.addAndGet(now - accTime);
				totalCount.incrementAndGet();
			}
			
			if (periodStart + adjPeriod * periodIdx < now && analyzeRes.tryAcquire()) {
				try {
					int total = totalCount.get();
					if (total > 0) {
						// clear statistics every 3 periods
						boolean isFinalPeriod = periodIdx == 3;
						
						// such Counts are not guarded, and may lose data here, but it's OK,
						// analyzer needs to be lock free
						long accAveMilli = totalMilliSec.get() / total;
						int networkIssues = networkIssueCount.get();
						int blocks = blockCount.get();
						
						if (isFinalPeriod) {
							periodStart = now;
							periodIdx = 0;
							totalMilliSec.set(0);
							totalCount.set(0);
							networkIssueCount.set(0);
							blockCount.set(0);
						}
						
						boolean isSpeedDown = analyze(now, isFinalPeriod, total, accAveMilli, networkIssues, blocks);
						// clear statistics when speed down
						if (isSpeedDown) {
							totalMilliSec.set(0);
							totalCount.set(0);
							networkIssueCount.set(0);
							blockCount.set(0);
						}
					}
				} finally {
					periodIdx++;
					analyzeRes.release();
				}
			}
		}
		
		void onBlocked(HttpClientAssist.UrlEntity ue) {
			blockCount.incrementAndGet();
		}
		
		void onException(IOException ex) {
			if (isNetworkIssue(ex))
				networkIssueCount.incrementAndGet();
		}
		
		private volatile int lastWorkAccPerMinute;
		private volatile long lastSpeedUp;
		private volatile long lastSpeedDown;
		
		/**
		 * analyze access statistics within {@link mysh.crawler2.Crawler#adjPeriod}.
		 *
		 * @param now           current milli-second time
		 * @param isFinalPeriod all statistics data will be clear in final period
		 * @param total         total access count.
		 * @param accAveMilli   access average time (milli-second).
		 * @param networkIssues network fails count.
		 * @param blocks        blocked by remote server count.
		 * @return speed down or not
		 */
		private boolean analyze(long now, boolean isFinalPeriod,
		                        int total, long accAveMilli, int networkIssues, int blocks) {
			double networkIssueRate = 1.0 * networkIssues / total;
			double blockRate = 1.0 * blocks / (total - networkIssues);
			int accPerMinute = cuCrawler.getRatePerMinute();
			
			accPerMinute = Range.within(5, UrlClassifierConf.maxAccRatePM, accPerMinute);
			if (networkIssueRate > 0.15 || blockRate > 0.05) { // speed down
				lastSpeedDown = now;
				cuCrawler.setRatePerMinute((int) (accPerMinute * 0.66));
				log.info("{} speed down to APM:{}, networkIssues:{}, blocks:{}, total:{}",
						cuCrawler.name, cuCrawler.getRatePerMinute(), networkIssues, blocks, total);
				return true;
				
			} else if (isFinalPeriod && networkIssues == 0 && blocks == 0) { // speed up in final period
				
				if (lastSpeedDown > lastSpeedUp && lastSpeedUp + 3600 * 1000 > now) {
					// latest operation is speed down and latest speed up happened in 3 hours
					// back to last work access rate
					if (accPerMinute < lastWorkAccPerMinute) {
						cuCrawler.setRatePerMinute(Math.max(5, lastWorkAccPerMinute));
						log.info("{} speed back to APM:{}, networkIssues:{}, blocks:{}, total:{}",
								cuCrawler.name, cuCrawler.getRatePerMinute(), networkIssues, blocks, total);
					}
					
				} else {
					lastSpeedUp = now;
					lastWorkAccPerMinute = accPerMinute;
					cuCrawler.setRatePerMinute((int) (accPerMinute * (1.1 + rand.nextDouble() * 0.4)));
					log.info("{} speed up to APM:{}, networkIssues:{}, blocks:{}, total:{}",
							cuCrawler.name, cuCrawler.getRatePerMinute(), networkIssues, blocks, total);
				}
			}
			
			return false;
		}
	}
	
	/**
	 * crawler state.
	 */
	public enum Status {
		INIT, RUNNING, PAUSED, STOPPING, STOPPED
	}
}
