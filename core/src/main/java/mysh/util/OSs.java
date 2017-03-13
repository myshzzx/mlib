package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 * @author Mysh
 * @since 2014/10/12 18:37
 */
public class OSs {
	private static final Logger log = LoggerFactory.getLogger(OSs.class);

	public enum OS {
		Windows, Linux, Mac, Unix, Unknown
	}

	/**
	 * get os.
	 */
	public static OS getOS() {
		OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		String osName = osBean.getName().toLowerCase();
		if (osName.contains("windows"))
			return OS.Windows;
		else if (osName.contains("linux"))
			return OS.Linux;
		else if (osName.contains("mac"))
			return OS.Mac;
		else if (osName.contains("unix"))
			return OS.Unix;
		else
			return OS.Unknown;
	}

	/**
	 * get current process id.
	 */
	public static int getPid() {
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		String name = runtimeMXBean.getName();
		int idx = name.indexOf('@');
		return Integer.parseInt(name.substring(0, idx));
	}

	/**
	 * get command line of current process.
	 */
	public static String getCmdLine() throws IOException {
		Runtime runtime = Runtime.getRuntime();
		String pid = String.valueOf(getPid());
		String cmd;
		switch (getOS()) {
			case Windows:
				cmd = "wmic process where processid=" + pid + " get commandline";
				break;
			case Linux:
			case Unix:
			case Mac:
				cmd = "ps -fhp " + pid + " -o cmd";
				break;
			default:
				throw new RuntimeException("can't get cmdLine from unknown os.");
		}

		Process proc = runtime.exec(cmd);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
			String line;
			do {
				line = reader.readLine();
			} while (line != null && !line.contains("java"));
			return line;
		} finally {
			proc.destroy();
		}
	}

	/**
	 * restart current process.
	 *
	 * @param inheritIO see {@link ProcessBuilder#inheritIO()}
	 * @throws java.io.IOException
	 */
	public static void restart(boolean inheritIO) throws IOException {
		String cmd = getCmdLine();
		log.debug("restart cmdLine:" + cmd);

		executeCmd(cmd, inheritIO);

		System.exit(0);
	}

	/**
	 * execute a cmd without waiting for termination.
	 *
	 * @param inheritIO inherit current process IO, then child process will output to current process.
	 */
	public static Process executeCmd(String cmd, boolean inheritIO) throws IOException {
		log.info("executeCmd: {}", cmd);

		StringTokenizer st = new StringTokenizer(cmd);
		String[] cmdArray = new String[st.countTokens()];
		for (int i = 0; st.hasMoreTokens(); i++)
			cmdArray[i] = st.nextToken();

		ProcessBuilder pb = new ProcessBuilder(cmdArray);
		if (inheritIO)
			pb.inheritIO();
		return pb.start();
	}

	public enum OsProcPriority {
		VeryHigh("real time", "-20"),
		High("high priority", "-12"),
		AboveNormal("above normal", "-5"),
		Normal("normal", "0"),
		BelowNormal("below normal", "5"),
		VeryLow("idle", "19");

		String win;
		String linux;

		OsProcPriority(String win, String linux) {
			this.win = win;
			this.linux = linux;
		}
	}

	/**
	 * change process priority.
	 * Windows: wmic process where ProcessId=2547 CALL setpriority "xxx"
	 * Linux: renice -p 3534 -n -19
	 */
	public static void changePriority(int pid, OsProcPriority priority) throws IOException {
		String cmd;
		switch (getOS()) {
			case Windows:
				cmd = "wmic process where ProcessId=" + pid + " CALL setpriority \"" + priority.win + "\"";
				break;
			default:
				cmd = "renice -n " + priority.linux + " -p " + pid;
		}
		log.debug("change priority: " + cmd);
		Runtime.getRuntime().exec(cmd);
	}

	/**
	 * parse cmd line to separated params.<br/>
	 * example1: [cmd param1 "param 2"] -> [cmd, param1, param 2]<br/>
	 * example1: [cmd param1 "param \" 2"] -> [cmd, param1, param " 2]<br/>
	 * example1: [cmd param1 'a " b'] -> [cmd, param1, a " b]
	 */
	public static List<String> parseCmdLine(String cmd) {
		Objects.requireNonNull(cmd, "cmd line should not be null");
		cmd = cmd.trim();
		if (cmd.length() == 0) return Collections.emptyList();

		List<String> r = new ArrayList<>();
		char[] chars = cmd.toCharArray();
		int start = 0;
		char waiting = 0, c;
		int len = chars.length;
		for (int i = 0; i < len; i++) {
			c = chars[i];
			if (c == '\'' || c == '"') {
				waiting = c;
				start = ++i;
				while (i < len) {
					c = chars[i];
					if (c == '\\')
						i += 2;
					else if (c != waiting)
						i++;
					else break;
				}
				i = i > len ? len : i;
				r.add(new String(chars, start, i - start));
				i++;
			} else if (c > ' ') {
				start = i;
				while (i < len && chars[i] > ' ') i++;
				r.add(new String(chars, start, i - start));
			}
		}
		for (int i = 0; i < r.size(); i++) {
			r.set(i, r.get(i).replace("\\\"", "\"").replace("\\'", "'"));
		}
		return r;
	}

	/**
	 * current system cpu usage. [0, 1]
	 */
	public static double cpuUsageSystem() {
		OperatingSystemMXBean t = ManagementFactory.getOperatingSystemMXBean();
		if (t instanceof com.sun.management.OperatingSystemMXBean) {
			com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) t;
			return osBean.getSystemCpuLoad();
		} else {
			throw new RuntimeException("can't get system cpu usage");
		}
	}

	/**
	 * list all windows processes. it's a heavy operation.
	 */
	public static List<ProcessInfoWin32> allWinProcesses() throws IOException {
		Process p = Runtime.getRuntime().exec("wmic process get Name,CreationDate,ExecutablePath,ParentProcessId,Priority,ProcessId,ThreadCount,HandleCount,UserModeTime,KernelModeTime,VirtualSize,WorkingSetSize,PeakVirtualSize,PeakWorkingSetSize,CommandLine");
		Map<String, ProcessInfoWin32> r = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
			String header = reader.readLine();
			String[] cols = header.split("\\s+");
			int[] colsIdx = new int[cols.length];
			for (int i = 1; i < cols.length; i++) {
				colsIdx[i] = header.indexOf(cols[i], colsIdx[i - 1]);
			}

			List<ProcessInfoWin32> processes = new ArrayList<>();
			Map<String, String> infoMap = new HashMap<>();
			reader.lines()
							.filter(line -> line.length() > 0)
							.forEach(line -> {
								for (int i = 0; i < cols.length; i++) {
									String key = cols[i];
									String value = i < cols.length - 1 ?
													line.substring(colsIdx[i], colsIdx[i + 1]) : line.substring(colsIdx[i]);
									value = value.trim();
									infoMap.put(key, value);
								}
								processes.add(new ProcessInfoWin32(infoMap));
							});
			return processes;
		} finally {
			p.destroy();
		}
	}

	private static final DateTimeFormatter CREATION_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	public static class ProcessInfoWin32 implements Serializable {
		private static final long serialVersionUID = -2891053091206604968L;

		private int pid, parentPid, priority;
		private String name, cmdLine, exePath;
		private LocalDateTime startTime;
		private int threadCount, handleCount;
		private long userModeMicroSec, kernelModeMicroSec;
		private long virtualSize, workingSetSize, peakVirtualSize, peakWorkingSetSize;

		private ProcessInfoWin32(Map<String, String> m) {
			name = m.get("Name");
			pid = Integer.parseInt(m.get("ProcessId"));
			parentPid = Integer.parseInt(m.get("ParentProcessId"));
			cmdLine = m.get("CommandLine");
			startTime = LocalDateTime.parse(m.get("CreationDate").substring(0, 14), CREATION_DATE_FMT);
			exePath = m.get("ExecutablePath");
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
							", cmdLine='" + cmdLine + '\'' +
							'}';
		}

		public int getPid() {
			return pid;
		}

		public int getParentPid() {
			return parentPid;
		}

		public int getPriority() {
			return priority;
		}

		public String getName() {
			return name;
		}

		public String getCmdLine() {
			return cmdLine;
		}

		public String getExePath() {
			return exePath;
		}

		public LocalDateTime getStartTime() {
			return startTime;
		}

		public int getThreadCount() {
			return threadCount;
		}

		public int getHandleCount() {
			return handleCount;
		}

		public long getUserModeMicroSec() {
			return userModeMicroSec;
		}

		public long getKernelModeMicroSec() {
			return kernelModeMicroSec;
		}

		public long getVirtualSize() {
			return virtualSize;
		}

		public long getWorkingSetSize() {
			return workingSetSize;
		}

		public long getPeakVirtualSize() {
			return peakVirtualSize;
		}

		public long getPeakWorkingSetSize() {
			return peakWorkingSetSize;
		}
	}

	public static void terminateProcess(int pid, boolean force) {
		try {
			switch (getOS()) {
				case Windows:
					executeCmd("TASKKILL " + (force ? "/F " : "") + "/PID " + pid, true).waitFor();
					return;
				case Linux:
				case Mac:
				case Unix:
					executeCmd("kill " + (force ? "-9 " : "") + pid, true).waitFor();
					return;
				case Unknown:
				default:
					throw new UnsupportedOperationException("unsupported os: " + getOS());
			}
		} catch (Exception e) {
			log.error("terminateProcess error. pid=" + pid + ", force=" + force, e);
		}
	}
}
