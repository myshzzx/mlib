package mysh.dev.codegen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mysh
 * @since 2014/4/17 16:11
 */
public class CodeUtil {
	private static final Logger log = LoggerFactory.getLogger(CodeUtil.class);

	private static final int alphaStep = 'a' - 'A';

	/**
	 * 下划线命名转为驼峰命名.
	 */
	public static String underline2hump(String underline) {
		StringBuilder hump = new StringBuilder();
		String[] words = underline.toLowerCase().split("_");
		for (String word : words) {
			hump.append(toUpperCase(word.charAt(0)));
			hump.append(word.substring(1));
		}
		return hump.toString();
	}

	/**
	 * 驼峰命名转为下划线命名.
	 */
	public static String hump2underline(String hump) {
		StringBuilder underline = new StringBuilder();
		char[] chars = hump.toCharArray();

		for (int i = 0; i < chars.length; i++) {
			if (i == 0 || i == chars.length - 1)
				underline.append(toUpperCase(chars[i]));
			else {
				if (!isUpperCase(chars[i]) && isUpperCase(chars[i + 1])) {
					// test: StringBuilder -> STRING_BUILDER
					underline.append(toUpperCase(chars[i]));
					underline.append('_');
				} else if (isUpperCase(chars[i - 1]) && isUpperCase(chars[i]) && !isUpperCase(chars[i + 1])) {
					// test: MYSHZzx -> MYSH_ZZX
					underline.append('_');
					underline.append(toUpperCase(chars[i]));
				} else {
					underline.append(toUpperCase(chars[i]));
				}
			}
		}

		return underline.toString();
	}

	public static boolean isUpperCase(char c) {
		return c <= 'Z' && c >= 'A';
	}

	public static boolean isLowerCase(char c) {
		return c <= 'z' && c >= 'a';
	}

	public static char toUpperCase(char c) {
		return isLowerCase(c) ? (char) (c - alphaStep) : c;
	}

}
