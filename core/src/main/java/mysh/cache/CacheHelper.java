package mysh.cache;

import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import mysh.util.Compresses;
import mysh.util.Serializer;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * 缓存使用辅助.
 * 可控制缓存冷启动时对底层的穿透, 并支持数据压缩.
 * 缓存服务需要支持加分布式锁.
 *
 * @author <pre>凯泓(zhixian.zzx@alibaba-inc.com)</pre>
 * @since 2020-10-15
 */
@Accessors(fluent = true)
@Slf4j
@NotThreadSafe
public class CacheHelper {
	@Accessors(fluent = true)
	public static class CacheResult {
		boolean success;
		Object value;
		long modifySec;
	}
	
	interface Cache {
		CacheResult get(Serializable key);
		
		CacheResult put(Serializable key, Serializable value, final int putExpireSec);
		
		CacheResult tryLock(Serializable key, int lockExpireSec);
		
		CacheResult invalid(Serializable key);
	}
	
	private Cache cache;
	private Serializable key;
	
	/**
	 * 启用压缩
	 */
	private boolean useCompress;
	
	/**
	 * 过期时间(s), 相对时间或绝对时间都行.
	 */
	private int putExpireSec;
	
	/**
	 * 值加载时是否加锁. 默认不加锁, 但对于有突发流量的场景, 建议加锁并开启重试, 避免突发流量时无缓存对底层的压力
	 */
	private boolean lockWhenLoadValue;
	/**
	 * 记录最近一次成功加锁时间, 用户请无视此字段.
	 */
	private long lastLockTime;
	/**
	 * 锁超时时间(s), 默认5s
	 */
	private int lockExpireSec = 5;
	/**
	 * 加锁异常时当作加锁成功. 默认true
	 */
	private boolean lockSuccessOnExp = true;
	private static final String LOCKKEY_PREFIX = "__chLock.";
	
	private Callable<?> valueGetter;
	
	/**
	 * 读超时(s). 若tair数据未失效, 但读超时, 将触发一次值加载, 若未加载成功(获取锁失败), 返回超时的tair数据.
	 */
	private int readTimeoutSec;
	/**
	 * 整个取值流程中取值失败后的重试次数. 默认0(不重试)
	 */
	private int retryTimes;
	/**
	 * 重试间隔(默认50ms), 第i次重试前等待时间为 i * retryIntervalMs.
	 */
	private int retryIntervalMs = 50;
	
	private void validate() {
		if (putExpireSec > 0 && readTimeoutSec > putExpireSec) {
			throw new IllegalArgumentException(
					"readTimeoutSec higher than putExpireSec: readTimeoutSec=" + readTimeoutSec + ", putExpireSec=" + putExpireSec);
		}
	}
	
	/**
	 * 读取流程:
	 * <pre>
	 *   1. 读缓存
	 *      1.1 读到数据且没有读超时直接返回 value
	 *      1.2 有读超时则继续往下
	 *   2. 有自定义加载器
	 *      2.1 有锁时取锁成功则加载
	 *      2.2 无锁时直接加载
	 *      2.3 加载成功时写缓存, 返回数据
	 *   3. 重试检查
	 *      3.1 若从 tair 加载成功, 则直接返回 value
	 *      3.2 重试次数未达到时回到1, 否则直接返回 value
	 *   4. 返回的 value 可能是读超时的数据, 也可能是null
	 * </pre>
	 *
	 * @param <T>
	 * @return
	 * @throws Exception
	 */
	public <T> T get() throws Exception {
		this.validate();
		Object value = null;
		boolean readSuccessFromTair = false;
		int tryTime = 0;
		while (true) {
			try {
				CacheResult r = cache.get(key);
				if (r != null && r.success) {
					value = r.value;
					if (useCompress) {
						value = Serializer.BUILD_IN.deserialize(Compresses.decompressZip((byte[]) value));
					}
					readSuccessFromTair = true;
					
					// 若没有读超时直接返回结果
					if (readTimeoutSec <= 0 || System.currentTimeMillis() / 1000 - r.modifySec <= readTimeoutSec) {
						return (T) value;
					}
				} else {
					log.error("cache-get-fail, key={}, result={}", key, r);
				}
			} catch (Throwable t) {
				log.error("cache-get-exp, key={}", key, t);
			}
			
			if (valueGetter != null) {
				boolean loadSuccess = false;
				if (lockWhenLoadValue) {
					// 全局锁
					if (tryLock()) {
						try {
							value = valueGetter.call();
							loadSuccess = true;
						} finally {
							releaseLock();
						}
					}
				} else {
					// 无锁
					value = valueGetter.call();
					loadSuccess = true;
				}
				
				if (loadSuccess) {
					put((Serializable) value);
					return (T) value;
				}
			}
			
			if (!readSuccessFromTair && ++tryTime <= retryTimes) {
				Thread.sleep(tryTime * retryIntervalMs);
			} else {
				return (T) value;
			}
		}
	}
	
	/**
	 * 加锁. 用法示例
	 * <pre>
	 * if(tryLock()){
	 *     try {
	 *         ...
	 *     } finally {
	 *         releaseLock();
	 *     }
	 * }
	 * </pre>
	 *
	 * @return 加锁是否成功
	 */
	public boolean tryLock() {
		String lockKey = LOCKKEY_PREFIX + key;
		try {
			CacheResult r = cache.tryLock(lockKey, lockExpireSec);
			boolean lockSuccess = r != null && r.success;
			if (lockSuccess) {
				lastLockTime = System.currentTimeMillis();
			}
			log.info("tryLock,key={},success={}", lockKey, lockSuccess);
			return lockSuccess;
		} catch (Throwable t) {
			log.error("tryLock-exp,key={},result={}", lockKey, lockSuccessOnExp, t);
			return lockSuccessOnExp;
		}
	}
	
	public void releaseLock() {
		String lockKey = LOCKKEY_PREFIX + key;
		if (lastLockTime + lockExpireSec * 1000L > System.currentTimeMillis()) {
			// 未超时才释放锁
			try {
				CacheResult r = cache.invalid(lockKey);
				log.info("releaseLock,key={},result={}", lockKey, r);
			} catch (Throwable t) {
				log.error("releaseLock-exp, key={}", lockKey, t);
			}
		}
	}
	
	public void put(Serializable value) {
		try {
			CacheResult wr;
			if (useCompress) {
				wr = cache.put(key,
						Compresses.compressZip(Serializer.BUILD_IN.serialize(value)), putExpireSec);
			} else {
				wr = cache.put(key, value, putExpireSec);
			}
			if (wr == null || !wr.success) {
				log.error("cache-put-fail, key={}, result={}", key, wr);
			} else {
				log.info("cache-put,key={}", key);
			}
		} catch (Throwable t) {
			log.error("cache-put-exp, key={}", key, t);
		}
	}
}
