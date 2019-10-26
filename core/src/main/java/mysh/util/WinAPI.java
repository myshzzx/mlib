package mysh.util;

import com.sun.jna.Native;
import com.sun.jna.platform.DesktopWindow;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.win32.*;
import com.sun.jna.win32.W32APIOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

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
		KernelExt INSTANCE = Native.load("kernel32", KernelExt.class, W32APIOptions.DEFAULT_OPTIONS);
		
		boolean SetProcessAffinityMask(HANDLE hProcess, long dwProcessAffinityMask);
		
		/**
		 * https://docs.microsoft.com/en-us/windows/desktop/api/processthreadsapi/nf-processthreadsapi-suspendthread
		 */
		int SuspendThread(HANDLE hThread);
		
		/**
		 * https://docs.microsoft.com/en-us/windows/desktop/api/processthreadsapi/nf-processthreadsapi-resumethread
		 */
		int ResumeThread(HANDLE hThread);
		
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
	
	/**
	 * If the function succeeds, the return value is the thread's previous suspend count; otherwise -1
	 */
	static int suspendThread(int tid) {
		WinNT.HANDLE handle = kernel32.OpenThread(
				WinNT.THREAD_ALL_ACCESS, false, tid);
		int r = kernel32.SuspendThread(handle);
		kernel32.CloseHandle(handle);
		return r;
	}
	
	/**
	 * If the function succeeds, the return value is the thread's previous suspend count.
	 * If the function fails, the return value is (DWORD) -1. To get extended error information, call GetLastError.
	 */
	static int resumeThread(int tid) {
		WinNT.HANDLE handle = kernel32.OpenThread(
				WinNT.THREAD_ALL_ACCESS, false, tid);
		int r = kernel32.ResumeThread(handle);
		kernel32.CloseHandle(handle);
		return r;
	}
	
	static void showOrHideWindow(Predicate<DesktopWindow> filter, boolean show) {
		List<DesktopWindow> allWindows = WindowUtils.getAllWindows(!show);
		allWindows.stream().filter(filter)
		          .forEach(w -> showOrHideHwnd(w.getHWND(), show));
	}
	
	static void showOrHideHwnd(WinDef.HWND hwnd, boolean show) {
		if (hwnd != null)
			user32.ShowWindow(hwnd, show ? User32.SW_SHOW : User32.SW_HIDE);
	}
}
