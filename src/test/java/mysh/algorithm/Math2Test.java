
package mysh.algorithm;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class Math2Test {

	private void checkPrime(int from, int to, int[] ps) {
		int i = from;
		for (int p : ps) {
			while (i < p)
				assertFalse(Math2.isPrime(i++));

			assertTrue(Math2.isPrime(i++));
		}

		while (i <= to)
			assertFalse(Math2.isPrime(i++));
	}

	@Test
	public void isPrime() {
		assertTrue(Math2.isPrime(2));
		assertTrue(Math2.isPrime(3));
		assertTrue(Math2.isPrime(5));
		assertTrue(Math2.isPrime(7));
		assertTrue(Math2.isPrime(11));
		assertTrue(Math2.isPrime(13));
		assertTrue(Math2.isPrime(17));
		assertTrue(Math2.isPrime(19));
		assertTrue(Math2.isPrime(23));
		assertTrue(Math2.isPrime(100_000_081));

		assertFalse(Math2.isPrime(-3));
		assertFalse(Math2.isPrime(-2));
		assertFalse(Math2.isPrime(-1));
		assertFalse(Math2.isPrime(0));
		assertFalse(Math2.isPrime(1));
		assertFalse(Math2.isPrime(4));
		assertFalse(Math2.isPrime(6));
		assertFalse(Math2.isPrime(8));
		assertFalse(Math2.isPrime(9));
		assertFalse(Math2.isPrime(121));
		assertFalse(Math2.isPrime(169));
		assertFalse(Math2.isPrime(221));
	}


	@Test
	public void genPrime() {

		int from, to;

		from = 100_000_000;
		to = 100_000_100;
		checkPrime(from, to, Math2.genPrime(from, to));

		from = 10;
		to = 1_000_000;
		checkPrime(from, to, Math2.genPrime(from, to));
	}

	@Test
	public void genPrime2() {
		int limit = 1_000_000;
		checkPrime(0, limit, Math2.genPrime(limit));
	}

	@Test
	public void gcd() {
		assertEquals(3, Math2.gcd(12, 15));
		assertEquals(3, Math2.gcd(27, 15));
	}

	@Test
	public void numSysN2Dec() {
		assertEquals(254, Math2.numSysN2Dec(16, new int[]{15, 14}));
		assertEquals(5807, Math2.numSysN2Dec(16, new int[]{1, 6, 10, 15}));
	}

	@Test
	public void numSysDec2N() {
		assertArrayEquals(new int[]{1, 15}, Math2.numSysDec2N(16, 31));
		assertArrayEquals(new int[]{1, 6, 10, 15}, Math2.numSysDec2N(16, 5807));
	}

	@Test
	public void factorial() {

		assertEquals((Math2.factorial(1)), 1);
		assertEquals((Math2.factorial(4)), 24);
		assertEquals((Math2.factorial(12)), 479001600);
	}

	@Test
	public void countPermutation() {

		assertEquals(Math2.countArrangement(1, 1), 1);
		assertEquals(Math2.countArrangement(4, 1), 4);
		assertEquals(Math2.countArrangement(7, 2), 42);
		assertEquals(Math2.countArrangement(6, 3), 120);
	}

	@Test
	public void countCombination() {

		assertEquals(Math2.countCombination(1, 1), 1);
		assertEquals(Math2.countCombination(4, 1), 4);
		assertEquals(Math2.countCombination(7, 2), 21);
		assertEquals(Math2.countCombination(6, 3), 20);
	}

	@Test
	public void permutation() {

		int[][] r = Math2.arrange(0, 4);
		System.out.println("testPermutation1.1" + Arrays.deepToString(r));

		r = Math2.arrange(1, 4);
		System.out.println("testPermutation1.2" + Arrays.deepToString(r));

		r = Math2.arrange(0, 1);
		System.out.println("testPermutation1.3" + Arrays.deepToString(r));
	}

	@Test
	public void permutation2() {

		int[][] r = Math2.arrange(new int[]{10, 11, 12, 13, 14, 15}, 1, 4);
		System.out.println("testPermutation2.1" + Arrays.deepToString(r));
	}

	@Test
	public void combination() {

		int[][] r = Math2.combine(5, 2);
		System.out.println("testCombination1.1" + Arrays.deepToString(r));

		r = Math2.combine(4, 3);
		System.out.println("testCombination1.2" + Arrays.deepToString(r));

		r = Math2.combine(1, 1);
		System.out.println("testCombination1.3" + Arrays.deepToString(r));
	}

}
