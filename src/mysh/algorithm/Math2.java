
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
	public static int countPermutation(int n, int i) {

		if (n < 1 || i < 1 || i > n)
			throw new IllegalArgumentException();

		int count = n;
		while (--i > 0)
			count *= --n;

		return count;
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

		return countPermutation(n, i) / factorial(i);
	}
}
