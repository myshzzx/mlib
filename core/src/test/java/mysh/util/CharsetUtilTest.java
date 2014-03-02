
package mysh.util;

import org.junit.Assert;
import org.junit.Test;

public class CharsetUtilTest {

	@Test
	public void encodeTest() throws Exception {

		String ori = "Mysh 是伟人.";
		byte[] utf8 = ori.getBytes("utf8");
		byte[] gbk = ori.getBytes("gbk");

		byte[] utf8ToGbk = CharsetUtil.encodeTrans(utf8, "utf8", "gbk");
		byte[] gbkToUtf8 = CharsetUtil.encodeTrans(gbk, "gbk", "utf8");

		Assert.assertEquals(ori, new String(utf8ToGbk, "gbk"));
		Assert.assertEquals(ori, new String(gbkToUtf8, "utf8"));
		Assert.assertArrayEquals(utf8, gbkToUtf8);
		Assert.assertArrayEquals(gbk, utf8ToGbk);
	}
}
