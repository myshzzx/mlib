package mysh.util;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

/**
 * Windows Util.
 *
 * @author mysh
 * @since 2015/8/17
 */
public interface WinAPI {
	User32 user32 = User32.INSTANCE;
	Kernel32 kernel32 = Kernel32.INSTANCE;

	static String getForeGroundWindowTitle() {
		WinDef.HWND hwnd = user32.GetForegroundWindow();
		int len = user32.GetWindowTextLength(hwnd) + 1;
		char[] title = new char[len];
		user32.GetWindowText(hwnd, title, len);
		return Native.toString(title);
	}
}
