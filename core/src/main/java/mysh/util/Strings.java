package mysh.util;

/**
 * @author Mysh
 * @since 2015/1/26 22:04
 */
public class Strings {

	/**
	 * check whether str is null or "".
	 */
	public static boolean isEmpty(String str) {
		return str == null || str.length() == 0;
	}

	/**
	 * check whether str is null or "", or consists of \s.
	 */
	public static boolean isBlank(String str) {
		return str == null || str.trim().length() == 0;
	}
}
