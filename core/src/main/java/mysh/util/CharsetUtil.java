
package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * 字符工具类.
 *
 * @author Allen
 */
public class CharsetUtil {

	private static final Logger log = LoggerFactory.getLogger(CharsetUtil.class);

	private CharsetUtil() {

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
