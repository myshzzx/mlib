package mysh.thrift;

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServerEventHandler;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.net.InetAddress;
import java.util.concurrent.Executors;

/**
 * SSL服务端构造工厂。
 *
 * @author 张智贤
 */
public class ThriftSSLServerFactory implements IServerFactory {
	private static final Logger log = LoggerFactory.getLogger(ThriftSSLServerFactory.class);
	// 客户端不超时（允许长连接）
	private final int clientTimeoutMilli = 0;
	private String serverHost;
	private int serverPort;
	private TProcessor processor;
	private Resource selfKeyStore;
	private String selfKeyStorePw;
	private boolean isRequireClientAuth = true;
	private Resource trustKeyStore;
	private String trustKeyStorePw;
	private TServerEventHandler serverEventHandler;
	private boolean isServerHolderThreadDaemon = true;

	public TServer buildAndServe() throws Exception {
		TSSLTransportFactory.TSSLTransportParameters transportParams = new TSSLTransportFactory.TSSLTransportParameters();
		transportParams.setKeyStore(this.selfKeyStore.getFile().getAbsolutePath(), this.selfKeyStorePw);
		transportParams.requireClientAuth(this.isRequireClientAuth);
		if (this.isRequireClientAuth)
			transportParams.setTrustStore(this.trustKeyStore.getFile().getAbsolutePath(), this.trustKeyStorePw);
		TServerSocket serverTransport = TSSLTransportFactory.getServerSocket(this.serverPort, this.clientTimeoutMilli,
						InetAddress.getByName(this.serverHost), transportParams);

		TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport);
		serverArgs.processor(this.processor);
		serverArgs.inputProtocolFactory(new TCompactProtocol.Factory());
		serverArgs.outputProtocolFactory(new TCompactProtocol.Factory());
		serverArgs.executorService(Executors.newCachedThreadPool());

		final TThreadPoolServer server = new TThreadPoolServer(serverArgs);
		if (this.serverEventHandler != null) {
			server.setServerEventHandler(this.serverEventHandler);
		}

		Thread serverHolderThread = new Thread("Thrift Server Holder") {
			@Override
			public void run() {
				try {
					server.serve();
				} catch (Throwable t) {
					log.error("Thrift 服务端启动失败。", t);
				}
			}
		};
		if (this.isServerHolderThreadDaemon) {
			serverHolderThread.setDaemon(true);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					if (server.isServing()) {
						server.stop();
					}
				}
			});
		}
		serverHolderThread.start();

		return server;
	}

	/**
	 * 服务主机地址。
	 */
	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}

	/**
	 * 服务端口。
	 */
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * 服务处理器。
	 */
	public void setProcessor(TProcessor processor) {
		this.processor = processor;
	}

	/**
	 * 服务端证书库。
	 */
	public void setSelfKeyStore(Resource selfKeyStore) {
		this.selfKeyStore = selfKeyStore;
	}

	/**
	 * 服务端证书库密码。
	 */
	public void setSelfKeyStorePw(String selfKeyStorePw) {
		this.selfKeyStorePw = selfKeyStorePw;
	}

	/**
	 * 是否需要客户端认证。
	 */
	public void setRequireClientAuth(boolean requireClientAuth) {
		isRequireClientAuth = requireClientAuth;
	}

	/**
	 * 信任证书库。
	 */
	public void setTrustKeyStore(Resource trustKeyStore) {
		this.trustKeyStore = trustKeyStore;
	}

	/**
	 * 信任证书库密码。
	 */
	public void setTrustKeyStorePw(String trustKeyStorePw) {
		this.trustKeyStorePw = trustKeyStorePw;
	}

	/**
	 * 服务端事件监听器。
	 */
	public void setServerEventHandler(TServerEventHandler serverEventHandler) {
		this.serverEventHandler = serverEventHandler;
	}

	/**
	 * 服务启动线程是否守护线程(默认：是).
	 */
	public void setServerHolderThreadDaemon(boolean serverHolderThreadDaemon) {
		isServerHolderThreadDaemon = serverHolderThreadDaemon;
	}
}
