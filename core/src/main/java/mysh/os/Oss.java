package mysh.os;

import mysh.util.Encodings;
import mysh.util.Exps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.Charset;
import java.util.*;

/**
 * os utils.
 *
 * @author Mysh
 * @since 2014/10/12 18:37
 */
public abstract class Oss {
	private static final Logger log = LoggerFactory.getLogger(Oss.class);
	
	public static final String fileSep = System.getProperty("file.separator");
	public static final String lineSep = System.getProperty("line.separator");
	
	public enum OS {
		Windows, Linux, Mac, Unix, Unknown
	}
	
	public enum GPU {
		nVidia, AMD, Intel, Unknown;
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
	
	public static boolean isWindows() {
		return getOS() == OS.Windows;
	}
	
	public static boolean isLinux() {
		return getOS() == OS.Linux;
	}
	
	public static boolean isMac() {
		return getOS() == OS.Mac;
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
	
	public static void terminateProcess(int pid, boolean force, boolean killTree, boolean wait) {
		try {
			switch (getOS()) {
				case Windows:
					Process process = executeCmd("TASKKILL " + (killTree ? "/T " : "") + (force ? "/F " : "") + "/PID " + pid, true);
					if (wait)
						process.waitFor();
					return;
				case Linux:
				case Mac:
				case Unix:
					Process killSubs = null;
					if (killTree) {
						killSubs = executeCmd("pkill " + (force ? "-9 " : "") + " -P " + pid, true);
					}
					Process killSelf = executeCmd("kill " + (force ? "-9 " : "") + pid, true);
					if (wait) {
						if (killTree)
							killSubs.waitFor();
						killSelf.waitFor();
					}
					return;
				case Unknown:
				default:
					throw new UnsupportedOperationException("unsupported os: " + getOS());
			}
		} catch (Exception e) {
			log.error("terminateProcess error. pid=" + pid + ", force=" + force, e);
		}
	}
	
	/**
	 * read process output content.
	 * this method will block until process terminate, so make sure the process will exit immediately.
	 */
	public static byte[] readFromProcess(String cmd) {
		try {
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
		} catch (Exception e) {
			throw Exps.unchecked(e);
		}
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
	
	private static volatile String cpuId, mainBoardSerialNumber;
	
	public static String getCpuId() {
		if (cpuId != null)
			return cpuId;
		
		switch (getOS()) {
			case Windows:
				String infos = new String(readFromProcess("wmic cpu get ProcessorId"));
				cpuId = infos.split("\\s+")[1];
				break;
			default:
				throw new UnsupportedOperationException("unsupported os: " + getOS());
		}
		
		return cpuId;
	}
	
	public static String getMainBoardSerialNumber() {
		if (mainBoardSerialNumber != null)
			return mainBoardSerialNumber;
		
		switch (getOS()) {
			case Windows:
				String infos = new String(readFromProcess("wmic baseboard get SerialNumber"));
				mainBoardSerialNumber = infos.split("\\s+")[1];
				break;
			default:
				throw new UnsupportedOperationException("unsupported os: " + getOS());
		}
		
		return mainBoardSerialNumber;
	}
	
	public static String getGPUName() {
		if (Oss.isWindows()) {
			String nameInfo = new String(Oss.readFromProcess("powershell \"Get-WmiObject -Class CIM_PCVideoController | Select-Object -Property Name\""));
			String[] is = nameInfo.trim().split("[\r\n]+");
			return is[is.length - 1];
		}
		throw new UnsupportedOperationException("need dev");
	}
	
	private static GPU currGPU;
	
	public static GPU getGPU() {
		if (currGPU == null) {
			String name = getGPUName().toUpperCase();
			if (name.contains("NVIDIA"))
				currGPU = GPU.nVidia;
			else if (name.contains("AMD"))
				currGPU = GPU.AMD;
			else if (name.contains("INTEL"))
				currGPU = GPU.Intel;
			else
				currGPU = GPU.Unknown;
		}
		return currGPU;
	}
	
}
