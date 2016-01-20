package mysh.util;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ID generator.
 *
 * @author mysh
 * @since 2016/1/19
 */
public class IdGen {
	private static final Random rnd = new Random();

	/**
	 * time based id generator.
	 */
	public static long timeBasedId() {
		return timeBasedIdAtomic();
	}

	private static final AtomicInteger c = new AtomicInteger();

	/**
	 * 5 seconds test
	 * conflict/total, rate
	 * 8/10699664, 0.000001
	 * 13/10994199, 0.000001
	 * 4/10887793, 0.000000
	 */
	private static long timeBasedIdAtomic() {
		return (System.currentTimeMillis() * 100 + c.incrementAndGet() % 100) * 10_000 + System.nanoTime() % 10_000;
	}

	/**
	 * 5 seconds test
	 * conflict/total, rate
	 * 19821/4603695, 0.004305
	 * 20705/4606272, 0.004495
	 * 26743/6320166, 0.004231
	 */
	private static long timeBasedIdRnd() {
		return System.currentTimeMillis() * 1000_000 + rnd.nextInt(1000_000);
	}

	/**
	 * 5 seconds test
	 * conflict/total, rate
	 * 8266105/11649367, 0.709577
	 * 8125621/11480971, 0.707750
	 * 8255322/11677617, 0.706940
	 */
	private static long timeBasedIdNano() {
		return System.currentTimeMillis() * 1000_000 + System.nanoTime() % 1000_000;
	}


}
