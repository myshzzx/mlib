
package mysh.util;

import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static mysh.util.EncodingUtil.*;
import static org.junit.Assert.*;

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
		assertTrue(isUTF8Bytes(utf8NoBOM));

		System.out.println("utf8 " + new String(utf8, "utf8"));
		assertTrue(isUTF8Bytes(utf8));

		System.out.println("gbk " + new String(gbk, "gbk"));
		assertFalse(isUTF8Bytes(gbk));

		System.out.println("iso-8859-1 " + new String(ascii, "iso-8859-1"));
		assertTrue(isUTF8Bytes(ascii));
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

		byte[] utf8ToGbk = encodeTrans(utf8, "utf8", "gbk");
		byte[] gbkToUtf8 = encodeTrans(gbk, "gbk", "utf8");

		assertEquals(ori, new String(utf8ToGbk, "gbk"));
		assertEquals(ori, new String(gbkToUtf8, "utf8"));
		assertArrayEquals(utf8, gbkToUtf8);
		assertArrayEquals(gbk, utf8ToGbk);
	}


	@Test
	public void testisPureJapanese() throws Exception {
		assertTrue(isPureJapanese('マ'));
		assertTrue(isPureJapanese('輪'));
		assertTrue(isPureJapanese('場'));
		assertFalse(isPureJapanese('>'));
	}

	@Test
	public void testHasJapanese() throws Exception {
		assertTrue(hasPureJapanese("ラドクリフ、マラソン五輪代表に1万m出場にも含み"));
		assertFalse(hasPureJapanese("pron. 两个中的哪一个"));
	}

	@Test
	public void testIsChinese() throws Exception {
		assertTrue(isChinese('輪'));
		assertTrue(isChinese('两'));

		assertFalse(isChinese('5'));
		assertFalse(isChinese('a'));
		assertFalse(isChinese('.'));
		assertFalse(isChinese('>'));
		assertFalse(isChinese('フ'));
	}
}
