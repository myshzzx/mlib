
package mysh.jpipe;

import java.io.IOException;
import java.net.ServerSocket;

public class Listener extends Thread {

	private int listeningPort;

	private String targetHost;

	private int targetPort;

	/**
	 * 请求监听器.<br/>
	 * 在本地端口监听, 收到连接请求时启动管道器, 这实际是一个请求分发器.
	 */
	public Listener(int listeningPort, String targetHost, int targetPort) {

		super("Listener");

		if (listeningPort < 1 || listeningPort > 65534 || targetPort < 1
				|| targetPort > 65534) {
			throw new RuntimeException("invalid port");
		}

		this.listeningPort = listeningPort;
		this.targetHost = targetHost;
		this.targetPort = targetPort;

		System.out.println("Listener start, [ localhost : " + this.listeningPort + " ] ~ [ "
				+ this.targetHost + " : " + this.targetPort + " ]");
	}

	@Override
	public void run() {

		try {
			ServerSocket server = new ServerSocket(this.listeningPort);

			while (true) {
				try {
					new Pipe(server.accept(), this.targetHost, this.targetPort);
				} catch (IOException e) {
					System.err.println(e);
				}
			}
		} catch (IOException e) {
			System.err.println("在 " + this.listeningPort + " 端口监听失败, " + e);
		}

	}
}
