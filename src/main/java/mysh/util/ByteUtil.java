
package mysh.util;

import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;

/**
 * 字节工具.
 * 
 * @author Allen
 * 
 */
public class ByteUtil {

	private static final Logger log = Logger.getLogger(ByteUtil.class);

	/**
	 * 在 content 中查找 key 的位置, 找不到返回 -1.<br/>
	 * content == null 或 key == null 或 起始位置越界, 则返回 -1.<br/>
	 * key 长度为 0 时返回 -1.<br/>
	 * 此方法源自 String.IndexOf(char[] , int , int , char[] , int , int , int )
	 * 
	 * @param content
	 *               查找源
	 * @param startIndex
	 *               查找源的起始索引位置
	 * @param key
	 *               查找目标
	 * @return
	 */
	public static int findBytesIndex(byte[] content, int startIndex, byte[] key) {

		if (startIndex < 0)
			startIndex = 0;

		if (content == null || key == null || key.length == 0
				|| startIndex > content.length - key.length)
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
	 * 
	 * content == null 或 key == null 或 起始位置越界, 则返回 -1.<br/>
	 * key 长度为 0 时返回 -1.<br/>
	 * 此方法源自 String.IndexOf(char[] , int , int , char[] , int , int , int )
	 * 
	 * @param content
	 *               查找源
	 * @param contentEncode
	 *               查找源的编码格式. 若为 null, 则认为是 UTF8.
	 * @param startIndex
	 *               查找源的起始索引位置
	 * @param key
	 *               查找目标
	 * 
	 */
	public static int findStringIndexIgnoreCase(byte[] content, String contentEncode,
			int startIndex, String key) {

		if (startIndex < 0)
			startIndex = 0;

		if (content == null || key == null || key.length() == 0)
			return -1;

		byte[] key1, key2;
		try {
			if (contentEncode == null || contentEncode.length() == 0)
				contentEncode = "UTF-8";
			key1 = key.toUpperCase().getBytes(contentEncode);
			key2 = key.toLowerCase().getBytes(contentEncode);
		} catch (UnsupportedEncodingException e) {
			log.error("转码到 [" + contentEncode + "] 失败.", e);
			return -1;
		}

		if (startIndex > content.length - key1.length)
			return -1;

		int limit = content.length - key1.length + 1;
		int keyLength = key1.length;
		int j;

		byte firstKey1 = key1[0];
		byte firstKey2 = key2[0];
		byte tempByte;

		for (int i = startIndex; i < limit; i++) {

			if ((tempByte = content[i]) != firstKey1 && tempByte != firstKey2) {
				while (++i < limit && (tempByte = content[i]) != firstKey1
						&& tempByte != firstKey2)
					;

				if (i == limit)
					return -1;
			}

			for (j = 1; j < keyLength
					&& ((tempByte = content[i + j]) == key1[j] || tempByte == key2[j]); j++)
				;
			if (j == keyLength)
				return i;

		}

		return -1;

	}
}
