
package mysh.algorithm;

import static org.junit.Assert.*;

import org.junit.Test;

public class Math2Test {

	@Test
	public void testFactorial() {

		assertEquals((Math2.factorial(1)), 1);
		assertEquals((Math2.factorial(4)), 24);
		assertEquals((Math2.factorial(12)), 479001600);
	}

	@Test
	public void testCountPermutation() {

		assertEquals(Math2.countPermutation(1, 1), 1);
		assertEquals(Math2.countPermutation(4, 1), 4);
		assertEquals(Math2.countPermutation(7, 2), 42);
		assertEquals(Math2.countPermutation(6, 3), 120);
	}

	@Test
	public void testCountCombination() {

		assertEquals(Math2.countCombination(1, 1), 1);
		assertEquals(Math2.countCombination(4, 1), 4);
		assertEquals(Math2.countCombination(7, 2), 21);
		assertEquals(Math2.countCombination(6, 3), 20);
	}
}
