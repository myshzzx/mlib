package mysh.util;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * HotKeysGlobalTest
 *
 * @author mysh
 * @since 2015/8/17
 */
public class HotKeysGlobalTest {

	@Test
	public void testAddWin32KeyboardListener() throws Exception {
		HotKeysGlobal.addWin32KeyboardListener((c, a, s, ch, desc) -> {
			System.out.println(c + " " + a + " " + s + " " + desc);
		});

		new CountDownLatch(1).await();
	}
}
