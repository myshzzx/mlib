package mysh.thrift;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.TConfiguration;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.layered.TFramedTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.SocketException;

/**
 * a connection-pooled, TLS-supported and thread-safe thrift-client maker.<br/>
 * DO NOT support async mode, because async mode focus on thread-reuse(for large scale c/s),
 * but this implementation focus on connection-reuse(for small scale and heavy communication).
 *
 * @param <TI> thrift server interface.
 */
public class ThriftClientFactory<TI> {
	private static final Logger log = LoggerFactory.getLogger(ThriftClientFactory.class);

	private class ThriftClient implements Closeable {
		private volatile boolean isClosed = false;
		private volatile TI client;
		private volatile TTransport transport;

		public ThriftClient() throws Exception {
			rebuildClient();
		}

		@SuppressWarnings({"ConstantConditions"})
		private void rebuildClient() throws TTransportException, IOException, NoSuchMethodException,
				IllegalAccessException, InvocationTargetException, InstantiationException {

			if (isClosed) {
				throw new RuntimeException("thrift client has been closed.");
			}

			if (transport != null) {
				transport.close();
			}

			if (conf.useTLS) {
				TSSLTransportFactory.TSSLTransportParameters transportParams = new TSSLTransportFactory.TSSLTransportParameters();
				transportParams.setTrustStore(
						ThriftClientFactory.class.getClassLoader().getResource(conf.trustKeyStore).getPath(),
						conf.trustKeyStorePw);
				if (conf.isRequireClientAuth) {
					transportParams.setKeyStore(
							ThriftClientFactory.class.getClassLoader().getResource(conf.selfKeyStore).getPath(),
							conf.selfKeyStorePw);
				}
				transport = TSSLTransportFactory.getClientSocket(
						conf.serverHost, conf.serverPort, conf.clientSocketTimeout, transportParams);
			} else {
				transport = new TFramedTransport(
						new TSocket(new TConfiguration(),
								conf.serverHost, conf.serverPort, conf.clientSocketTimeout),
						conf.nonTLSServerMaxFrameSize
				);
				transport.open();
			}

			TProtocol protocol = new TCompactProtocol(transport);
			client = conf.tClientClass.getConstructor(TProtocol.class).newInstance(protocol);
		}

		@Override
		public void close() {
			this.isClosed = true;
			if (transport != null) {
				transport.close();
			}
		}

		private volatile long lastInvokeTime = System.currentTimeMillis();

