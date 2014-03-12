package mysh.thrift;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Thrift 可重用的客户端对象工厂。
 * 通过对象池来重用客户端连接，而不是每次调用都重新建立连接。
 *
 * @param <T> 要构建的客户端对象接口。
 * @author 张智贤
 */
public final class ThriftReusableClientFactory<T> implements IClientFactory<T> {
	private static final Logger log = LoggerFactory.getLogger(ThriftReusableClientFactory.class);
	/**
	 * 连接池配置。
	 */
	private final GenericObjectPool.Config config = new GenericObjectPool.Config();
	/**
	 * 要构建的客户端对象接口。
	 */
	private Class<? extends T> iface;
	/**
	 * 连接池对象工厂。
	 */
	private PoolableObjectFactory<T> poolObjFactory;

	{
		this.setMinPoolCap(1);
		this.setMaxPoolCap(5);
		this.setEvictIntervalMilli(10 * 60000);
		this.config.timeBetweenEvictionRunsMillis = 5000;
		this.config.testOnBorrow = true;
		this.config.testOnReturn = true;
		this.config.testWhileIdle = true;
	}

	/**
	 * 根据配置构建对象。
	 */
	@SuppressWarnings("unchecked")
	public T build() {

		if (this.iface == null || !this.iface.isInterface() || this.poolObjFactory == null) {
			throw new IllegalArgumentException("构建失败，请检查配置参数\n");
		}

		try {
			return (T) Proxy.newProxyInstance(
							ThriftReusableClientFactory.this.getClass().getClassLoader(),
							new Class[]{this.iface},
							new InvocationHandler() {

								final ObjectPool<T> pool = new GenericObjectPool<T>(poolObjFactory, config);

								@Override
								public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
									T obj = this.pool.borrowObject();
									try {
										return method.invoke(obj, args);
									} finally {
										this.pool.returnObject(obj);
									}
								}
							}
			);
		} catch (Exception e) {
			log.error("创建代理对象失败。", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * 要构建的客户端对象接口。
	 */
	public void setIface(Class<? extends T> iface) {
		this.iface = iface;
	}

	/**
	 * 连接池对象工厂。
	 */
	public PoolableObjectFactory<T> getPoolObjFactory() {
		return poolObjFactory;
	}

	/**
	 * 连接池对象工厂。
	 */
	@Override
	public void setPoolObjFactory(PoolableObjectFactory<T> poolObjFactory) {
		this.poolObjFactory = poolObjFactory;
	}

	/**
	 * 回收检查间隔时间。
	 */
	public void setEvictIntervalMilli(int interval) {
		this.config.minEvictableIdleTimeMillis = interval;
	}

	/**
	 * 最小对象池容量。
	 */
	public void setMinPoolCap(int minPoolCap) {
		this.config.minIdle = minPoolCap;
	}

	/**
	 * 最大对象池容量。
	 */
	public void setMaxPoolCap(int maxPoolCap) {
		this.config.maxActive = maxPoolCap;
		this.config.maxIdle = maxPoolCap;
	}
}
