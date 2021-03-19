package mysh.util;

import mysh.ui.UIs;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.swing.*;

/**
 * UIsTest
 *
 * @author mysh
 * @since 2016/4/9
 */
@Disabled
public class UIsTest {
	@Test
	public void infoMsg() throws Exception {
		JDialog d = UIs.infoMsg(null, 300, 200, "Monitor Center is closing...");
		Thread.sleep(3000);
		d.dispose();
	}
}
