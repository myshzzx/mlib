
package mysh.algorithm;

/**
 * 质数.
 *
 * @author ZhangZhx
 */
public class Prime {

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

		int factorLimit = (int) Math.pow(to, 0.5) + 1;
		for (int factor = 2; factor < factorLimit; factor++) {
			int composite = from / factor;
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

		int factorLimit = (int) Math.pow(limit, 0.5) + 1;
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
}
