
package mysh.algorithm;

import java.util.ArrayList;

public class Math2 {

	/**
	 * 取 [from, to] 内的质数. 传入参数需大于10.
	 */
	public static int[] genPrime(int from, int to) {

		if (to < from || from < 10)
			throw new IllegalArgumentException();

		// 第 i 个值表示 i+from 是否质数
		boolean[] get = new boolean[to - from + 1];

		for (int i = 0; i < get.length; i++)
			get[i] = true;

		int factorLimit = (int) Math.sqrt(to) + 1;
		for (int factor = 2; factor < factorLimit; factor++) {
			int composite = from / factor;
			if (composite == 0) composite = 1;
			composite *= factor;
			if (composite == from)
				get[0] = false;

			while ((composite += factor) <= to)
				get[composite - from] = false;
		}

		int primeCount = 0;
		for (int i = 0; i < get.length; i++)
			if (get[i])
				primeCount++;
		int[] primes = new int[primeCount];
		for (int i = 0, j = 0; i < get.length; i++)
			if (get[i])
				primes[j++] = from + i;

		return primes;
	}

	/**
	 * 取 [2, limit] 内的质数.
	 */
	public static int[] genPrime(int limit) {
		if (limit < 2) throw new IllegalArgumentException();

		// 第 i 个值表示 i 是否质数
		boolean[] get = new boolean[limit + 1];
		get[0] = get[1] = false;
		for (int i = 0; i < get.length; i++)
			get[i] = true;

		int factorLimit = (int) Math.sqrt(limit) + 1;
		for (int factor = 2; factor < factorLimit; factor++) {
			if (get[factor]) {
				// 至此可确定 factor 一定是质数
				int composite = factor;
				while ((composite += factor) <= limit)
					get[composite] = false;
			}
		}

		int primeCount = 0;
		for (int i = 2; i < get.length; i++)
			if (get[i])
				primeCount++;
		int[] primes = new int[primeCount];
		for (int i = 2, j = 0; i < get.length; i++)
			if (get[i])
				primes[j++] = i;

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
		if (n < 2 || dec < 1) throw new IllegalArgumentException();

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
			if (count > Integer.MAX_VALUE) throw new RuntimeException("calculation overflow");
		}
		while (--t > 0) count /= t + 1;
		return (int) count;
	}

	/**
	 * 辅助类.
	 *
	 * @author ZhangZhx
	 */
	private static final class Arrange {

		private final int[][] result;

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

		public Arrange(int[] a, int start, int end) {

			if (a == null || a.length < end || start < 0 || start >= end)
				throw new IllegalArgumentException();

			this.a = a;
			this.start = start;
			this.end = end;
			this.result = new int[Math2.factorial(end - start)][end - start];

			this.perm(start, end);
		}

		private void perm(int m, int n) {

			if (m == n) {
				System.arraycopy(this.a, this.start, this.result[this.resultIndex++],
								0, this.end - this.start);
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
	 * 部分全排列.
	 *
	 * @param start start index(include). &gt;=0.
	 * @param end   end index.(exclude). &gt;start.
	 * @return
	 */
	public static int[][] arrange(int start, int end) {

		int[] a = new int[end];
		for (int i = 0; i < a.length; i++)
			a[i] = i;
		return new Arrange(a, start, end).get();
	}

	/**
	 * 对给定数组部分全排列.
	 *
	 * @param a
	 * @param start start index(include). &gt;=0.
	 * @param end   end index.(exclude). &gt;start.
	 * @return
	 */
	public static int[][] arrange(int[] a, int start, int end) {

		return new Arrange(a, start, end).get();
	}

	/**
	 * 从 n 个数 ( 1~n ) 中任选 i 个数.
	 */
	public static int[][] combine(int n, int i) {

		if (n < 1 || i < 1 || i > n)
			throw new IllegalArgumentException();

		final class Comb {

			private final int[] a;

			private final int n;

			private final int i;

			private final int[][] result;

			private int resultCount = 0;

			private Comb(int n, int i) {

				this.n = n;
				this.i = i;

				this.a = new int[this.n];
				for (int index = 0; index < this.a.length; index++)
					this.a[index] = index;

				this.result = new int[Math2.countCombination(this.n, this.i)][this.i];

				this.comb(0, new int[this.i], this.i);
			}

			private void comb(int startIndex, int[] tmp, int remain) {

				if (remain == 0) {
					System.arraycopy(tmp, 0, this.result[this.resultCount++], 0,
									this.i);
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

		return new Comb(n, i).get();
	}
}
