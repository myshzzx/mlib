
package mysh.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

public class EncodingUtilTest {

	byte[] utf8NoBOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 0x6d, 0x79, 0x73,
					0x68, (byte) 0xE4, (byte) 0xBC, (byte) 0x9F, (byte) 0xE4, (byte) 0xBA,
					(byte) 0xBA};

	byte[] utf8 = new byte[]{0x6d, 0x79, 0x73, 0x68, (byte) 0xE4, (byte) 0xBC, (byte) 0x9F,
					(byte) 0xE4, (byte) 0xBA, (byte) 0xBA};

	byte[] gbk = new byte[]{0x6d, 0x79, 0x73, 0x68, (byte) 0xCE, (byte) 0xB0, (byte) 0xC8,
					(byte) 0xCB};

	byte[] ascii = new byte[]{0x6d, 0x79, 0x73, 0x68};

	@Test
	public void test() throws UnsupportedEncodingException {

		System.out.println("utf8NoBOM " + new String(utf8NoBOM, "utf8"));
		Assert.assertTrue(EncodingUtil.isUTF8Bytes(utf8NoBOM));

		System.out.println("utf8 " + new String(utf8, "utf8"));
		Assert.assertTrue(EncodingUtil.isUTF8Bytes(utf8));

		System.out.println("gbk " + new String(gbk, "gbk"));
		Assert.assertFalse(EncodingUtil.isUTF8Bytes(gbk));

		System.out.println("iso-8859-1 " + new String(ascii, "iso-8859-1"));
		Assert.assertTrue(EncodingUtil.isUTF8Bytes(ascii));
	}

	//	@Test
//	public void testMozilla() throws Exception {
//
//		System.out.println(EncodingUtil.getCharsetByMozillaUniversalDetector(utf8NoBOM));
//		System.out.println(EncodingUtil.getCharsetByMozillaUniversalDetector(utf8));
//		System.out.println(EncodingUtil.getCharsetByMozillaUniversalDetector(gbk));
//		System.out.println(EncodingUtil.getCharsetByMozillaUniversalDetector(ascii));
//
//		byte[] readFileToByteArray = FileUtil.readFileToByteArray(
//				"C:\\Users\\Allen\\Desktop\\a.txt", Integer.MAX_VALUE);
//		String charsetByMozillaUniversalDetector = EncodingUtil.getCharsetByMozillaUniversalDetector(readFileToByteArray);
//		System.out.println(charsetByMozillaUniversalDetector);
//		System.out.println(new String(readFileToByteArray, charsetByMozillaUniversalDetector));
//
//	}

	@Test
	public void encodeTest() throws Exception {

		String ori = "Mysh 是伟人.";
		byte[] utf8 = ori.getBytes("utf8");
		byte[] gbk = ori.getBytes("gbk");

		byte[] utf8ToGbk = EncodingUtil.encodeTrans(utf8, "utf8", "gbk");
		byte[] gbkToUtf8 = EncodingUtil.encodeTrans(gbk, "gbk", "utf8");

		Assert.assertEquals(ori, new String(utf8ToGbk, "gbk"));
		Assert.assertEquals(ori, new String(gbkToUtf8, "utf8"));
		Assert.assertArrayEquals(utf8, gbkToUtf8);
		Assert.assertArrayEquals(gbk, utf8ToGbk);
	}
}
