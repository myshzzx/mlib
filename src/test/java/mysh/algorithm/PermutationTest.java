
package mysh.algorithm;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class PermutationTest {

	@Test
	public void testPermutation() {

		int[][] r = Permutation.permutation(0, 4);
		System.out.println("testPermutation1.1" + Arrays.deepToString(r));

		r = Permutation.permutation(1, 4);
		System.out.println("testPermutation1.2" + Arrays.deepToString(r));

		r = Permutation.permutation(0, 1);
		System.out.println("testPermutation1.3" + Arrays.deepToString(r));
	}

	@Test
	public void testPermutation2() {

		int[][] r = Permutation.permutation(new int[] { 10, 11, 12, 13, 14, 15 }, 1, 4);
		System.out.println("testPermutation2.1" + Arrays.deepToString(r));
	}

	@Test
	public void testCombination() {

		int[][] r = Permutation.combination(5, 2);
		System.out.println("testCombination1.1" + Arrays.deepToString(r));

		r = Permutation.combination(4, 3);
		System.out.println("testCombination1.2" + Arrays.deepToString(r));

		r = Permutation.combination(1, 1);
		System.out.println("testCombination1.3" + Arrays.deepToString(r));
	}

}
