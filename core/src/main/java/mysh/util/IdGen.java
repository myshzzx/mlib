package mysh.util;

import com.google.common.util.concurrent.RateLimiter;
import mysh.net.Nets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Enumeration;
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
	private static final Logger log = LoggerFactory.getLogger(IdGen.class);
	
	private static final SecureRandom secRnd = new SecureRandom();
	private static final AtomicInteger c = new AtomicInteger(secRnd.nextInt() >>> 1);
	
	
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
	private static int flag;
	
	static {
		try {
			Nets.iterateNetworkIF(nif -> {
				Enumeration<InetAddress> ias = nif.getInetAddresses();
				while (ias.hasMoreElements()) {
					InetAddress ia = ias.nextElement();
					if (ia instanceof Inet4Address) {
						try {
							byte[] hwa = nif.getHardwareAddress();
							int hwai = hwa.length - 1;
							flag = hwa[hwai--] & 0xFF;
							flag |= ((hwa[hwai--] << 8) & 0xFF00);
							flag |= ((hwa[hwai--] << 16) & 0xFF0000);
							flag |= ((hwa[hwai] << 24) & 0xFF000000);
						} catch (SocketException e) {
							log.warn("get-netface-hwAddr-fail,nif={}", nif, e);
							continue;
						}
						if (flag != 0) {
							break;
						}
					}
				}
			});
		} catch (SocketException e) {
			log.warn("get-netface-fail", e);
		}
		flag ^= secRnd.nextInt();
		flag = Math.abs(flag % 1000_000);
	}
	
	/**
	 * cluster version of {@link #timeBasedId()}.
	 *
	 * @param scalePower10 scale exponent (1~4). cluster scale = 10^scalePower10
	 */
	public static long timeBasedDistId(int scalePower10) {
		if (scalePower10 < 1 || scalePower10 > 4) {
			throw new IllegalArgumentException("scalePower10 E [1,4]");
		}
		int scale = (int) Math.pow(10, scalePower10 + 1);
		int tail = 1_000_000;
		if (scalePower10 > 1) {
			limiterMap.computeIfAbsent(scalePower10, s -> RateLimiter.create(tail / scale * 30)).acquire();
		}
		return System.currentTimeMillis() * tail
				+ (flag % scale) * tail / scale
				+ inc() % (tail / scale);
	}
}
