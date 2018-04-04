package mysh.util;

import java.security.SecureRandom;
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
	
	/**
	 * cluster version of {@link #timeBasedId()}.
	 *
	 * @param scalePower10 scale exponent (1~5). cluster scale = 10^scalePower10
	 */
	public static long timeBasedDistId(int scalePower10) {
		if (scalePower10 < 1 || scalePower10 > 5) {
			throw new IllegalArgumentException("scalePower10 E [1,5]");
		}
		int mask = (int) Math.pow(10, scalePower10);
		return System.currentTimeMillis() * 1000_000 + (flag % mask) * 1000_000 / mask + inc() % (1000_000 / mask);
	}
}
