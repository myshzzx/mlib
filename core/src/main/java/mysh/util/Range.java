package mysh.util;

/**
 * Range
 *
 * @author mysh
 * @since 2016/1/9
 */
public class Range {
	
	public static boolean isWithin(int min, int max, int value) {
		return value >= min && value <= max;
	}
	
	public static boolean isWithin(long min, long max, long value) {
		return value >= min && value <= max;
	}
	
	/**
	 * return a value that within [min, max].
	 *
	 * @param min   min value
	 * @param max   max value
	 * @param value given value
	 */
	public static int within(int min, int max, int value) {
		if (min > max) throw new IllegalArgumentException("min should be less then max");
		
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}
	
	/**
	 * return a value that within [min, max].
	 *
	 * @param min   min value
	 * @param max   max value
	 * @param value given value
	 */
	public static long within(long min, long max, long value) {
		if (min > max) throw new IllegalArgumentException("min should be less then max");
		
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}
	
	/**
	 * return a value that within [min, max].
	 *
	 * @param min   min value
	 * @param max   max value
	 * @param value given value
	 */
	public static double within(double min, double max, double value) {
		if (min > max) throw new IllegalArgumentException("min should be less then max");
		
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}
	
	/**
	 * return a value that within [min, max].
	 *
	 * @param min   min value, can be null.
	 * @param max   max value, can be null.
	 * @param value given value, can be null.
	 */
	public static <T extends Comparable<T>> T within(T min, T max, T value) {
		if (value == null) return null;
		if (min != null && max != null && min.compareTo(max) > 0)
			throw new IllegalArgumentException("min should be less then max");
		
		if (min != null && value.compareTo(min) < 0) return min;
		if (max != null && max.compareTo(value) < 0) return max;
		return value;
	}
}
