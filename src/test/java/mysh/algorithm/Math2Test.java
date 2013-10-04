
package mysh.algorithm;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class Math2Test {

	@Test
	public void testFactorial() {

		assertEquals((Math2.factorial(1)), 1);
		assertEquals((Math2.factorial(4)), 24);
		assertEquals((Math2.factorial(12)), 479001600);
	}

	@Test
	public void testCountPermutation() {

		assertEquals(Math2.countArrangement(1, 1), 1);
		assertEquals(Math2.countArrangement(4, 1), 4);
		assertEquals(Math2.countArrangement(7, 2), 42);
		assertEquals(Math2.countArrangement(6, 3), 120);
	}

	@Test
	public void testCountCombination() {

		assertEquals(Math2.countCombination(1, 1), 1);
		assertEquals(Math2.countCombination(4, 1), 4);
		assertEquals(Math2.countCombination(7, 2), 21);
		assertEquals(Math2.countCombination(6, 3), 20);
	}

	@Test
	public void testPermutation() {

		int[][] r = Math2.arrange(0, 4);
		System.out.println("testPermutation1.1" + Arrays.deepToString(r));

		r = Math2.arrange(1, 4);
		System.out.println("testPermutation1.2" + Arrays.deepToString(r));

		r = Math2.arrange(0, 1);
		System.out.println("testPermutation1.3" + Arrays.deepToString(r));
	}

	@Test
	public void testPermutation2() {

		int[][] r = Math2.arrange(new int[]{10, 11, 12, 13, 14, 15}, 1, 4);
		System.out.println("testPermutation2.1" + Arrays.deepToString(r));
	}

	@Test
	public void testCombination() {

		int[][] r = Math2.combine(5, 2);
		System.out.println("testCombination1.1" + Arrays.deepToString(r));

		r = Math2.combine(4, 3);
		System.out.println("testCombination1.2" + Arrays.deepToString(r));

		r = Math2.combine(1, 1);
		System.out.println("testCombination1.3" + Arrays.deepToString(r));
	}

}
