package mysh.appcontainer;

import mysh.util.OSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * an application container. just like jsp container such as Tomcat or Jetty.<br/>
 * It's created to reduce memory usage in a multi-jvm env,
 * so that java apps can share one jvm, and make a better use of memory.
 *
 * notice that, the app should at least terminate all thread it created when it exit,
 * or they will remain there even they are daemon.
 *
 * usage example:  echo "c:/xxx.jar" param1 param2 | nc localhost 12345
 */
public class AppContainer {
	private static final Logger log = LoggerFactory.getLogger(AppContainer.class);

	public AppContainer(int serverPort) throws IOException {
		ServerSocket sock = new ServerSocket(serverPort);
		OSUtil.OS os = OSUtil.getOS();
		String fileSep = os == OSUtil.OS.Windows ? ";" : ":";

		Socket accept;
		while ((accept = sock.accept()) != null) {
			Socket sockAcc = accept;
			new Thread() {
				@Override
				public void run() {
					handleReq(fileSep, sockAcc);
				}
			}.start();
		}

		sock.close();
	}

	private void handleReq(String fileSep, Socket accept) {
		try (Socket sock = accept;
		     BufferedReader r = new BufferedReader(new InputStreamReader(sock.getInputStream()))) {
			String cmd = r.readLine();
			if (cmd != null && cmd.length() > 0) {
				log.info("on get:" + cmd);
				List<String> params = OSUtil.parseCmdLine(cmd);
				String[] files = params.get(0).split(fileSep);

				ArrayList<URL> urls = new ArrayList<>();
				for (String file : files) {
					urls.add(new URL("jar:file:///" + file + "!/"));
				}
				URLClassLoader cl = new URLClassLoader(urls.toArray(new URL[urls.size()]),
								AppContainer.class.getClassLoader().getParent());
				Thread.currentThread().setContextClassLoader(cl);
				InputStream in = cl.getResourceAsStream("META-INF/MANIFEST.MF");
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
					String line, mainClass = null;
					while ((line = reader.readLine()) != null) {
						if (line.startsWith("Main-Class:")) {
							mainClass = line.substring(12);
							break;
						}
					}
					if (mainClass != null) {
						Class<?> clazz = Class.forName(mainClass, true, cl);
						Method mainMethod = clazz.getMethod("main", String[].class);
						params.remove(0);
						mainMethod.invoke(clazz, new Object[]{params.toArray(new String[params.size()])});
					} else {
						log.info("can't find mainClass:" + cmd);
					}
				} catch (Exception e) {
					log.error(cmd, e);
				}
			}
		} catch (Exception e) {
			log.error("execute cmd error.", e);
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
