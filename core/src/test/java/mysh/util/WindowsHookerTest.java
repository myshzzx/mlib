package mysh.util;

import mysh.os.WinAPI;
import mysh.os.WindowsHooker;
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
public class WindowsHookerTest {
	private static final Logger log = LoggerFactory.getLogger(WindowsHookerTest.class);
	
	@Test
	public void testAddWin32KeyboardListener() throws Exception {
		WindowsHooker.KbAction kbAction = keyDown -> {
			log.info(keyDown.toString());
		};
		WindowsHooker.addWin32KeyboardListener(kbAction);
		
		// Times.sleepNoExp(10000);
		// WindowsHooker.removeWin32KeyboardListener(kbAction);
		
		new CountDownLatch(1).await();
	}
	
	@Test
	public void bindWin32Key() throws InterruptedException {
		WindowsHooker.bindWin32KeyboardListener(true, true, false, KeyEvent.VK_S,
				System.out::println);
		
		new CountDownLatch(1).await();
	}
	
	@Test
	public void testWindowEvent() throws InterruptedException {
		WindowsHooker.addWindowListener(hwnd -> {
			log.info("title:{}, proc:{}", WinAPI.getWindowTitle(hwnd), WinAPI.getWindowProcess(hwnd));
		});
		
		new CountDownLatch(1).await();
	}
}
