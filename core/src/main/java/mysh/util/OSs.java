package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

        StringTokenizer st = new StringTokenizer(cmd);
        String[] cmdArray = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++)
            cmdArray[i] = st.nextToken();

        ProcessBuilder pb = new ProcessBuilder(cmdArray);
        if (inheritIO)
            pb.inheritIO();
        pb.start();

        System.exit(0);
    }

    public static enum OsProcPriority {
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

    public static class ProcessInfo {
        public String pid;
        public String name;
        public long cpuNano;

        public ProcessInfo(String name, String pid, long cpuNano) {
            this.name = name;
            this.pid = pid;
            this.cpuNano = cpuNano;
        }
    }

    private static DateTimeFormatter fTime = DateTimeFormatter.ofPattern("H:mm:ss.SSS");

    public static Map<String, ProcessInfo> sysProc() throws IOException {
        Process p = Runtime.getRuntime().exec("pslist");
        Map<String, ProcessInfo> r = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            reader.lines().skip(4).forEach(line -> {
                if (Strings.isBlank(line)) return;

                String[] infos = line.split("\\s+");
                String name = infos[0];
                String pid = infos[1];
                String startTime = infos[6];
                startTime = startTime.substring(startTime.indexOf(':') - 1);
                long cpuNano = LocalTime.parse(startTime, fTime).toNanoOfDay();
                r.put(pid, new ProcessInfo(name, pid, cpuNano));
            });
        } finally {
            p.destroy();
        }
        return r;
    }
}
