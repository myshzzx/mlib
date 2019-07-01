package mysh.appcontainer;

import mysh.util.Oss;
import mysh.util.Threads;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An application container. Just like jsp container such as Tomcat or Jetty.<br/>
 * It's created to reduce memory usage in a multi-jvm env,
 * so that java apps can share one jvm, and make a better use of memory.<br/>
 * If GUI-apps will run in the container, the container should use "single classloader",
 * because the EDT(event dispatch thread) is singleton, which holds only one classloader, so
 * jars of new apps need to be added to the classloader in case of "ClassNotFoundError",
 * but use "single classloader" may cause classes conflict, be careful of that.
 * <p/>
 * <pre>
 *   public static void shutdown() throws Exception{
 *     try{
 *        // release resources applied by the app
 *        // stop all threads started by the app
 *     } finally {
 *        // close the ClassLoader so that the jar files can be released by jvm
 *        ClassLoader cl = MainClass.class.getClassLoader();
 *        if (cl instanceof Closeable) ((Closeable) cl).close();
 *     }
 *   }
 *   </pre>
 * the main class of the app should implement a static method called "shutdown",
 * which releases all resources the app applied, such as threads, sockets, files and so on.
 * this method will be invoked when the app killed by AppContainer, and this method
 * <b>SHOULD ALSO BE INVOKED WHEN THE APP EXITS ITSELF</b>.
 * the app should never invoke System.exit, it's obviously. But be careful of JFrame,
 * don't do JFrame.setDefaultCloseOperation(EXIT_ON_CLOSE).
 * <p/>
 * NOTICE that, the META-INF/MANIFEST.MF should be inside jar files and declares the main class,
 * so the AppContainer can start the app.
 * <p></p>
 * <p>
 * usage example:  echo "c:/xxx.jar" param1 param2 | nc localhost 12345
 */
public class AppContainer {
	private static final Logger log = LoggerFactory.getLogger(AppContainer.class);
	private static final String fileSep = Oss.getOS() == Oss.OS.Windows ? ";" : ":";
	private final AtomicLong appCount = new AtomicLong(1);
	private final Map<Long, AppInfo> apps = new ConcurrentHashMap<>();
	private final ConsoleHelper consoleHelper = new ConsoleHelper();
	
	private final AcLoader singleAcLoader;
	
	public AppContainer(int serverPort, boolean useSingleClassLoader) throws IOException {
		ServerSocket sock = new ServerSocket(serverPort);
		
		consoleHelper.startMonitor();
		singleAcLoader = useSingleClassLoader ?
				                 new AcLoader(new URL[0], AppContainer.class.getClassLoader().getParent()) : null;
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			ExecutorService exec = Executors.newCachedThreadPool();
			apps.values().forEach(i -> exec.execute(() -> i.shutdown.run()));
			exec.shutdown();
			try {
				exec.awaitTermination(1, TimeUnit.MINUTES);
				sock.close();
			} catch (Exception e) {
			}
		}));
		
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
			List<String> params = Oss.parseCmdLine(cmd);
			String[] files = params.get(0).split(fileSep);
			
			ArrayList<URL> urls = new ArrayList<>();
			for (String file : files) {
				urls.add(new URL("jar:file:///" + file + "!/"));
			}
			AcLoader cl = new AcLoader(urls.toArray(new URL[urls.size()]),
					AppContainer.class.getClassLoader().getParent());
			
			// look for main class name
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
			
			if (singleAcLoader != null) {
				cl.close();
				singleAcLoader.addURLs(urls);
				cl = singleAcLoader;
			}
			AcLoader appLoader = cl;
			Thread.currentThread().setContextClassLoader(appLoader);
			
			try {
				if (mainClass != null) {
					Class<?> clazz = Class.forName(mainClass, true, appLoader);
					Method mainMethod = clazz.getMethod("main", String[].class);
					Method shutdownMethod = clazz.getMethod("shutdown");
					if (0 == (shutdownMethod.getModifiers() & Modifier.STATIC))
						throw new Exception("shutdown method should be static");
					
					long id = appCount.incrementAndGet();
					apps.put(id, new AppInfo(appLoader, id, cmd,
							() -> {
								try {
									shutdownMethod.setAccessible(true);
									shutdownMethod.invoke(clazz);
									appLoader.close();
								} catch (Exception e) {
									log.error("shutdown " + clazz.getName() + " error", e);
								}
							}));
					params.remove(0);
					mainMethod.invoke(clazz, new Object[]{params.toArray(new String[params.size()])});
				} else {
					log.info("can't find mainClass:" + cmd);
					appLoader.close();
				}
			} catch (Exception e) {
				log.error("start app error: " + cmd, e);
				appLoader.close();
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
							for (Thread thread : Threads.allThreads()) {
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
				List<String> params = Oss.parseCmdLine(line);
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
	
	private class AcLoader extends URLClassLoader {
		
		public AcLoader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}
		
		public synchronized void addURLs(ArrayList<URL> urls) {
			URL[] oldUrls = getURLs();
			CHECK:
			for (URL url : new HashSet<>(urls)) {
				for (URL oldUrl : oldUrls) {
					if (Objects.equals(url, oldUrl)) {
						continue CHECK;
					}
				}
				this.addURL(url);
			}
		}
		
		@Override
		public void close() throws IOException {
			if (this != singleAcLoader)
				super.close();
		}
		
		public void forceClose() throws IOException {
			super.close();
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
			new AppContainer(Integer.parseInt(args[0]), Boolean.valueOf(args[1]));
		} catch (Exception e) {
			System.out.println("usage: serverPort useSingleClassLoader");
			System.out.println("serverPort - AppContainer listener port");
			System.out.println("useSingleClassLoader - is use only one class loader for all apps");
		}
	}
}
