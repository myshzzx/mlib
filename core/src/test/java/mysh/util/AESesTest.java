package mysh.util;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * @author Mysh
 * @since 2014/4/2 15:14
 */
public class AESesTest {
	private static final Logger log = LoggerFactory.getLogger(AESesTest.class);

	@Test
	public void t1() throws Exception {
		String content = "myshzzx";
		char[] pw = "myshpw".toCharArray();
		byte[] salt = "myshsalt".getBytes();

		String encrypt = AESes.encrypt(content.getBytes(), pw, salt);
		log.debug("content[" + content + "], encrypt[" + encrypt + "]");
		byte[] decrypt = AESes.decrypt(encrypt, pw, salt);

		Assert.assertEquals(content, new String(decrypt));
	}

	@Test
	public void t2() throws Exception {
		byte[] key = new byte[16];
		Arrays.fill(key, (byte) 1);

		String algorithm = "AES/CBC/PKCS5Padding";

		byte[] iv = new byte[16];
		byte[] enc = AESes.encrypt("mysh".getBytes(), key, algorithm, iv);
		System.out.println(Arrays.toString(enc));

		byte[] c = AESes.decrypt(enc, key, algorithm, iv);
		System.out.println(new String(c));
	}
}
