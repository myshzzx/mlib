package mysh.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 无延时缓存.
 * 初始化时加载缓存值, get 取值总是立即返回(除了首次加载完成之前), 由 get 驱动异步更新.
 * 注意: 初始化之后 get 总能取到值, 但这个值有可能很久未更新.
 * 适用场景: 加载成本高, 访问频繁, 一致性要求低的数据.
 *
 * @author zhangzhixian<hzzhangzhixian@corp.netease.com>
 * @since 2016/8/10
 */
public class NoDelayCache<O> {
	private static final Logger log = LoggerFactory.getLogger(NoDelayCache.class);

	/**
	 * 缓存加载器
	 */
	public interface Loader<O> {
		/**
		 * 加载缓存.
		 *
		 * @return 返回 null 则忽略此次加载.
		 */
		Wrapper<O> load() throws Exception;
	}

	/**
	 * 缓存结果包装器
	 */
	public static class Wrapper<O> {
		private O v;

		public Wrapper(O v) {
			this.v = v;
		}
	}

	/**
	 * 异步加载线程池
	 */
	private static final ExecutorService exec = Executors.newCachedThreadPool();
	/**
	 * 随机数
	 */
	private static final Random rnd = new Random();

	/**
	 * 缓存对象
	 */
	private volatile O obj;
	/**
	 * 下次刷新时间
	 */
	private volatile long refreshTimeLimit;

	/**
	 * 缓存名
	 */
	private final String cacheName;
	/**
	 * 刷新间隔(ms)
	 */
	private final long refreshInterval;
	/**
	 * 缓存加载器
	 */
	private final Loader<O> loader;

	/**
	 * 实例化缓存, 并启动首次异步加载.
	 *
	 * @param cacheName   缓存名. 用于调试日志.
	 * @param intervalSec 刷新间隔(秒). 刷新由取值(get)驱动, 若长时间未取值, 下一次取值会取到很早的数据.
	 *                    实际刷新间隔会调整为 ±5% 以内的随机值, 以免多个实例节点同时加载缓存.
	 * @param loader      缓存加载器.
	 */
	public NoDelayCache(String cacheName, long intervalSec, Loader<O> loader) {
		this.cacheName = cacheName;
		Assert.isTrue(intervalSec > 0, "刷新间隔必需为正值");
		Assert.notNull(loader, "缓存加载器不能为空");

		this.refreshInterval = (long) (intervalSec * 1000 * (rnd.nextDouble() / 10 + 0.9));
		this.loader = loader;
		log.info("NoDelayCache-init, name:{}, refreshInterval:{}", cacheName, refreshInterval);

		checkRefresh();
	}

	private final CountDownLatch initLatch = new CountDownLatch(1);

	/**
	 * 取缓存. 等待首次加载. 超时(60秒)抛异常.
	 */
	public O get() {
		return get(60);
	}

	/**
	 * 取缓存. 等待首次加载. 超时抛异常.
	 *
	 * @param waitSec 等待超时(秒)
	 */
	public O get(int waitSec) {
		try {
			if (initLatch.getCount() > 0 && !initLatch.await(waitSec, TimeUnit.SECONDS))
				throw new RuntimeException("等待初始化超时");
		} catch (InterruptedException e) {
			throw new RuntimeException("等待初始化被中断", e);
		}

		checkRefresh();
		return obj;
	}

	/**
	 * 刷新标记
	 */
	private final AtomicBoolean refreshFlag = new AtomicBoolean(false);

	/**
	 * 检查并刷新
	 */
	private void checkRefresh() {
		final long now = System.currentTimeMillis();
		if (now > refreshTimeLimit) { // 刷新时间已到
			if (refreshFlag.compareAndSet(false, true)) { // 避免并发刷新
				exec.execute(new Runnable() {
					@Override
					public void run() {
						try {
							log.info("refresh-cache-begin: {}, task: {}", cacheName, now);

							Wrapper<O> loadWrap = loader.load();
							if (loadWrap == null) {
								log.info("refresh-cache-ignored: {}, task: {}", cacheName, now);
								return;
							} else {
								obj = loadWrap.v;
							}

							if (initLatch.getCount() > 0)
								initLatch.countDown();

							refreshTimeLimit = now + refreshInterval;
							log.info("refresh-cache-done: {}, task: {}", cacheName, now);
						} catch (Throwable t) {
							log.error(String.format("refresh-cache-error: %s, task: %d", cacheName, now), t);
						} finally {
							refreshFlag.set(false);
						}
					}
				});
			}
		}
	}

}
