
package mysh.jpipe;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class Pipe {

	private Socket localSock;

	private Socket remoteSock;

	/**
	 * 连接管道器.<br/>
	 * 收到本地连接请求, 连接到远程目标, 生成插件实例, 建立双向通信管道.
	 * 
	 * @param localSock
	 * @param remoteHost
	 * @param remotePort
	 */
	public Pipe(Socket localSock, String remoteHost, int remotePort) {

		this.localSock = localSock;

		try {
			this.remoteSock = new Socket(remoteHost, remotePort);

			List<Plugin> plugins = PluginsGenerator.generatePluginsInstance(
					this.localSock, this.remoteSock);
			new Pusher(Pusher.Type.LOCAL, this.localSock, this.remoteSock, plugins)
					.start();
			new Pusher(Pusher.Type.REMOTE, this.remoteSock, this.localSock, plugins)
					.start();
		} catch (Exception e) {
			System.err.println("连接到 [" + remoteHost + ": " + remotePort
					+ "] 失败. 无法建立数据管道.");
			if (this.localSock != null) {
				try {
					this.localSock.close();
				} catch (IOException e1) {
				}
			}
		}
	}

}
