
package mysh.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;

/**
 * 字符工具类.
 * 
 * @author Allen
 * 
 */
public class CharsetUtil {

	private static final Logger log = Logger.getLogger(CharsetUtil.class);

	public static byte[] encodeTrans(byte[] source, String sourceEncode, String desEncode)
			throws Exception {

		try {
			CharBuffer sourceCharBuf = Charset.forName(sourceEncode).decode(
					ByteBuffer.wrap(source));
			ByteBuffer outputData = Charset.forName(desEncode).encode(sourceCharBuf);

			return outputData.array();
		} catch (Exception e) {
			log.error("转换失败", e);
			throw new RuntimeException("转换失败", e);
		}
	}
}
