package mysh.thrift;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TTransport;
import org.springframework.core.io.Resource;

/**
 * SSL 安全连接客户端对象池对象构建工厂。
 *
 * @param <T> 要构建的客户端对象接口。
 * @author 张智贤
 */
public class ThriftSSLClientPoolObjFactory<T extends TServiceClient> implements PoolableObjectFactory<T> {

	private Class<T> clientClass;
	private String serverHost;
	private int serverPort;
	private int clientInvokeTimeoutMilli = 10000;
	private Resource selfKeyStore;
	private String selfKeyStorePw;
	private boolean isRequireClientAuth = true;
	private Resource trustKeyStore;
	private String trustKeyStorePw;

	@Override
	public T makeObject() throws Exception {
		TSSLTransportFactory.TSSLTransportParameters transportParams = new TSSLTransportFactory.TSSLTransportParameters();
		transportParams.setTrustStore(this.trustKeyStore.getFile().getAbsolutePath(), this.trustKeyStorePw);
		if (this.isRequireClientAuth) {
			transportParams.setKeyStore(this.selfKeyStore.getFile().getAbsolutePath(), this.selfKeyStorePw);
		}
		TTransport transport = TSSLTransportFactory.getClientSocket(this.serverHost, this.serverPort, this.clientInvokeTimeoutMilli, transportParams);

		TProtocol protocol = new TCompactProtocol(transport);
		return clientClass.getConstructor(TProtocol.class).newInstance(protocol);
	}

	@Override
	public void destroyObject(T obj) throws Exception {
		obj.getOutputProtocol().getTransport().close();
		if (obj.getInputProtocol().getTransport().isOpen()) {
			obj.getInputProtocol().getTransport().close();
		}
	}

	@Override
	public boolean validateObject(T obj) {
		// 由于 Thrift 的问题，isClose() 和 isOpen() 都不能检查连接有效性，这里用 flush 异常检查
		try {
			obj.getOutputProtocol().getTransport().flush();
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	@Override
	public void activateObject(T obj) throws Exception {
	}

	@Override
	public void passivateObject(T obj) throws Exception {
	}

	/**
	 * 要构建的客户端类。
	 */
	public void setClientClass(Class<T> clientClass) {
		this.clientClass = clientClass;
	}

	/**
	 * 服务端主机。
	 */
	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}

	/**
	 * 服务端端口。
	 */
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * 客户端连接后远程调用超时。
	 */
	public void setClientInvokeTimeoutMilli(int clientInvokeTimeoutMilli) {
		this.clientInvokeTimeoutMilli = clientInvokeTimeoutMilli;
	}

	/**
	 * 本地JKS证书库。
	 */
	public void setSelfKeyStore(Resource selfKeyStore) {
		this.selfKeyStore = selfKeyStore;
	}

	/**
	 * 本地JKS证书库密码。
	 */
	public void setSelfKeyStorePw(String selfKeyStorePw) {
		this.selfKeyStorePw = selfKeyStorePw;
	}

	/**
	 * 服务端是否要求客户端身份认证。
	 * 若需要，则客户端要提供本地证书。
	 */
	public void setRequireClientAuth(boolean requireClientAuth) {
		isRequireClientAuth = requireClientAuth;
	}

	/**
	 * 本地信任的JKS证书库。
	 */
	public void setTrustKeyStore(Resource trustKeyStore) {
		this.trustKeyStore = trustKeyStore;
	}

	/**
	 * 本地信任的JKS证书库密码。
	 */
	public void setTrustKeyStorePw(String trustKeyStorePw) {
		this.trustKeyStorePw = trustKeyStorePw;
	}
}
