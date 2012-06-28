
package mysh.util;

/**
 * 字节工具.
 * 
 * @author Allen
 * 
 */
public class ByteUtil {

	/**
	 * 在 content 中查找 key 的位置, 找不到返回 -1.<br/>
	 * content == null 或 key == null 或 起始位置越界, 则返回 -1.<br/>
	 * key 长度为 0 时返回 0.
	 * 
	 * @param content
	 * @param startIndex
	 * @param key
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
		int i = startIndex, j;
		ContentIterate: for (; i < limit; i++) {
			for (j = 0; j < keyLength; j++) {
				if (content[i + j] != key[j]) {
					continue ContentIterate;
				}
			}
			return i;
		}

		return -1;
	}
}
