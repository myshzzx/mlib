package mysh.thrift;

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServerEventHandler;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TServerSocket;
import org.springframework.core.io.Resource;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * a TLS-supported and non-blocking thrift-server maker.
 */
public class ThriftServerFactory {

	// 客户端不超时（允许长连接）
	private final int clientTimeout = 0;
	private String serverHost;
	private int serverPort;
	private TProcessor processor;

	private boolean useTLS;
	private Resource selfKeyStore;
	private String selfKeyStorePw;
	private boolean isRequireClientAuth = true;
	private Resource trustKeyStore;
	private String trustKeyStorePw;

	private TServerEventHandler serverEventHandler;

	/**
	 * @return build but not start server.
	 * @throws Exception
	 */
	public TServer build() throws Exception {
		final TServer server;
		if (useTLS) { // TLS server
			TSSLTransportFactory.TSSLTransportParameters transportParams = new TSSLTransportFactory.TSSLTransportParameters();
			transportParams.setKeyStore(this.selfKeyStore.getFile().getAbsolutePath(), this.selfKeyStorePw);
			transportParams.requireClientAuth(this.isRequireClientAuth);
			if (this.isRequireClientAuth)
				transportParams.setTrustStore(this.trustKeyStore.getFile().getAbsolutePath(), this.trustKeyStorePw);
			TServerSocket serverTransport = TSSLTransportFactory.getServerSocket(this.serverPort, this.clientTimeout,
							InetAddress.getByName(this.serverHost), transportParams);

			server = new TThreadPoolServer(
							new TThreadPoolServer.Args(serverTransport)
											.processor(this.processor)
											.protocolFactory(new TCompactProtocol.Factory())
											.executorService(Executors.newCachedThreadPool()));
		} else { // Non security Server
			TNonblockingServerSocket serverTransport = new TNonblockingServerSocket(
							new InetSocketAddress(this.serverHost, this.serverPort), this.clientTimeout);

			server = new THsHaServer(
							new THsHaServer.Args(serverTransport)
											.processor(this.processor)
											.protocolFactory(new TCompactProtocol.Factory()));
		}

		if (this.serverEventHandler != null) {
			server.setServerEventHandler(this.serverEventHandler);
		}

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
	 * 是否启用 TLS. 安全相关配置的开关.
	 */
	public void setUseTLS(boolean useTLS) {
		this.useTLS = useTLS;
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

}
