package mysh.util;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

/**
 * Windows Util.
 *
 * @author mysh
 * @since 2015/8/17
 */
public class WinAPI {
	public static final User32 user32 = User32.INSTANCE;
	public static final Kernel32 kernel32 = Kernel32.INSTANCE;

	public static String getForeGroundWindowTitle() {
		WinDef.HWND hwnd = user32.GetForegroundWindow();
		int len = user32.GetWindowTextLength(hwnd);
		char[] title = new char[len];
		user32.GetWindowText(hwnd, title, len + 1);
		return new String(title);
	}
}