package mysh.appcontainer;

import mysh.util.OSUtil;
import mysh.util.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * an application container. just like jsp container such as Tomcat or Jetty.<br/>
 * It's created to reduce memory usage in a multi-jvm env,
 * so that java apps can share one jvm, and make a better use of memory.
 * <p>
 * the main class of the app should implement a static method called "shutdown",
 * which releases all resources the app applied, such as threads, sockets, files and so on.
 * this method will be invoked when the app killed by AppContainer, and this method
 * should also be invoked when the app exits itself.
 * the app should never invoke System.exit, it's obviously, but be careful of JFrame,
 * don't do JFrame.setDefaultCloseOperation(EXIT_ON_CLOSE).
 * <p>
 * notice that, the META-INF/MANIFEST.MF should be inside jar files and declares the main class,
 * so the AppContainer can start the app.
 * <p>
 * usage example:  echo "c:/xxx.jar" param1 param2 | nc localhost 12345
 */
public class AppContainer {
	private static final Logger log = LoggerFactory.getLogger(AppContainer.class);
	private static final String fileSep = OSUtil.getOS() == OSUtil.OS.Windows ? ";" : ":";
	private final AtomicLong appCount = new AtomicLong(1);
	private final Map<Long, AppInfo> apps = new ConcurrentHashMap<>();
	private final ConsoleHelper consoleHelper = new ConsoleHelper();

	public AppContainer(int serverPort) throws IOException {
		ServerSocket sock = new ServerSocket(serverPort);

		consoleHelper.startMonitor();

		Socket accept;
		while ((accept = sock.accept()) != null) {
			Socket sockAcc = accept;
			new Thread() {
				@Override
				public void run() {
					handleReq(sockAcc);
				}
			}.start();
		}

		sock.close();
	}

	private void handleReq(Socket accept) {
		try (Socket sock = accept;
		     BufferedReader r = new BufferedReader(new InputStreamReader(sock.getInputStream()))) {
			if (!sock.getRemoteSocketAddress().toString().contains("127.0.0.1"))
				return;

			String cmd = r.readLine();
			if ("c".equals(cmd)) {
				consoleHelper.goConsole(r, new PrintWriter(sock.getOutputStream()));
			} else {
				log.info("on get:" + cmd);
				handleCmd(cmd);
			}
		} catch (Exception e) {
			if (!(e instanceof SocketException))
				log.error("execute cmd error.", e);
		}
	}

