
package mysh.jpipe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class Pipe {
	private static final Logger log = LoggerFactory.getLogger(Pipe.class);

	/**
	 * 连接管道器.<br/>
	 * 收到本地连接请求, 连接到远程目标, 生成插件实例, 建立双向通信管道.
	 */
	public Pipe(Socket localSock, String remoteHost, int remotePort, int bufLen, Runnable closeNotifier) {

		try {
			Socket remoteSock = new Socket(remoteHost, remotePort);

			List<Plugin> plugins = PluginsGenerator.generatePluginsInstance(localSock, remoteSock);
			new Pusher(Pusher.Type.LOCAL, localSock, remoteSock, plugins, bufLen, closeNotifier).start();
			new Pusher(Pusher.Type.REMOTE, remoteSock, localSock, plugins, bufLen, closeNotifier).start();
		} catch (Exception e) {
			log.info("连接到 [" + remoteHost + ": " + remotePort + "] 失败. 无法建立数据管道." + e);

			if (localSock != null) {
				try {
					localSock.close();
				} catch (IOException ex) {
				}
			}

			closeNotifier.run();
		}
	}

}
