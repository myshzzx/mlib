
package mysh.util;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Test;

public class TextEncodeUtilTest {

	@Test
	public void test() throws UnsupportedEncodingException {

		byte[] utf8NoBOM = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 0x6d, 0x79,
				0x73, 0x68, (byte) 0xE4, (byte) 0xBC, (byte) 0x9F, (byte) 0xE4,
				(byte) 0xBA, (byte) 0xBA };
		byte[] utf8 = new byte[] { 0x6d, 0x79, 0x73, 0x68, (byte) 0xE4, (byte) 0xBC,
				(byte) 0x9F, (byte) 0xE4, (byte) 0xBA, (byte) 0xBA };
		byte[] gbk = new byte[] { 0x6d, 0x79, 0x73, 0x68, (byte) 0xCE, (byte) 0xB0,
				(byte) 0xC8, (byte) 0xCB };
		byte[] ascii = new byte[] { 0x6d, 0x79, 0x73, 0x68 };

		System.out.println("utf8NoBOM " + new String(utf8NoBOM, "utf8"));
		Assert.assertTrue(TextEncodeUtil.isUTF8Bytes(utf8NoBOM));

		System.out.println("utf8 " + new String(utf8, "utf8"));
		Assert.assertTrue(TextEncodeUtil.isUTF8Bytes(utf8));

		System.out.println("gbk " + new String(gbk, "gbk"));
		Assert.assertFalse(TextEncodeUtil.isUTF8Bytes(gbk));

		System.out.println("iso-8859-1 " + new String(ascii, "iso-8859-1"));
		Assert.assertTrue(TextEncodeUtil.isUTF8Bytes(ascii));
	}

}
