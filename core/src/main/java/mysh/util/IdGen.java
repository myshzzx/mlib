package mysh.util;

import mysh.net.Nets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Enumeration;
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
	 * 够用200年的单机id生成
	 * 5 seconds test
	 * conflict/total, rate
	 * result: 0/4098201, 0.000000
	 */
	public static long timeBasedId() {
		return System.currentTimeMillis() * 1000_000 + inc() % 1000_000;
	}
	
	private static int flag;
	
	static {
		try {
			Nets.iterateNetworkIF(nif -> {
				Enumeration<InetAddress> ias = nif.getInetAddresses();
				while (ias.hasMoreElements()) {
					InetAddress ia = ias.nextElement();
					if (ia instanceof Inet4Address) {
						try {
							if (!nif.isUp() || nif.isLoopback() || nif.isVirtual() || nif.isPointToPoint()) {
								return;
							}
							byte[] hwa = nif.getHardwareAddress();
							if (hwa == null) {
								continue;
							}
							int hwai = hwa.length - 1;
							flag = hwa[hwai--] & 0xFF;
							flag |= ((hwa[hwai--] << 8) & 0xFF00);
							flag |= ((hwa[hwai] << 16) & 0xFF0000);
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
	}
	
	/**
	 * 够用30年的基本无冲突的分布式id生成 (有限速,总体递增式)
	 * cluster version of {@link #timeBasedId()}.
	 *
	 * @param scalePower10 scale exponent (1~5). cluster scale = 10^scalePower10
	 */
	public static long increasedDistId(int scalePower10) {
		if (scalePower10 < 1 || scalePower10 > 5) {
			throw new IllegalArgumentException("scalePower10 E [1,5]");
		}
		int scale = (int) Math.pow(10, scalePower10);
		long tail = 10_000_000;
		return (System.currentTimeMillis() - 1600000000000L) * tail
				+ (flag % scale) * tail / scale
				+ inc() % (tail / scale);
	}
	
	/**
	 * 够用 n 天的滚动 hash 式 id 生成 (18位)
	 * 超过 n 天可能出现冲突
	 * 会出现时间位数变化的天数上限: 11天(9位)/115天(10位)/1157天(11位)/11574天(12位)
	 */
	public static long hashedDistId(int scrollDays, int scalePower10) {
		if (scalePower10 < 1 || scalePower10 > 5) {
			throw new IllegalArgumentException("scalePower10 E [1,5]");
		}
		int scale = (int) Math.pow(10, scalePower10);
		long tail = (long) Math.pow(10, 18 - scalePower10);
		int timeBits = (int) Math.ceil(Math.log10(scrollDays * 24 * 3600_000L));
		int incMod = (int) Math.pow(10, 18 - scalePower10 - timeBits);
		return (flag % scale) * tail
				+ (System.currentTimeMillis() % (long) Math.pow(10, timeBits)) * incMod
				+ inc() % incMod;
	}
}