		public Object invokeThriftClient(Method method, Object[] args) throws Throwable {
			lastInvokeTime = System.currentTimeMillis();

			if (isClosed) {
				throw new RuntimeException("thrift client has been closed.");
			}

			try {
				Object result = method.invoke(client, args);
				return result;
			} catch (Throwable e) {
				if (e.getCause() instanceof TException && !e.getCause().getClass().getName().contains("org.apache.thrift")) {
					// business exp
					throw e.getCause();
				} else {
					// sys exp
					this.rebuildClient();

					if (e.getCause() != null) {
						if (e.getCause().getCause() instanceof SocketException
								|| "Cannot write to null outputStream".equals(e.getCause().getMessage())
						) { // connection problem that can be restore, then re-invoke
							log.debug("thrift client invoke failed, but has reconnected and prepared for re-invoking", e);
							return method.invoke(client, args);
						}
					}
					throw e;
				}
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

		private String serverHost;
		private int serverPort;
		private int clientSocketTimeout;
		private int maxPoolSize=5;
		private int maxWaitMillis =-1;
		private boolean useAsync;
		private Class<T> iface;
		private Class<? extends T> tClientClass;
		private int nonTLSServerMaxFrameSize = 16384000;

		private boolean useTLS;
		private String trustKeyStore;
		private String trustKeyStorePw;
		private boolean isRequireClientAuth;
		private String selfKeyStore;
		private String selfKeyStorePw;

		/**
		 * thrift server interface.
		 */
		public Config<T> setIface(Class<T> iface) {
			this.iface = iface;
			return this;
		}

		/**
		 * thrift client class.
		 */
		public Config<T> setTClientClass(Class<? extends T> tClientClass) {
			this.tClientClass = tClientClass;
			return this;
		}

		/**
		 * server host.
		 */
		public Config<T> setServerHost(String serverHost) {
			this.serverHost = serverHost;
			return this;
		}

		/**
		 * server port.
		 */
		public Config<T> setServerPort(int serverPort) {
			this.serverPort = serverPort;
			return this;
		}

		/**
		 * connect to server and wait for server response timeout. default is 0(never time out).
		 */
		public Config<T> setClientSocketTimeout(int clientSocketTimeout) {
			this.clientSocketTimeout = clientSocketTimeout;
			return this;
		}

		/**
		 * connection pool max size. default is 5.
		 */
		public Config<T> setMaxPoolSize(int maxPoolSize) {
			this.maxPoolSize = maxPoolSize;
			return this;
		}

		/**
		 * max wait time for getting a client from pool. less than 0 means block until one available
		 */
		public Config<T> setMaxWaitMillis(int maxWaitMillis) {
			this.maxWaitMillis = maxWaitMillis;
			return this;
		}

		/**
		 * if not use TLS, the server is a non-blocking server, which use TFramedTransport,
		 * each invoke will be encapsulated to a frame,
		 * and there is a "max frame size" limit (default 16384000), if the frame size exceed, invoking will fail.
		 */
		public Config<T> setNonTLSServerMaxFrameSize(int nonTLSServerMaxFrameSize) {
			if (nonTLSServerMaxFrameSize > 0) {
				this.nonTLSServerMaxFrameSize = nonTLSServerMaxFrameSize;
			}
			return this;
		}

		/**
		 * use TLS connection.
		 */
		public Config<T> setUseTLS(boolean useTLS) {
			this.useTLS = useTLS;
			return this;
		}

		/**
		 * CA key store.
		 */
		public Config<T> setTrustKeyStore(String trustKeyStore) {
			this.trustKeyStore = trustKeyStore;
			return this;
		}

		/**
		 * CA key store password.
		 */
		public Config<T> setTrustKeyStorePw(String trustKeyStorePw) {
			this.trustKeyStorePw = trustKeyStorePw;
			return this;
		}

		/**
		 * need client connection auth.
		 */
		public Config<T> setRequireClientAuth(boolean isRequireClientAuth) {
			this.isRequireClientAuth = isRequireClientAuth;
			return this;
		}

		/**
		 * client key store.
		 */
		public Config<T> setSelfKeyStore(String selfKeyStore) {
			this.selfKeyStore = selfKeyStore;
			return this;
		}

		/**
		 * client key store password.
		 */
		public Config<T> setSelfKeyStorePw(String selfKeyStorePw) {
			this.selfKeyStorePw = selfKeyStorePw;
			return this;
		}
	}

	/**
	 * closeable client encapsulation.
	 */
	public static class ClientHolder<T> implements Closeable {
		private T client;
		private Closeable c;

		private ClientHolder(T client, Closeable c) {
			this.client = client;
			this.c = c;
		}

		public T getClient() {
			return client;
		}

		@Override
		public void close() throws IOException {
			if (c != null) {
				c.close();
			}
		}
	}

	private static long POOL_IDLE_OBJ_TIMEOUT = 61000;
	private Config<TI> conf;

	public ThriftClientFactory(Config<TI> conf) {
		this.conf = conf;
	}

	/**
	 * build pooled(auto close connections) and thread-safe (unblocking) client.
	 * <br/>
	 * WARNING: the holder needs to be closed after using.
	 */
	@SuppressWarnings("unchecked")
	public ClientHolder<TI> buildPooled() {
		GenericObjectPoolConfig poolConf = new GenericObjectPoolConfig();
		poolConf.setMinIdle(0);
		poolConf.setMaxTotal(conf.maxPoolSize);
		poolConf.setMaxWaitMillis(conf.maxWaitMillis);
		poolConf.setTimeBetweenEvictionRunsMillis(POOL_IDLE_OBJ_TIMEOUT);
		poolConf.setTestWhileIdle(true);

		GenericObjectPool<ThriftClient> pool = new GenericObjectPool(new PoolObjMaker(), poolConf);

		TI client = (TI) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{conf.iface},
				(obj, method, args) -> {
					ThriftClient tc = pool.borrowObject();
					try {
						return tc.invokeThriftClient(method, args);
					} finally {
						// sys exp will cause client rebuild, so client is reusable
						pool.returnObject(tc);
					}
				});
		return new ClientHolder<>(client, pool::close);
	}

}
