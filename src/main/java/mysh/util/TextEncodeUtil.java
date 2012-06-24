
package mysh.util;

import java.util.Arrays;

/**
 * 编码工具类. <br/>
 * 
 * @author Allen
 * 
 */
public class TextEncodeUtil {

	/**
	 * 判断是否 utf8 编码串. <br/>
	 * 用 utf8 编码规则判断, 较快. 更多信息, 见 <a href=
	 * "http://www.codeguru.com/cpp/misc/misc/multi-lingualsupport/article.php/c10451/The-Basics-of-UTF8.htm"
	 * >The-Basics-of-UTF8</a>
	 * 
	 * @param data
	 *               字节串
	 * @return
	 */
	public static boolean isUTF8Bytes(byte[] data) {

		int i = 0, ti;
		int size = data.length;

		while (i < size) {
			int step;

			if ((data[i] & 0x80) == 0x0) { // 0xxxxxxx
				step = 1;
			} else if ((data[i] & 0xe0) == 0xc0) { // 110xxxxx 10xxxxxx
				step = 2;
			} else if ((data[i] & 0xf0) == 0xe0) { // 1110xxxx 10xxxxxx 10xxxxxx
				step = 3;
			} else if ((data[i] & 0xf8) == 0xf0) { // 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
				step = 4;
			} else if ((data[i] & 0xfc) == 0xf8) { // 111110xx 10xxxxxx 10xxxxxx 10xxxxxx
									// 10xxxxxx
				step = 5;
			} else if ((data[i] & 0xfe) == 0xfc) { // 1111110x 10xxxxxx 10xxxxxx 10xxxxxx
									// 10xxxxxx 10xxxxxx
				step = 6;
			} else {
				return false;
			}

			if (i + step > size)
				return false;
			for (ti = 1; ti < step; ti++) {
				if ((data[i + ti] & 0xc0) != 0x80)
					return false;
			}

			i += step;
		}

		return true;
	}

	/**
	 * 判断是否 utf8 编码串.<br/>
	 * 用转换后对比的方式, 较慢.
	 * 
	 * @param data
	 *               字节串
	 * @return
	 */
	public static boolean isUTF8Bytes2(byte[] data) {

		try {
			String utf8 = new String(data, "utf8");
			return Arrays.equals(data, utf8.getBytes());
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 判断是否 utf8 编码串. <br/>
	 * 用 utf8 编码规则判断, 较快. 更多信息, 见 <a href=
	 * "http://www.codeguru.com/cpp/misc/misc/multi-lingualsupport/article.php/c10451/The-Basics-of-UTF8.htm"
	 * >The-Basics-of-UTF8</a>
	 * 
	 * @param data
	 *               字节串
	 * @return
	 */
	@Deprecated
	public static boolean isUTF8Bytes_ori(byte[] data) {

		int i = 0;
		int size = data.length;

		while (i < size) {
			int step = 0;
			if ((data[i] & 0x80) == 0x0) { // 0xxxxxxx
				step = 1;
			} else if ((data[i] & 0xe0) == 0xc0) { // 110xxxxx 10xxxxxx
				if (i + 2 > size)
					return false;
				if ((data[i + 1] & 0xc0) != 0x80)
					return false;

				step = 2;
			} else if ((data[i] & 0xf0) == 0xe0) { // 1110xxxx 10xxxxxx 10xxxxxx
				if (i + 3 > size)
					return false;
				if ((data[i + 1] & 0xc0) != 0x80)
					return false;
				if ((data[i + 2] & 0xc0) != 0x80)
					return false;

				step = 3;
			} else {
				return false;
			}

			i += step;
		}

		if (i == size)
			return true;

		return false;
	}

}
