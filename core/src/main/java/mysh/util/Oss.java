package mysh.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * os utils.
 *
 * @author Mysh
 * @since 2014/10/12 18:37
 */
public class Oss {
	private static final Logger log = LoggerFactory.getLogger(Oss.class);

	public static final String fileSep = System.getProperty("file.separator");
	public static final String lineSep = System.getProperty("line.separator");

	public enum OS {
		Windows, Linux, Mac, Unix, Unknown
	}

	private static OS currOs;

	/**
	 * get os.
	 */
	public static OS getOS() {
		if (currOs != null)
			return currOs;

		OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		String osName = osBean.getName().toLowerCase();
		if (osName.contains("windows"))
			currOs = OS.Windows;
		else if (osName.contains("linux"))
			currOs = OS.Linux;
		else if (osName.contains("mac"))
			currOs = OS.Mac;
		else if (osName.contains("unix"))
			currOs = OS.Unix;
		else
			currOs = OS.Unknown;

		return currOs;
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
		int pid = getPid();
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
		return executeCmd(cmd, inheritIO, true);
	}

	/**
	 * execute a cmd without waiting for termination.
	 *
	 * @param inheritIO inherit current process IO, then child process will output to current process.
	 */
	public static Process executeCmd(String cmd, boolean inheritIO, boolean printLog) throws IOException {
		if (printLog)
			log.debug("executeCmd: {}", cmd);

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
		if (cmd.length() == 0)
			return Collections.emptyList();

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
					else
						break;
				}
				i = i > len ? len : i;
				r.add(new String(chars, start, i - start));
				i++;
			} else if (c > ' ') {
				start = i;
				while (i < len && chars[i] > ' ')
					i++;
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
	public static double getCpuUsageSystem() {
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
	 *
	 * @param fetchCmdLine fetch cmd line or not. it's an expensive op, fetch them only if you need to iterate all processes' cmd lines.
	 */
	public static List<ProcessInfoWin32> getAllWinProcesses(boolean fetchCmdLine) throws IOException {
		if (getOS() != OS.Windows) {
			return Collections.emptyList();
		}
		Charset winCharset = getOsCharset();
		String wmicGetProcs = "wmic process get Name,CreationDate,ExecutablePath,ParentProcessId,Priority,ProcessId,ThreadCount,HandleCount,UserModeTime,KernelModeTime,VirtualSize,WorkingSetSize,PeakVirtualSize,PeakWorkingSetSize";
		if (fetchCmdLine)
			wmicGetProcs += ",CommandLine";
		Process p = executeCmd(wmicGetProcs, false, false);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), winCharset))) {
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
						byte[] lineBytes = line.getBytes(winCharset); for (int i = 0; i < cols.length; i++) {
							String key = cols[i];
							String value = i < cols.length - 1 ?
									new String(lineBytes, colsIdx[i], colsIdx[i + 1] - colsIdx[i], winCharset)
									: new String(lineBytes, colsIdx[i], lineBytes.length - colsIdx[i], winCharset);
							value = value.trim();
							infoMap.put(key, value);
						}
						processes.add(new ProcessInfoWin32(infoMap));
					});
			return processes;
		}
	}

	private static final DateTimeFormatter CREATION_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	public static class ProcessInfoWin32 implements Serializable {
		private static final long serialVersionUID = -2891053091206604968L;

		private static Cache<Long, String> cmdLineCache = CacheBuilder.newBuilder()
				.maximumSize(1000)
				.expireAfterWrite(10, TimeUnit.HOURS)
				.build();
		private int pid, parentPid, priority;
		private String name, exePath, cmdLine;
		private LocalDateTime startTime;
		private int threadCount, handleCount;
		private long userModeMicroSec, kernelModeMicroSec;
		private long virtualSize, workingSetSize, peakVirtualSize, peakWorkingSetSize;

		private ProcessInfoWin32(Map<String, String> m) {
			name = m.get("Name");
			pid = Integer.parseInt(m.get("ProcessId"));
			parentPid = Integer.parseInt(m.get("ParentProcessId"));

			startTime = LocalDateTime.parse(m.get("CreationDate").substring(0, 14), CREATION_DATE_FMT);
			exePath = m.get("ExecutablePath"); cmdLine = m.get("CommandLine");
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
							byte[] infoBytes = readFromProcess("wmic process where processid=" + pid + " get CommandLine");
							String info = new String(infoBytes, getOsCharset());
							return info.startsWith("CommandLine") ? info.substring(11).trim() : "";
						});
			} catch (ExecutionException e) {
				log.error("get cmd line error. " + this, e);
				return null;
			}
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

	public static void terminateLinuxProcessTree(Process process) {
		if (process != null) {
			try {
				Field fpid = process.getClass().getDeclaredField("pid");
				fpid.setAccessible(true);
				int pid = fpid.getInt(process);
				executeCmd("pkill -TERM -P " + pid, false);
				terminateProcess(pid, true);
			} catch (Exception e) {
				log.error("process-cannot-be-terminated", e);
			}
		}
	}

	/**
	 * read process output content.
	 * this method will block until process terminate, so make sure the process will exit immediately.
	 */
	public static byte[] readFromProcess(String cmd) throws IOException {
		Process process = executeCmd(cmd, false);
		byte[] buf = new byte[1000];
		ByteArrayOutputStream aos = new ByteArrayOutputStream(buf.length);
		try (InputStream in = process.getInputStream()) {
			int len;
			while ((len = in.read(buf)) > -1) {
				aos.write(buf, 0, len);
			}
		}
		return aos.toByteArray();
	}

	/**
	 * default app charset of current OS
	 */
	public static Charset getOsCharset() {
		String enc = System.getProperty("sun.jnu.encoding");
		if (enc == null)
			enc = System.getProperty("ibm.system.encoding");

		if (enc == null)
			return Oss.getOS() == OS.Windows ? Encodings.GBK : Encodings.UTF_8;
		else
			return Charset.forName(enc);
	}
}
