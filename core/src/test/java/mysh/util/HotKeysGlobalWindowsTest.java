package mysh.util;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.KeyEvent;
import java.util.concurrent.CountDownLatch;

/**
 * HotKeysGlobalTest
 *
 * @author mysh
 * @since 2015/8/17
 */
@Ignore
public class HotKeysGlobalWindowsTest {
	private static final Logger log = LoggerFactory.getLogger(HotKeysGlobalWindowsTest.class);

	@Test
	public void testAddWin32KeyboardListener() throws Exception {
		HotKeysGlobalWindows.addWin32KeyboardListener(
						keyDown -> {
							if (keyDown.isWindowChanges())
								System.out.println(keyDown.getWinTitle());
							log.info(keyDown.toString());
						});

		new CountDownLatch(1).await();
	}

	@Test
	public void bindWin32Key() throws InterruptedException {
		HotKeysGlobalWindows.bindWin32KeyboardListener(true, true, false, KeyEvent.VK_S,
				System.out::println);

		new CountDownLatch(1).await();
	}
}
