package mysh.util;

/**
 * @author Mysh
 * @since 2014/10/12 10:05
 */
public class Asserts {

	/**
	 * if !flag, throws a RuntimeException with expMsg.
	 */
	public static void require(boolean flag, String expMsg) {
		if (!flag)
			throw new RuntimeException(expMsg);
	}

	public static void notNull(Object obj, String msg) {
		if (obj == null) {
			throw new IllegalArgumentException("null arg: " + msg);
		}
	}
}
