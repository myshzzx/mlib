
package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

/**
 * 字节工具.
 *
 * @author Allen
 */
public class Bytes {

	private static final Logger log = LoggerFactory.getLogger(Bytes.class);
	/**
	 * 默认编码.
	 */
	public static final String DefaultEncode = "UTF-8";

	/**
	 * 在 content 中查找 key 的位置, 找不到返回 -1.<br/>
	 * content == null 或 key == null 或 起始位置越界, 则返回 -1.<br/>
	 * key 长度为 0 时返回 -1.<br/>
	 * 此方法源自 String.IndexOf(char[] , int , int , char[] , int , int , int )
	 *
	 * @param content    查找源
	 * @param startIndex 查找源的起始索引位置
	 * @param key        查找目标
	 */
	public static int findBytesIndex(byte[] content, int startIndex, byte[] key) {

		if (startIndex < 0)
			startIndex = 0;

		if (content == null || key == null || key.length == 0 || startIndex > content.length - key.length)
			return -1;

		int limit = content.length - key.length + 1;
		int keyLength = key.length;
		int j;

		byte firstKey = key[0];

		for (int i = startIndex; i < limit; i++) {

			if (content[i] != firstKey) {
				while (++i < limit && content[i] != firstKey)
					;

				if (i == limit)
					return -1;
			}

			for (j = 1; j < keyLength && content[i + j] == key[j]; j++)
				;
			if (j == keyLength)
				return i;

		}

		return -1;
	}

	/**
	 * 在 content 中查找 key 的位置(忽略大小写), 找不到返回 -1.<br/>
	 * <p/>
	 * content == null 或 key == null 或 起始位置越界, 则返回 -1.<br/>
	 * key 长度为 0 时返回 0.<br/>
	 * 此方法源自 String.IndexOf(char[] , int , int , char[] , int , int , int )
	 *
	 * @param content       查找源
	 * @param contentEncode 查找源的编码格式. 若为 null, 则使用默认编码 ByteUtil.DefaultEncode.
	 * @param startIndex    查找源的起始索引位置
	 * @param key           查找目标
	 */
	public static int findStringIndexIgnoreCase(byte[] content, String contentEncode, int startIndex, String key) {

		if (content == null || key == null)
			return -1;

		if (key.length() == 0)
			return 0;

		byte[] keyUpperCase, keyLowerCase;
		try {
			if (contentEncode == null || contentEncode.length() == 0)
				contentEncode = Bytes.DefaultEncode;
			keyUpperCase = key.toUpperCase().getBytes(contentEncode);
			keyLowerCase = key.toLowerCase().getBytes(contentEncode);
		} catch (UnsupportedEncodingException e) {
			log.error("转码到 [" + contentEncode + "] 失败.", e);
			return -1;
		}

		return Bytes.findStringIndexIgnoreCase(content, startIndex, keyUpperCase, keyLowerCase);
	}

	/**
	 * 在 content 中查找关键字的位置(忽略大小写), 找不到返回 -1.<br/>
	 * <p/>
	 * content == null 或 查找关键字为 null 或 起始位置越界, 则返回 -1.<br/>
	 * 查找关键字 长度为 0 时返回 0.<br/>
	 * 此方法源自 String.IndexOf(char[] , int , int , char[] , int , int , int )
	 *
	 * @param content      查找源
	 * @param startIndex   查找源的起始索引位置
	 * @param keyUpperCase 查找关键字的大写 byteArray
	 * @param keyLowerCase 查找关键字的小写 byteArray
	 */
	public static int findStringIndexIgnoreCase(byte[] content, int startIndex, byte[] keyUpperCase,
	                                            byte[] keyLowerCase) {

		if (startIndex < 0)
			startIndex = 0;

		if (content == null || keyUpperCase == null || keyLowerCase == null
				|| keyUpperCase.length != keyLowerCase.length)
			return -1;

		if (keyUpperCase.length == 0)
			return 0;

		if (startIndex > content.length - keyUpperCase.length)
			return -1;

		int limit = content.length - keyUpperCase.length + 1;
		int keyLength = keyUpperCase.length;
		int j;

		byte firstKey1 = keyUpperCase[0];
		byte firstKey2 = keyLowerCase[0];
		byte tempByte;

		for (int i = startIndex; i < limit; i++) {

			if ((tempByte = content[i]) != firstKey1 && tempByte != firstKey2) {
				while (++i < limit && (tempByte = content[i]) != firstKey1 && tempByte != firstKey2)
					;

				if (i == limit)
					return -1;
			}

			for (j = 1; j < keyLength
					&& ((tempByte = content[i + j]) == keyUpperCase[j] || tempByte == keyLowerCase[j]); j++)
				;
			if (j == keyLength)
				return i;

		}

		return -1;
	}
}
