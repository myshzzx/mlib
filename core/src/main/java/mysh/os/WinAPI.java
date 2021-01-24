package mysh.os;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sun.jna.Native;
import com.sun.jna.platform.DesktopWindow;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
	
	class ProcessInfo implements Serializable {
		private static final long serialVersionUID = -2891053091206604968L;
		
		private static final DateTimeFormatter CREATION_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		private static Cache<Long, String> cmdLineCache = CacheBuilder.newBuilder()
		                                                              .maximumSize(1000)
		                                                              .expireAfterWrite(10, TimeUnit.HOURS)
		                                                              .build();
		@Getter
		private int pid, parentPid, priority;
		@Getter
		private String name, exePath, cmdLine;
		@Getter
		private LocalDateTime startTime;
		@Getter
		private int threadCount, handleCount;
		@Getter
		private long userModeMicroSec, kernelModeMicroSec;
		@Getter
		private long virtualSize, workingSetSize, peakVirtualSize, peakWorkingSetSize;
		
		private ProcessInfo() {
		}
		
		private ProcessInfo(Map<String, String> m) {
			name = m.get("Name");
			pid = Integer.parseInt(m.get("ProcessId"));
			parentPid = Integer.parseInt(m.get("ParentProcessId"));
			
			startTime = LocalDateTime.parse(m.get("CreationDate").substring(0, 14), CREATION_DATE_FMT);
			exePath = m.get("ExecutablePath");
			cmdLine = m.get("CommandLine");
			priority = Integer.parseInt(m.get("Priority"));
			threadCount = Integer.parseInt(m.get("ThreadCount"));
			handleCount = Integer.parseInt(m.get("HandleCount"));
			userModeMicroSec = Long.parseLong(m.get("UserModeTime")) / 10;
			kernelModeMicroSec = Long.parseLong(m.get("KernelModeTime")) / 10;
			virtualSize = Long.parseLong(m.get("VirtualSize"));
			workingSetSize = Long.parseLong(m.get("WorkingSetSize"));
			peakVirtualSize = Long.parseLong(m.get("PeakVirtualSize"));
			peakWorkingSetSize = Long.parseLong(m.get("PeakWorkingSetSize"));
		}
		
		@Override
		public String toString() {
			return "ProcessInfoWin32{" +
					"pid=" + pid +
					", parentPid=" + parentPid +
					", name='" + name + '\'' +
					", exePath='" + exePath + '\'' +
					'}';
		}
		
		/**
		 * 取命令行(非常耗时), 并缓存
		 */
		public String getCmdLine() {
			if (cmdLine != null)
				return cmdLine;
			
			try {
				return cmdLine = cmdLineCache.get(
						((long) pid << 34) | startTime.toEpochSecond(ZoneOffset.UTC),
						() -> {
							byte[] infoBytes = Oss.readFromProcess("wmic process where processid=" + pid + " get CommandLine");
							String info = new String(infoBytes, Oss.getOsCharset());
							return info.startsWith("CommandLine") ? info.substring(11).trim() : "";
						});
			} catch (ExecutionException e) {
				log.error("get cmd line error. " + this, e);
				return null;
			}
		}
	}
	
	/**
	 * list all windows processes. it's a heavy operation.
	 *
	 * @param fetchCmdLine fetch cmd line or not. it's an expensive op, fetch them only if you need to iterate all processes' cmd lines.
	 */
	static List<ProcessInfo> getAllWinProcesses(boolean fetchCmdLine) throws Exception {
		if (Oss.getOS() != Oss.OS.Windows) {
			return Collections.emptyList();
		}
		Charset winCharset = Oss.getOsCharset();
		String wmicGetProcs = "wmic process get Name,CreationDate,ExecutablePath,ParentProcessId,Priority,ProcessId,ThreadCount,HandleCount,UserModeTime,KernelModeTime,VirtualSize,WorkingSetSize,PeakVirtualSize,PeakWorkingSetSize";
		if (fetchCmdLine)
			wmicGetProcs += ",CommandLine";
		Process p = Oss.executeCmd(wmicGetProcs, false, false);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), winCharset))) {
			String header = reader.readLine();
			String[] cols = header.split("\\s+");
			int[] colsIdx = new int[cols.length];
			for (int i = 1; i < cols.length; i++) {
				colsIdx[i] = header.indexOf(cols[i], colsIdx[i - 1]);
			}
			
			List<ProcessInfo> processes = new ArrayList<>();
			Map<String, String> infoMap = new HashMap<>();
			reader.lines()
			      .filter(line -> line.length() > 0)
			      .forEach(line -> {
				      byte[] lineBytes = line.getBytes(winCharset);
				      for (int i = 0; i < cols.length; i++) {
					      String key = cols[i];
					      String value;
					      if (i < cols.length - 1)
						      value = new String(lineBytes, colsIdx[i], colsIdx[i + 1] - colsIdx[i], winCharset);
					      else
						      value = new String(lineBytes, colsIdx[i], lineBytes.length - colsIdx[i], winCharset);
					      infoMap.put(key, value.trim());
				      }
				      processes.add(new ProcessInfo(infoMap));
			      });
			return processes;
		}
	}
	
	static WinDef.HWND getForeGroundWindow() {
		return user32.GetForegroundWindow();
	}
	
	static String getWindowTitle(WinDef.HWND hwnd) {
		int len = user32.GetWindowTextLength(hwnd) + 1;
		char[] title = new char[len];
		user32.GetWindowText(hwnd, title, len);
		return Native.toString(title);
	}
	
	static ProcessInfo getWindowProcess(WinDef.HWND hwnd) {
		IntByReference pidRef = new IntByReference();
		user32.GetWindowThreadProcessId(hwnd, pidRef);
		int pid = pidRef.getValue();
		WinNT.HANDLE proc = kernel32.OpenProcess(
				WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ, false, pid);
		char[] name = new char[1024];
		int len = Psapi.INSTANCE.GetModuleFileNameExW(proc, null, name, name.length);
		String path = new String(name, 0, len);
		kernel32.CloseHandle(proc);
		
		ProcessInfo processInfo = new ProcessInfo();
		processInfo.pid = pid;
		processInfo.exePath = path;
		return processInfo;
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
	
	static Stream<DesktopWindow> getAllWindows(boolean onlyVisible) {
		return WindowUtils.getAllWindows(true).stream();
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
	
	static boolean closeWindow(WinDef.HWND hwnd) {
		return user32.DestroyWindow(hwnd);
	}
	
	static boolean moveWindow(WinDef.HWND hWnd, int x, int y, int nWidth, int nHeight, boolean bRepaint) {
		return WinAPI.user32.MoveWindow(hWnd, x, y, nWidth, nHeight, bRepaint);
	}
	
	/**
	 * @param nCmdShow see WinUser#SW_*
	 */
	static boolean showWindow(WinDef.HWND hWnd, int nCmdShow) {
		return user32.ShowWindow(hWnd, nCmdShow);
	}
}
