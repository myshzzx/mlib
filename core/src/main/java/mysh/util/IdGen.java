package mysh.util;

import com.google.common.util.concurrent.RateLimiter;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IdGen.
 * time based, usable from 1970 to 2262.
 * 1522831285440......
 *
 * @since 2018/04/04
 */
public class IdGen {
	private static final SecureRandom secRnd = new SecureRandom();
	private static final long flag = Math.abs(secRnd.nextInt() % 1000_000);
	private static final AtomicInteger c;
	
	static {
		c = new AtomicInteger(secRnd.nextInt() >>> 1);
	}
	
	private static int inc() {
		int inc = c.incrementAndGet();
		if (inc < 0) {
			synchronized (c) {
				if (c.get() < 0) {
					c.set(0);
				}
				return c.incrementAndGet();
			}
		} else {
			return inc;
		}
	}
	
	/**
	 * 5 seconds test
	 * conflict/total, rate
	 * result: 0/4098201, 0.000000
	 */
	public static long timeBasedId() {
		return System.currentTimeMillis() * 1000_000 + inc() % 1000_000;
	}
	
	private static final Map<Integer, RateLimiter> limiterMap = new ConcurrentHashMap<>();
	
	/**
	 * cluster version of {@link #timeBasedId()}.
	 *
	 * @param scalePower10 scale exponent (1~4). cluster scale = 10^scalePower10
	 */
	public static long timeBasedDistId(int scalePower10) {
		if (scalePower10 < 1 || scalePower10 > 4) {
			throw new IllegalArgumentException("scalePower10 E [1,4]");
		}
		int scale = (int) Math.pow(10, scalePower10);
		int tail = 1_000_000;
		if (scalePower10 > 1) {
			limiterMap.computeIfAbsent(scalePower10, s -> RateLimiter.create(tail / scale * 50)).acquire();
		}
		return System.currentTimeMillis() * tail + (flag % scale) * tail / scale + inc() % (tail / scale);
	}
}
