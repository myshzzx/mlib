
package mysh.algorithm;

public class Math2 {

	/**
	 * 阶乘.
	 *
	 * @param n
	 * @return
	 */
	public static int factorial(int n) {

		if (n < 1)
			throw new IllegalArgumentException();

		int factorial = n;
		while (--n > 1)
			factorial *= n;

		return factorial;
	}

	/**
	 * 从 n 个元素中取 i 个元素全排列的数目.
	 *
	 * @param n
	 * @param i
	 * @return
	 */
	public static int countArrangement(int n, int i) {

		if (n < 1 || i < 1 || i > n)
			throw new IllegalArgumentException();

		long count = n;
		while (--i > 0) {
			count *= --n;
			if (count > Integer.MAX_VALUE) throw new RuntimeException("count overflow");
		}

		return (int) count;
	}

	/**
	 * 从 n 个元素中取 i 个元素的数目.
	 *
	 * @param n
	 * @param i
	 * @return
	 */
	public static int countCombination(int n, int i) {

		if (n < 1 || i < 1 || i > n)
			throw new IllegalArgumentException();

		long count = n;
		int t = 0;
		while (++t < i) {
			count *= n - t;
			if (count > Integer.MAX_VALUE) throw new RuntimeException("count overflow");
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
