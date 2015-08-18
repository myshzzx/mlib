package mysh.util;

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
public class HotKeysGlobalTest {
	private static final Logger log = LoggerFactory.getLogger(HotKeysGlobalTest.class);

	@Test
	public void testAddWin32KeyboardListener() throws Exception {
		HotKeysGlobal.addWin32KeyboardListener(new HotKeysGlobal.Win32KbAction() {
			@Override
			public void onKeyDown(boolean c, boolean a, boolean s, int ch, String desc, boolean winChg, String winTitle) {
				if (winChg)
					System.out.println(winTitle);
				log.info(friendlyKeyDesc(c, a, s, ch));
			}
		});

		new CountDownLatch(1).await();
	}

	@Test
	public void bindWin32Key() throws InterruptedException {
		HotKeysGlobal.bindWin32KeyboardListener(true, true, false, KeyEvent.VK_S,
						(c, a, s, vc, vcd, wc, wt) -> {
							System.out.println(wt);
						});

		new CountDownLatch(1).await();
	}
}
