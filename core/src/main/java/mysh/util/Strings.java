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
	 * opposite to {@link #isEmpty(String)}
	 */
	public static boolean isNotEmpty(String str) {
		return !isEmpty(str);
	}

	/**
	 * check whether str is null or "", or consists of \s.
	 */
	public static boolean isBlank(String s) {
		if (s == null) {
			return true;
		}
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isWhitespace(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * opposite to {@link #isBlank(String)}
	 */
	public static boolean isNotBlank(String str) {
		return !isBlank(str);
	}
}
