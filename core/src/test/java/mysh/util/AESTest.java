package mysh.util;

import org.junit.Test;

/**
 * @author Mysh
 * @since 2014/4/2 15:14
 */
public class AESTest {
	@Test
	public void t1() throws Exception {
		byte[] decrypt = AESUtil.decrypt("VfFU0+cwofZXq2Do47emim43xVOUZ4SOE1Euof/lmnY=", "weblogic".toCharArray(),
						"weblogic".getBytes());
		System.out.println(new String(decrypt));
	}
}
