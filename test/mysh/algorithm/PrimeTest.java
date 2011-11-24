
package mysh.algorithm;

import org.junit.Test;

public class PrimeTest {

	@Test
	public void test() {

		int from = 100000000, to = 100000100;

		long start = System.nanoTime();
		int[] primes1 = Prime.genPrime(from, to);
		System.out.println("primes1: " + (System.nanoTime() - start) / 1000);

		for (int i : primes1) {
			System.out.print(i + " ");
		}

		System.out.println();

		start = System.nanoTime();
		int[] primes2 = Prime.genPrime(to);
		System.out.println("primes2: " + (System.nanoTime() - start) / 1000);

		for (int i = primes2.length - primes1.length; i < primes2.length; i++) {
			System.out.print(primes2[i] + " ");
		}
	}

}
