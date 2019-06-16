
package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Character.UnicodeBlock;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static java.lang.Character.UnicodeBlock.*;

/**
 * 编码工具类. <br/>
 *
 * @author Allen
 */
public class Encodings {
	private static final Logger log = LoggerFactory.getLogger(Encodings.class);

	public static final Charset UTF_8 = StandardCharsets.UTF_8;
	public static final Charset GBK = Charset.forName("GBK");

	private static final Set<UnicodeBlock> chineseBlocks;

	static {
		chineseBlocks = new HashSet<>();
		chineseBlocks.add(CJK_UNIFIED_IDEOGRAPHS);
		chineseBlocks.add(CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A);
		chineseBlocks.add(CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B);
		chineseBlocks.add(CJK_SYMBOLS_AND_PUNCTUATION); // 判断中文的。号
		chineseBlocks.add(HALFWIDTH_AND_FULLWIDTH_FORMS); // 判断中文的，号
		chineseBlocks.add(GENERAL_PUNCTUATION); // 判断中文的“号
		chineseBlocks.add(CJK_RADICALS_SUPPLEMENT);
		chineseBlocks.add(CJK_COMPATIBILITY);
		chineseBlocks.add(CJK_COMPATIBILITY_FORMS);
		chineseBlocks.add(CJK_COMPATIBILITY_IDEOGRAPHS);
		chineseBlocks.add(CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT);
	}

	public static boolean isChinese(char c) {
		return chineseBlocks.contains(UnicodeBlock.of(c));
	}
	
	public static boolean isChinese(int c) {
		return chineseBlocks.contains(UnicodeBlock.of(c));
	}

	public static boolean hasChinese(String str) {
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
		UnicodeBlock block = of(c);
		return block == HIRAGANA || block == KATAKANA || block == KATAKANA_PHONETIC_EXTENSIONS;
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
	 * is char a pure korean
	 */
	public static boolean isPureKorean(char c) {
		UnicodeBlock block = of(c);
		return block == HANGUL_SYLLABLES || block == HANGUL_JAMO || block == HANGUL_COMPATIBILITY_JAMO;
	}

	/**
	 * whether string has any pure korean
	 */
	public static boolean hasPureKorean(String str){
		if (str == null || str.length() == 0) return false;
		int len = str.length();
		for (int i = 0; i < len; i++) {
			if (isPureKorean(str.charAt(i)))
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

			final byte di = data[i];
			if ((di & 0x80) == 0x0) { // 0xxxxxxx
				step = 1;
			} else if ((di & 0xe0) == 0xc0) { // 110xxxxx 10xxxxxx
				step = 2;
			} else if ((di & 0xf0) == 0xe0) { // 1110xxxx 10xxxxxx 10xxxxxx
				step = 3;
			} else if ((di & 0xf8) == 0xf0) { // 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
				step = 4;
			} else if ((di & 0xfc) == 0xf8) { // 111110xx 10xxxxxx 10xxxxxx 10xxxxxx
				// 10xxxxxx
				step = 5;
			} else if ((di & 0xfe) == 0xfc) { // 1111110x 10xxxxxx 10xxxxxx 10xxxxxx
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
