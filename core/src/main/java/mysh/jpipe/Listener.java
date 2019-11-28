
package mysh.jpipe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Listener implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(Listener.class);
	
	private final int listeningPort;
	private final String targetHost;
	private final int targetPort;
	private final int bufLen;
	private ServerSocket server;
	private final Map<Socket, Object> localSocks = new ConcurrentHashMap<>();
	
	/**
	 * 请求监听器.<br/>
	 * 在本地端口监听, 收到连接请求时启动管道器, 这实际是一个请求分发器.
	 */
	public Listener(int listeningPort, String targetHost, int targetPort, int bufLen) {
		
		if (listeningPort < 1 || listeningPort > 65534 || targetPort < 1
				|| targetPort > 65534) {
			throw new RuntimeException("invalid port");
		}
		
		this.listeningPort = listeningPort;
		this.targetHost = targetHost;
		this.targetPort = targetPort;
		this.bufLen = bufLen;
	}
	
	public void start() throws IOException {
		server = new ServerSocket(this.listeningPort);
		log.info("Listener start, [ localhost : " + this.listeningPort + " ] ~ [ "
				+ this.targetHost + " : " + this.targetPort + " ]");
		Thread t = new Thread("jpipe.Listener") {
			@Override
			public void run() {
				try {
					while (!isInterrupted()) {
						Socket sock = server.accept();
						localSocks.put(sock, "");
						new Pipe(sock, targetHost, targetPort, bufLen, () -> localSocks.remove(sock));
					}
				} catch (Exception e) {
					if (!(e instanceof IOException))
						log.error("server.accept error, listener is going to exit.", e);
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}
	
	@Override
	public void close() throws IOException {
		server.close();
		localSocks.keySet().stream().forEach(sock -> {
			try {
				sock.close();
			} catch (IOException e) {
			}
		});
	}
}
