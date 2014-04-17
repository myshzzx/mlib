package mysh.util;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mysh
 * @since 2014/4/2 15:14
 */
public class AESTest {
	private static final Logger log = LoggerFactory.getLogger(AESTest.class);

	@Test
	public void t1() throws Exception {
		String content = "myshzzx";
		char[] pw = "myshpw".toCharArray();
		byte[] salt = "myshsalt".getBytes();

		String encrypt = AESUtil.encrypt(content.getBytes(), pw, salt);
		log.debug("content[" + content + "], encrypt[" + encrypt + "]");
		byte[] decrypt = AESUtil.decrypt(encrypt, pw, salt);

		Assert.assertEquals(content, new String(decrypt));
	}
}
