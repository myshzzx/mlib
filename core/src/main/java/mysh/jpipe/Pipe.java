
package mysh.jpipe;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class Pipe {

	/**
	 * 连接管道器.<br/>
	 * 收到本地连接请求, 连接到远程目标, 生成插件实例, 建立双向通信管道.
	 */
	public Pipe(Socket localSock, String remoteHost, int remotePort) {

		try {
			Socket remoteSock = new Socket(remoteHost, remotePort);

			List<Plugin> plugins = PluginsGenerator.generatePluginsInstance(
							localSock, remoteSock);
			new Pusher(Pusher.Type.LOCAL, localSock, remoteSock, plugins)
							.start();
			new Pusher(Pusher.Type.REMOTE, remoteSock, localSock, plugins)
							.start();
		} catch (Exception e) {
			System.err.println("连接到 [" + remoteHost + ": " + remotePort
							+ "] 失败. 无法建立数据管道.");
			if (localSock != null) {
				try {
					localSock.close();
				} catch (IOException e1) {
				}
			}
		}
	}

}
