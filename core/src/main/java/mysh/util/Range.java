package mysh.util;

/**
 * Range
 *
 * @author mysh
 * @since 2016/1/9
 */
public class Range {
	public static int within(int min, int max, int value) {
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}

	public static long within(long min, long max, long value) {
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}

	public static double within(double min, double max, double value) {
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}

	public static <T extends Comparable<T>> T within(T min, T max, T value) {
		if (value == null) return null;

		if (min != null && value.compareTo(min) < 0) return min;
		if (max != null && max.compareTo(value) < 0) return max;
		return value;
	}
}
