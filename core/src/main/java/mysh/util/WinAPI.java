package mysh.util;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import com.sun.jna.win32.W32APIOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Windows Util.
 *
 * @author mysh
 * @since 2015/8/17
 */
public interface WinAPI {
	Logger log = LoggerFactory.getLogger(WinAPI.class);

	User32 user32 = User32.INSTANCE;
	KernelExt kernel32 = KernelExt.INSTANCE;

	interface KernelExt extends Kernel32 {
		KernelExt INSTANCE = Native.loadLibrary("kernel32", KernelExt.class, W32APIOptions.DEFAULT_OPTIONS);

		boolean SetProcessAffinityMask(HANDLE hProcess, long dwProcessAffinityMask);
	}

	static String getForeGroundWindowTitle() {
		WinDef.HWND hwnd = user32.GetForegroundWindow();
		int len = user32.GetWindowTextLength(hwnd) + 1;
		char[] title = new char[len];
		user32.GetWindowText(hwnd, title, len);
		return Native.toString(title);
	}

	/**
	 * @param mask exp : 0b11101
	 */
	static void setProcessAffinityMask(int pid, long mask) {
		log.info("set-process-affinity-mask,pid={},mask={}", pid, mask);
		WinNT.HANDLE processHandle = kernel32.OpenProcess(WinNT.PROCESS_ALL_ACCESS, false, pid);
		if (processHandle == null)
			throw new RuntimeException("process not found. pid=" + pid);

		try {
			kernel32.SetProcessAffinityMask(processHandle, mask);
			chkLastError();
		} finally {
			kernel32.CloseHandle(processHandle);
		}
	}

	/**
	 * https://msdn.microsoft.com/en-us/library/windows/desktop/ms681381(v=vs.85).aspx
	 */
	static void chkLastError() {
		String msg = Kernel32Util.getLastErrorMessage();
		if (!Objects.equals("The operation completed successfully.", msg))
			throw new RuntimeException(msg);
	}

}
