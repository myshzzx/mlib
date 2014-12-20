
package mysh.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class Math2 {

	/**
	 * 判断 n 是否质数.
	 *
	 * @see <a href='http://en.wikipedia.org/wiki/Primality_test'>Primality test</a>
	 */
	public static boolean isPrime(int n) {
		if (n <= 3) {
			return n > 1;
		} else if (n % 2 == 0 || n % 3 == 0) {
			return false;
		} else {
			int l = (int) (Math.sqrt(n)) + 1;
			for (int i = 5; i < l; i += 6) {
				if (n % i == 0 || n % (i + 2) == 0) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * 判断 n 是否质数.
	 *
	 * @see <a href='http://en.wikipedia.org/wiki/Primality_test'>Primality test</a>
	 */
	public static boolean isPrime(long n) {
		if (n <= 3) {
			return n > 1;
		} else if (n % 2 == 0 || n % 3 == 0) {
			return false;
		} else {
			long l = (long) (Math.sqrt(n)) + 1;
			for (long i = 5; i < l; i += 6) {
				if (n % i == 0 || n % (i + 2) == 0) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * 取 [2, limit] 内的质数.
	 */
	public static int[] genPrime(int limit) {
		if (limit < 2) throw new IllegalArgumentException();

		// 第 i 个值表示 i 是否合数
		boolean[] get = new boolean[limit + 1];
		get[0] = get[1] = true;

		int factorLimit = (int) Math.sqrt(limit) + 1;
		int composite;
		for (int factor = 2; factor < factorLimit; factor++) {
			if (!get[factor]) {
				// 至此可确定 factor 一定是质数
				composite = factor;
				while ((composite += factor) <= limit)
					get[composite] = true;
			}
		}

		int primeCount = 0;
		for (int i = 2; i < get.length; i++)
			if (!get[i]) primeCount++;

		int[] primes = new int[primeCount];
		for (int i = 2, j = 0; i < get.length; i++)
			if (!get[i]) primes[j++] = i;

		return primes;
	}

	/**
	 * 取 [from, to] 内的质数. 传入参数需不小于10.<br/>
	 * 使用并行计算, 若计算时间超过30分钟, 将抛异常.
	 *
	 * @see <a href='http://zh.wikipedia.org/zh/質數定理'>质数定理</a>
	 * @see <a href='http://michaelnielsen.org/polymath1/index.php?title=Bounded_gaps_between_primes#World_records'>质数差上界</a>
	 */
	public static int[] genPrime(final int from, final int to) {

		if (to < from || from < 10)
			throw new IllegalArgumentException();

		// 第 i 个值表示 i+from 是否合数
		boolean[] get = new boolean[to - from + 1];
		int factorLimit = (int) Math.sqrt(to) + 1;

		int parts = Math.min(Runtime.getRuntime().availableProcessors(), factorLimit - 2);
		int step = (factorLimit - 2) / parts + 1;
		ForkJoinPool fjp = ForkJoinPool.commonPool();

		for (int factor = 2; factor < factorLimit; factor += step) {
			int start = factor, end = Math.min(factor + step, factorLimit);
			fjp.execute(() -> {
				for (int f = start; f < end; f++) {
					int composite = from / f;
					if (composite == 0) composite = 1;
					composite *= f;
					if (composite == from) get[0] = true;

					int compositeEnd = to - f;
					while (composite <= compositeEnd) {
						composite += f;
						get[composite - from] = true;
					}
				}
			});
		}
		if (!fjp.awaitQuiescence(30, TimeUnit.MINUTES))
			throw new RuntimeException("genPrime doesn't complete in 30 minutes.");

		int primeCount = 0;
		for (boolean g : get) if (!g) primeCount++;

		int[] primes = new int[primeCount];
		for (int i = 0, j = 0; i < get.length; i++)
			if (!get[i]) primes[j++] = from + i;

		return primes;
	}

	/**
	 * 取 [from, to] 内的质数. 传入参数需不小于10且跨度不超过 {@link Integer#MAX_VALUE}/2.<br/>
	 * 使用并行计算, 若计算时间超过30分钟, 将抛异常.
	 *
	 * @see <a href='http://zh.wikipedia.org/zh/質數定理'>质数定理</a>
	 * @see <a href='http://michaelnielsen.org/polymath1/index.php?title=Bounded_gaps_between_primes#World_records'>质数差上界</a>
	 */
	public static long[] genPrime(final long from, final long to) {

		if (to < from || from < 10 || to - from > Integer.MAX_VALUE / 2)
			throw new IllegalArgumentException();

		// 第 i 个值表示 i+from 是否合数
		boolean[] get = new boolean[(int) (to - from + 1)];
		long factorLimit = (long) Math.sqrt(to) + 1;

		int parts = (int) Math.min(Runtime.getRuntime().availableProcessors(), factorLimit - 2);
		long step = (factorLimit - 2) / parts + 1;
		ForkJoinPool fjp = ForkJoinPool.commonPool();

		for (long factor = 2; factor < factorLimit; factor += step) {
			long start = factor, end = Math.min(factor + step, factorLimit);
			fjp.execute(() -> {
				for (long f = start; f < end; f++) {
					long composite = from / f;
					if (composite == 0) composite = 1;
					composite *= f;
					if (composite == from) get[0] = true;

					long compositeEnd = to - f;
					while (composite <= compositeEnd) {
						composite += f;
						get[((int) (composite - from))] = true;
					}
				}
			});
		}
		if (!fjp.awaitQuiescence(30, TimeUnit.MINUTES))
			throw new RuntimeException("genPrime doesn't complete in 30 minutes.");

		int primeCount = 0;
		for (boolean g : get) if (!g) primeCount++;

		long[] primes = new long[primeCount];
		for (int i = 0, j = 0; i < get.length; i++)
			if (!get[i]) primes[j++] = from + i;

		return primes;
	}

	/**
	 * 求两个数最大公约数.
	 *
	 * @return 传入小于1的数时返回 -1.
	 */
	public static long gcd(long a, long b) {
		if (a < 1 || b < 1) return -1;

		long t;
		while ((t = a % b) > 0) {
			a = b;
			b = t;
		}
		return b;
	}

	/**
	 * 进制转换 n进制 转 十进制.
	 *
	 * @param n  n.
	 * @param nn n进制数.
	 * @return 十进制数.
	 */
	public static long numSysN2Dec(int n, int... nn) {
		if (n < 2 || nn == null) throw new IllegalArgumentException();
		long fac = 1;
		long dec = 0;
		long t;
		for (int i = nn.length - 1; i > -1; i--) {
			if (nn[i] >= n || nn[i] < 0) throw new IllegalArgumentException();
			t = nn[i] * fac;
			if (dec > Long.MAX_VALUE - t) throw new RuntimeException("calculation overflow");
			dec += t;
			fac *= n;
		}

		return dec;
	}

	/**
	 * 进制转换 十进制 转 n进制.
	 *
	 * @param n   n.
	 * @param dec 十进制正数.
	 * @return n进制数.
	 */
	public static int[] numSysDec2N(int n, long dec) {
		if (n < 2 || dec < 1) throw new IllegalArgumentException("n=" + n + ", dec=" + dec);

		ArrayList<Integer> r = new ArrayList<>(10);

		while (dec > 0) {
			r.add((int) (dec % n));
			dec /= n;
		}

		int[] rr = new int[r.size()];
		for (int i = 0; i < rr.length; i++)
			rr[i] = r.get(rr.length - 1 - i);
		return rr;
	}


	/**
	 * 阶乘.
	 */
	public static int factorial(int n) {

		if (n < 1)
			throw new IllegalArgumentException();

		long factorial = n;
		while (--n > 1) {
			factorial *= n;
			if (factorial > Integer.MAX_VALUE) throw new RuntimeException("calculation overflow");
		}

		return (int) factorial;
	}

	/**
	 * 从 n 个元素中取 i 个元素全排列的数目.
	 */
	public static int countArrangement(int n, int i) {

		if (n < 1 || i < 1 || i > n)
			throw new IllegalArgumentException();

		long count = n;
		while (--i > 0) {
			count *= --n;
			if (count > Integer.MAX_VALUE) throw new RuntimeException("calculation overflow");
		}

		return (int) count;
	}

	/**
	 * 从 n 个元素中取 i 个元素的数目.
	 */
	public static int countCombination(int n, int i) {

		if (n < 1 || i < 1 || i > n)
			throw new IllegalArgumentException();

		long count = n;
		int t = 0;
		while (++t < i) {
			count *= n - t;
			if (count > Integer.MAX_VALUE)
				throw new RuntimeException("calculation overflow: n=" + n + ", i=" + i);
		}
		while (--t > 0) count /= t + 1;
		return (int) count;
	}

	/**
	 * 从 n 个元素中取 i 个元素的数目.
	 */
	public static long countCombinationLong(int n, int i) {

		if (n < 1 || i < 1 || i > n)
			throw new IllegalArgumentException();

		long count = n;
		int t = 0;
		while (++t < i) {
			if (Long.MAX_VALUE / (n - t) <= count)
				throw new RuntimeException("calculation overflow: n=" + n + ", i=" + i);
			count *= n - t;
		}
		while (--t > 0) count /= t + 1;
		return count;
	}

	/**
	 * 部分全排列.
	 *
	 * @param start start index(include). &gt;=0.
	 * @param end   end index.(exclude). &gt;start.
	 */
	public static int[][] arrange(int start, int end) {

		int[] a = new int[end];
		for (int i = 0; i < a.length; i++)
			a[i] = i;
		return new Arrange(a, start, end, null).get();
	}

	/**
	 * 对给定数组部分全排列.
	 *
	 * @param start start index(include). &gt;=0.
	 * @param end   end index.(exclude). &gt;start.
	 */
	public static int[][] arrange(int[] a, int start, int end) {

		return new Arrange(a, start, end, null).get();
	}

	/**
	 * 部分全排列.
	 *
	 * @param start start index(include). &gt;=0.
	 * @param end   end index.(exclude). &gt;start.
	 */
	public static void arrange(int start, int end, IntArrayHandler handler) {
		if (handler == null)
			throw new IllegalArgumentException("handler should not be null");

		int[] a = new int[end];
		for (int i = 0; i < a.length; i++)
			a[i] = i;
		new Arrange(a, start, end, handler);
	}

	/**
	 * 对给定数组部分全排列.
	 *
	 * @param start start index(include). &gt;=0.
	 * @param end   end index.(exclude). &gt;start.
	 */
	public static void arrange(int[] a, int start, int end, IntArrayHandler handler) {
		if (handler == null)
			throw new IllegalArgumentException("handler should not be null");

		new Arrange(a, start, end, handler);
	}

	/**
	 * 辅助类.
	 *
	 * @author ZhangZhx
	 */
	private static final class Arrange {

		private final int[][] result;
		private final IntArrayHandler handler;

		private int resultIndex = 0;
		private final int[] a;

		/**
		 * start index(include).
		 */
		private final int start;

		/**
		 * end index(exclude).
		 */
		private final int end;

		public Arrange(int[] a, int start, int end, IntArrayHandler handler) {

			if (a == null || a.length < end || start < 0 || start >= end) {
				throw new IllegalArgumentException("a=" + Arrays.toString(a)
								+ ", start=" + start + ", end=" + end);
			}

			this.a = a;
			this.start = start;
			this.end = end;
			this.handler = handler;
			this.result = handler != null ?
							null : new int[Math2.factorial(end - start)][end - start];

			this.perm(start, end);
		}

		private void perm(int m, int n) {

			if (m == n) {
				if (result != null)
					System.arraycopy(this.a, this.start, this.result[this.resultIndex++],
									0, this.end - this.start);
				else {
					int[] r = new int[this.end - this.start];
					System.arraycopy(this.a, this.start, r, 0, this.end - this.start);
					handler.handle(r);
				}
			} else {
				for (int i = m; i < n; i++) {
					this.swap(m, i);
					this.perm(m + 1, n);
					this.swap(m, i);
				}
			}
		}

		private void swap(int m, int n) {

			int t = this.a[m];
			this.a[m] = this.a[n];
			this.a[n] = t;
		}

		public int[][] get() {

			return this.result;
		}
	}

	/**
	 * 从 n 个数 ( 1~n ) 中任选 i 个数.
	 */
	public static int[][] combine(int n, int i) {

		return new Comb(n, i, null).get();
	}

	/**
	 * 从 n 个数 ( 1~n ) 中任选 i 个数.
	 */
	public static void combine(int n, int i, IntArrayHandler handler) {
		if (handler == null)
			throw new IllegalArgumentException("handler should not be null");

		new Comb(n, i, handler);
	}

	private static class Comb {

		private final int[] a;

		private final int n;

		private final int i;
		private final IntArrayHandler handler;

		private final int[][] result;

		private int resultCount = 0;

		private Comb(int n, int i, IntArrayHandler handler) {

			if (n < 1 || i < 1 || i > n)
				throw new IllegalArgumentException("n=" + n + ", i=" + i);

			this.n = n;
			this.i = i;
			this.handler = handler;

			this.a = new int[this.n];
			for (int index = 0; index < this.a.length; index++)
				this.a[index] = index;

			this.result = handler != null ?
							null : new int[Math2.countCombination(this.n, this.i)][this.i];

			this.comb(0, new int[this.i], this.i);
		}

		private void comb(int startIndex, int[] tmp, int remain) {

			if (remain == 0) {
				if (result != null)
					System.arraycopy(tmp, 0,
									this.result[this.resultCount++], 0,
									this.i);
				else {
					int[] r = new int[this.i];
					System.arraycopy(tmp, 0, r, 0, this.i);
					handler.handle(r);
				}
			} else {
				for (int index = startIndex; index < this.n - remain + 1; index++) {
					tmp[tmp.length - remain] = this.a[index] + 1;
					this.comb(index + 1, tmp, remain - 1);
				}
			}

		}

		private int[][] get() {

			return this.result;
		}
	}

	public static interface IntArrayHandler {
		void handle(int[] a);
	}

	/**
	 * whether given string is number.
	 */
	public static boolean isNumber(String s) {
		if (s == null) return false;
		String ss = s.trim();

		boolean hasE = false;
		boolean canSymbolAppear = true;
		boolean canPointAppear = true;
		boolean valid = false;

		for (int i = 0; i < ss.length(); i++) {
			char c = ss.charAt(i);
			if ((c == '+' || c == '-') && canSymbolAppear) {
				canSymbolAppear = false;
				continue;
			}

			if (!hasE) {
				if ((c >= '0' && c <= '9')) {
					valid = true;
					canSymbolAppear = false;
				} else if (c == '.' && canPointAppear) {
					canPointAppear = false;
					canSymbolAppear = false;
				} else if ((c == 'e' || c == 'E') && valid) {
					hasE = true;
					canSymbolAppear = true;
					valid = false;
				} else
					return false;
			} else {
				if ((c >= '0' && c <= '9')) {
					valid = true;
					canSymbolAppear = false;
				} else
					return false;
			}
		}
		return valid;
	}

}
