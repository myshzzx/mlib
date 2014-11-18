package mysh.thrift;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.*;
import org.springframework.core.io.Resource;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.SocketException;

/**
 * a connection-pooled, TLS-supported and thread-safe thrift-client maker.
 *
 * @param <T> thrift server interface.
 */
public class ThriftClientFactory<T> {

	private class ThriftClient implements Closeable {
		private volatile T client;
		private volatile TTransport transport;

		public ThriftClient() throws Exception {
			rebuildClient();
		}

		private void rebuildClient() throws TTransportException, NoSuchMethodException,
						IllegalAccessException, InvocationTargetException, InstantiationException, IOException {

			this.close();

			if (conf.useTLS) {
				TSSLTransportFactory.TSSLTransportParameters transportParams = new TSSLTransportFactory.TSSLTransportParameters();
				transportParams.setTrustStore(conf.trustKeyStore.getFile().getAbsolutePath(), conf.trustKeyStorePw);
				if (conf.isRequireClientAuth) {
					transportParams.setKeyStore(conf.selfKeyStore.getFile().getAbsolutePath(), conf.selfKeyStorePw);
				}
				transport = TSSLTransportFactory.getClientSocket(
								conf.serverHost, conf.serverPort, conf.clientSocketTimeout, transportParams);
			} else {
				transport = new TFramedTransport(new TSocket(
								conf.serverHost, conf.serverPort, conf.clientSocketTimeout)
				);
				transport.open();
			}

			TProtocol protocol = new TCompactProtocol(transport);
			client = conf.tclient.getConstructor(TProtocol.class).newInstance(protocol);
		}

		@Override
		public void close() {
			if (transport != null)
				transport.close();
		}

		private volatile long lastInvokeTime = System.currentTimeMillis();

		public Object invokeThriftClient(Method method, Object[] args) throws Exception {
			lastInvokeTime = System.currentTimeMillis();

			try {
				return method.invoke(client, args);
			} catch (Exception e) {
				this.rebuildClient();
				if (e.getCause() != null) {
					if (e.getCause().getCause() instanceof SocketException
									|| e.getCause().getMessage().equals("Cannot write to null outputStream")
									) // connection problem that can be restore, then re-invoke
						return method.invoke(client, args);
				}
				throw e;
			}
		}

		public boolean isIdleTimeout() {
			return System.currentTimeMillis() - lastInvokeTime > POOL_IDLE_OBJ_TIMEOUT;
		}
	}

	private class PoolObjMaker extends BasePooledObjectFactory<ThriftClient> {

		@Override
		public ThriftClient create() throws Exception {
			return new ThriftClient();
		}

		@Override
		public PooledObject<ThriftClient> wrap(ThriftClient obj) {
			return new DefaultPooledObject<>(obj);
		}

		@Override
		public boolean validateObject(PooledObject<ThriftClient> p) {
			return !p.getObject().isIdleTimeout();
		}

		@Override
		public void destroyObject(PooledObject<ThriftClient> p) throws Exception {
			p.getObject().close();
		}
	}

	public static class Config<T> {

		private Class<T> iface;
		private Class<? extends T> tclient;
		private String serverHost;
		private int serverPort;
		private int clientSocketTimeout;

		private boolean useTLS;
		private Resource trustKeyStore;
		private String trustKeyStorePw;
		private boolean isRequireClientAuth;
		private Resource selfKeyStore;
		private String selfKeyStorePw;

		/**
		 * thrift server interface.
		 */
		public void setIface(Class<T> iface) {
			this.iface = iface;
		}

		/**
		 * thrift client class.
		 */
		public void setTclient(Class<? extends T> tclient) {
			this.tclient = tclient;
		}

		/**
		 * server host.
		 */
		public void setServerHost(String serverHost) {
			this.serverHost = serverHost;
		}

		/**
		 * server port.
		 */
		public void setServerPort(int serverPort) {
			this.serverPort = serverPort;
		}

		/**
		 * connect to server and wait for server response timeout. default is 0(never time out).
		 */
		public void setClientSocketTimeout(int clientSocketTimeout) {
			this.clientSocketTimeout = clientSocketTimeout;
		}

		/**
		 * use TLS connection.
		 */
		public void setUseTLS(boolean useTLS) {
			this.useTLS = useTLS;
		}

		/**
		 * CA key store.
		 */
		public void setTrustKeyStore(Resource trustKeyStore) {
			this.trustKeyStore = trustKeyStore;
		}

		/**
		 * CA key store password.
		 */
		public void setTrustKeyStorePw(String trustKeyStorePw) {
			this.trustKeyStorePw = trustKeyStorePw;
		}

		/**
		 * need client connection auth.
		 */
		public void setRequireClientAuth(boolean isRequireClientAuth) {
			this.isRequireClientAuth = isRequireClientAuth;
		}

		/**
		 * client key store.
		 */
		public void setSelfKeyStore(Resource selfKeyStore) {
			this.selfKeyStore = selfKeyStore;
		}

		/**
		 * client key store password.
		 */
		public void setSelfKeyStorePw(String selfKeyStorePw) {
			this.selfKeyStorePw = selfKeyStorePw;
		}
	}

	private static long POOL_IDLE_OBJ_TIMEOUT = 61000;
	private Config<T> conf;

	public ThriftClientFactory(Config<T> conf) {
		this.conf = conf;
	}

	@SuppressWarnings("unchecked")
	public T build() {
		GenericObjectPoolConfig poolConf = new GenericObjectPoolConfig();
		poolConf.setMinIdle(0);
		poolConf.setMaxTotal(Integer.MAX_VALUE);
		poolConf.setMaxWaitMillis(conf.clientSocketTimeout);
		poolConf.setTimeBetweenEvictionRunsMillis(POOL_IDLE_OBJ_TIMEOUT);
		poolConf.setTestWhileIdle(true);

		GenericObjectPool<ThriftClient> pool = new GenericObjectPool(new PoolObjMaker(), poolConf);

		return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{conf.iface},
						(obj, method, args) -> {
							ThriftClient tc = pool.borrowObject();
							try {
								return tc.invokeThriftClient(method, args);
							} finally {
								pool.returnObject(tc);
							}
						});
	}

}
