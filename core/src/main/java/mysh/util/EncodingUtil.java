
package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.lang.Character.UnicodeBlock.*;

/**
 * 编码工具类. <br/>
 *
 * @author Allen
 */
public class EncodingUtil {
	private static final Logger log = LoggerFactory.getLogger(EncodingUtil.class);

	private static final Set<Character.UnicodeBlock> chineseBlocks;

	static {
		chineseBlocks = new HashSet<>();
		chineseBlocks.add(CJK_UNIFIED_IDEOGRAPHS);
		chineseBlocks.add(CJK_COMPATIBILITY_IDEOGRAPHS);
		chineseBlocks.add(CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A);
		chineseBlocks.add(CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B);
		chineseBlocks.add(CJK_SYMBOLS_AND_PUNCTUATION);
		chineseBlocks.add(HALFWIDTH_AND_FULLWIDTH_FORMS);
		chineseBlocks.add(GENERAL_PUNCTUATION);
	}

	public static boolean isChinese(char c) {
		return chineseBlocks.contains(Character.UnicodeBlock.of(c));
	}

	public static boolean hasChinese(String str){
		if (str == null || str.length() == 0) return false;
		int len = str.length();
		for (int i = 0; i < len; i++) {
			if (isChinese(str.charAt(i)))
				return true;
		}
		return false;
	}

	/**
	 * is char a pure japanese(Kana only)
	 */
	public static boolean isPureJapanese(char c) {
		Character.UnicodeBlock block = of(c);
		return block == HIRAGANA || block == KATAKANA;
	}

	/**
	 * whether string has any pure japanese(Kana only)
	 */
	public static boolean hasPureJapanese(String str) {
		if (str == null || str.length() == 0) return false;
		int len = str.length();
		for (int i = 0; i < len; i++) {
			if (isPureJapanese(str.charAt(i)))
				return true;
		}
		return false;
	}

	/**
	 * 判断是否 utf8 编码串. <br/>
	 * 用 utf8 编码规则判断, 较快. 更多信息, 见 <a href=
	 * "http://www.codeguru.com/cpp/misc/misc/multi-lingualsupport/article.php/c10451/The-Basics-of-UTF8.htm"
	 * >The-Basics-of-UTF8</a>
	 *
	 * @param data 字节串
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

	// /**
	// * 取编码格式. <br/>
	// * 用 mozilla univeral detector 检测编码类型.
	// *
	// * @param data
	// * 编码串.
	// * @return
	// */
	// public static String getCharsetByMozillaUniversalDetector(byte[] data) {
	//
	// UniversalDetector detector = new UniversalDetector(null);
	// detector.handleData(data, 0, data.length);
	// detector.dataEnd();
	// return detector.getDetectedCharset();
	// }

	/**
	 * 判断是否 utf8 编码串.<br/>
	 * 用转换后对比的方式, 较慢.
	 *
	 * @param data 字节串
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
	 * @param data 字节串
	 */
	@Deprecated
	public static boolean isUTF8Bytes_ori(byte[] data) {

		int i = 0;
		int size = data.length;

		while (i < size) {
			int step;
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

		return i == size;
	}

	public static byte[] encodeTrans(byte[] source, String sourceEncode, String desEncode) {

		try {
			CharBuffer sourceCharBuf = Charset.forName(sourceEncode).decode(
							ByteBuffer.wrap(source));
			ByteBuffer outputData = Charset.forName(desEncode).encode(sourceCharBuf);

			byte[] result = new byte[outputData.limit()];
			System.arraycopy(outputData.array(), 0, result, 0, result.length);
			return result;
		} catch (Exception e) {
			log.error("转换失败", e);
			throw new RuntimeException("转换失败", e);
		}
	}
}
