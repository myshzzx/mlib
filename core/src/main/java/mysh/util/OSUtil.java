package mysh.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;

/**
 * @author Mysh
 * @since 2014/10/12 18:37
 */
public class OSUtil {
	public static enum OS {
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
	 */
	public static void restart() throws IOException {
		String cmd = getCmdLine();
		Runtime runtime = Runtime.getRuntime();
		runtime.addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					runtime.exec(cmd);
				} catch (IOException e) {
				}
			}
		});
		System.exit(0);
	}
}