	private void handleCmd(String cmd) throws IOException {
		if (cmd != null && cmd.length() > 0) {
			List<String> params = OSUtil.parseCmdLine(cmd);
			String[] files = params.get(0).split(fileSep);

			ArrayList<URL> urls = new ArrayList<>();
			for (String file : files) {
				urls.add(new URL("jar:file:///" + file + "!/"));
			}
			AcLoader cl = new AcLoader(urls.toArray(new URL[urls.size()]),
							AppContainer.class.getClassLoader().getParent());
			Thread.currentThread().setContextClassLoader(cl);

			String mainClass = null, line;
			Enumeration<URL> manifests = cl.getResources("META-INF/MANIFEST.MF");
			SearchMainClass:
			while (manifests.hasMoreElements()) {
				InputStream in = manifests.nextElement().openStream();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
					while ((line = reader.readLine()) != null) {
						if (line.startsWith("Main-Class:")) {
							mainClass = line.substring(12);
							break SearchMainClass;
						}
					}
				} catch (Exception e) {
				}
			}

			try {
				if (mainClass != null) {
					Class<?> clazz = Class.forName(mainClass, true, cl);
					Method mainMethod = clazz.getMethod("main", String[].class);
					Method shutdownMethod = clazz.getMethod("shutdown");
					if (0 == (shutdownMethod.getModifiers() & Modifier.STATIC))
						throw new Exception("shutdown method should be static");

					long id = appCount.incrementAndGet();
					apps.put(id, new AppInfo(cl, id, cmd,
									() -> {
										try {
											shutdownMethod.setAccessible(true);
											shutdownMethod.invoke(clazz);
											cl.close();
										} catch (Exception e) {
											log.error("shutdown " + clazz.getName() + " error", e);
										}
									}));
					params.remove(0);
					mainMethod.invoke(clazz, new Object[]{params.toArray(new String[params.size()])});
				} else {
					log.info("can't find mainClass:" + cmd);
					cl.close();
				}
			} catch (Exception e) {
				log.error("start app error: " + cmd, e);
				cl.close();
				Runtime.getRuntime().exec("javaw -jar " + cmd);
			}
		}
	}

	private class ConsoleHelper {
		private void startMonitor() {
			Thread t = new Thread("AppContainer-Monitor") {
				@Override
				public void run() {
					while (true) {
						try {
							Thread.sleep(15000);
							Set<AcLoader> acLoaders = new HashSet<>();
							ClassLoader cl;
							for (Thread thread : ThreadUtil.allThreads()) {
								cl = thread.getContextClassLoader();
								if (cl instanceof AcLoader)
									acLoaders.add((AcLoader) cl);
							}
							apps.values().stream()
											.filter(appInfo -> !acLoaders.contains(appInfo.cl))
											.forEach(appInfo -> apps.remove(appInfo.id));
						} catch (InterruptedException e) {
							return;
						} catch (Exception e) {
							log.error("", e);
						}
					}
				}
			};
			t.setDaemon(true);
			t.start();
		}

		private void goConsole(BufferedReader reader, PrintWriter writer) throws Exception {
			writer.println("welcome to AppContainer console.");
			writer.println("available cmds:");
			writer.println("l \t- list all killable running apps");
			writer.println("k [id] \t- kill app by id");
			writer.println("r [id] \t- restart app by id");
			writer.println();
			writer.flush();

			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0) continue;
				List<String> params = OSUtil.parseCmdLine(line);
				switch (params.get(0)) {
					case "l":
						consoleList(writer);
						break;
					case "k":
						consoleKill(writer, params);
						break;
					case "r":
						consoleRestart(writer, params);
						break;
					default:
						writer.println("unknown command");
				}
				writer.println();
				writer.flush();
			}
		}

		private void consoleList(PrintWriter writer) {
			for (AppInfo appInfo : apps.values()) {
				writer.println(appInfo);
			}
		}

		private AppInfo consoleKill(PrintWriter writer, List<String> params) throws InterruptedException, IOException {
			long id = Long.parseLong(params.get(1));
			AppInfo appInfo = apps.get(id);
			if (appInfo == null) {
				writer.println("app doesn't exist");
			} else {
				appInfo.shutdown.run();
				appInfo.cl.close();
				writer.println(id + " killed");
				apps.remove(appInfo.id);
			}
			return appInfo;
		}

		private void consoleRestart(PrintWriter writer, List<String> params) throws IOException, InterruptedException {
			AppInfo appInfo = consoleKill(writer, params);
			if (appInfo != null) {
				handleCmd(appInfo.cmd);
				writer.println("restart complete");
			}
		}
	}

	private static class AcLoader extends URLClassLoader {

		public AcLoader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}
	}

	private static class AppInfo {
		private static DateTimeFormatter df = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");

		AcLoader cl;
		long id;
		LocalDateTime startTime;
		String cmd;
		Runnable shutdown;

		public AppInfo(AcLoader cl, long id, String cmd, Runnable shutdown) {
			this.cl = cl;
			this.id = id;
			this.startTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
			this.cmd = cmd;
			this.shutdown = shutdown;
		}

		@Override
		public String toString() {
			return id + "\t" + df.format(startTime) + "\t" + cmd;
		}
	}

	public static void main(String[] args) throws IOException {
		try {
			new AppContainer(Integer.parseInt(args[0]));
		} catch (Exception e) {
			System.out.println("usage: serverPort - AppContainer listener port");
		}
	}
}
