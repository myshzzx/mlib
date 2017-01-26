package mysh.util;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rands
 *
 * @author mysh
 * @since 2017/1/2
 */
public class Rands {
	public static final String a2z = "qwertyuiopasdfghjklzxcvbnm";
	public static final String A2Z = "QWERTYUIOPASDFGHJKLZXCVBNM";
	public static final String num = "0123456789";
	public static final String letterNum = a2z + A2Z + num;

	private static Random rnd = ThreadLocalRandom.current();

	public static final String randStr(String src, int len) {
		return randStr(rnd, src, len);
	}

	public static final String randStr(Random r, String src, int len) {
		if (r == null || Strings.isBlank(src) || len < 0)
			throw new IllegalArgumentException(String.format("r=%s,src=%s,len=%d", r, src, len));

		char[] c = new char[len];
		for (int i = 0; i < len; i++) {
			c[i] = src.charAt(r.nextInt(len));
		}
		return new String(c);
	}
}
