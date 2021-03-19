
package mysh.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class BytesTest extends Assertions {

	@Test
	public void findBytesIndexTest() {

		byte[] blankContent = new byte[0], blankKey = new byte[0];
		byte[] content1 = new byte[]{1, 2, 4, 1, 3, 1, 2, 3};
		byte[] content2 = new byte[]{1, 2, 3, 4, 3, 1, 2, 3};
		byte[] content3 = new byte[]{1, 2, 3, 4, 1, 2, 3, 1, 2, 3};
		byte[] content4 = new byte[]{1, 2, 5, 3, 4, 1, 3, 1, 4, 3};
		byte[] key = new byte[]{1, 2, 3};

		assertEquals(-1, Bytes.findBytesIndex(blankContent, 2, blankKey));
		assertEquals(-1, Bytes.findBytesIndex(content1, 2, blankKey));

		assertEquals(5, Bytes.findBytesIndex(content1, 2, key));
		assertEquals(-1, Bytes.findBytesIndex(content2, 6, key));
		assertEquals(4, Bytes.findBytesIndex(content3, 2, key));
		assertEquals(5, Bytes.findBytesIndex(content1, 0, key));
		assertEquals(7, Bytes.findBytesIndex(content3, 6, key));
		assertEquals(-1, Bytes.findBytesIndex(content4, 0, key));
	}

	//	@Test
	public void speedTest() throws IOException {

		// length : 2_000_000
		String content;
		String key = "abcdefghijklmnopqrstuvwxyz";

		try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(
						"C:\\Users\\Allen\\Desktop/a.txt")))) {

			content = r.readLine();
		}

		byte[] contentB = content.getBytes();
		byte[] keyB = key.getBytes();

		assertEquals(contentB.length, content.length());
		assertEquals(keyB.length, key.length());

		System.out.println("content length: " + content.length());

		int testTimes = 3_000;

		boolean f = true;
		long stringTestStart = System.nanoTime();
		for (int i = 0; i < testTimes; i++) {
			f = content.contains(key);
		}
		System.out.println(f + " string.contains: " + (System.nanoTime() - stringTestStart)
						/ 1_000_000);

		int resultIndex = 0;
		long byteTestStart = System.nanoTime();
		for (int i = 0; i < testTimes; i++) {
			// ByteUtil.indexOf(contentB, 0, contentB.length, keyB, 0, keyB.length, 0);
			resultIndex = Bytes.findBytesIndex(contentB, 0, keyB);
		}
		System.out.println(resultIndex + " byte: " + (System.nanoTime() - byteTestStart)
						/ 1_000_000);

	}

	@Test
	public void testFindStringIndexIgnoreCase() throws UnsupportedEncodingException {

		byte[] content1 = "mYsHfaieflemYsH".getBytes("utf8");
		byte[] content2 = "mYasHfaieflemYsH".getBytes("utf8");
		byte[] content3 = "abcdMysHfaieflemYsH".getBytes("utf8");
		byte[] content4 = "jfaoiejfalkjfei".getBytes("utf8");
		byte[] content5 = "mYshfiaef".getBytes("utf8");
		byte[] content6 = "aamYsh伟人fiaef".getBytes("utf8");
		byte[] content7 = "faemYsh伟人fiaef".getBytes("gbk");
		String key = "MySh";
		String key2 = "MySh伟人";

		assertEquals(0, Bytes.findStringIndexIgnoreCase(content1, null, 0, key));
		assertEquals(11, Bytes.findStringIndexIgnoreCase(content1, null, 1, key));
		assertEquals(12, Bytes.findStringIndexIgnoreCase(content2, null, 0, key));
		assertEquals(4, Bytes.findStringIndexIgnoreCase(content3, null, 0, key));
		assertEquals(4, Bytes.findStringIndexIgnoreCase(content3, null, 2, key));
		assertEquals(4, Bytes.findStringIndexIgnoreCase(content3, null, 4, key));
		assertEquals(-1, Bytes.findStringIndexIgnoreCase(content4, null, 0, key));
		assertEquals(-1, Bytes.findStringIndexIgnoreCase(content4, null, 3, key));
		assertEquals(-1, Bytes.findStringIndexIgnoreCase(content5, null, 30, key));
		assertEquals(-1, Bytes.findStringIndexIgnoreCase(content5, null, 3, key));

		assertEquals(2, Bytes.findStringIndexIgnoreCase(content6, null, 0, key2));
		assertEquals(-1, Bytes.findStringIndexIgnoreCase(content6, null, 3, key2));
		assertEquals(-1, Bytes.findStringIndexIgnoreCase(content6, "gbk", 3, key2));
		assertEquals(-1, Bytes.findStringIndexIgnoreCase(content7, null, 3, key2));
		assertEquals(3, Bytes.findStringIndexIgnoreCase(content7, "gbk", 3, key2));
		assertEquals(3, Bytes.findStringIndexIgnoreCase(content7, "gbk", 0, key2));
	}
}
