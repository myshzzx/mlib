package mysh.sql;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * @author mysh
 * @since 2021-05-16
 */
public class PooledDataSource implements DataSource, Closeable, AutoCloseable {
	private GenericObjectPool<Connection> pool;
	private DataSource ds;
	
	private interface PooledConnection {
		void destroy() throws SQLException;
		
		void renew() throws SQLException;
	}
	
	public PooledDataSource(GenericObjectPoolConfig<Connection> poolConfig, DataSource dataSource) {
		ds = dataSource;
		pool = new GenericObjectPool<>(new BasePooledObjectFactory<Connection>() {
			@Override
			public void destroyObject(PooledObject<Connection> p, DestroyMode mode) throws Exception {
				((PooledConnection) p.getObject()).destroy();
			}
			
			@Override
			public Connection create() throws Exception {
				return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Connection.class, PooledConnection.class},
						new InvocationHandler() {
							boolean isClosed;
							Method isClosedMethod = Connection.class.getDeclaredMethod("isClosed");
							Connection physicalConn = ds.getConnection();
							
							public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
								try {
									String name = method.getName();
									switch (name) {
										case "close":
											if (!physicalConn.getAutoCommit()) {
												physicalConn.rollback();
												physicalConn.setAutoCommit(true);
											}
											isClosed = true;
											pool.returnObject((Connection) proxy);
											return null;
										case "isClosed":
											if (!isClosed)
												isClosed = ((Boolean) method.invoke(physicalConn, args)).booleanValue();
											return isClosed;
										case "destroy":
											isClosed = true;
											physicalConn.close();
											return null;
										case "renew":
											isClosed = ((Boolean) isClosedMethod.invoke(physicalConn, args)).booleanValue();
											if (isClosed) {
												throw new SQLException("Connection is closed");
											}
											return null;
									}
									
									if (isClosed) {
										throw new SQLException("Connection is closed");
									}
									
									return method.invoke(physicalConn, args);
								} catch (SQLException e) {
									throw e;
								} catch (InvocationTargetException ex) {
									throw ex.getCause();
								}
							}
						});
			}
			
			@Override
			public PooledObject<Connection> wrap(Connection connection) {
				return new DefaultPooledObject<>(connection);
			}
			
			@Override
			public void activateObject(PooledObject<Connection> p) throws Exception {
				((PooledConnection) p.getObject()).renew();
			}
		}, poolConfig);
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		try {
			return pool.borrowObject();
		} catch (Exception e) {
			throw new SQLException("borrow connection from pool failed", e);
		}
	}
	
	@Override
	public void close() throws IOException {
		pool.close();
	}
	
	// 下面是代理
	
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return getConnection();
	}
	
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return ds.unwrap(iface);
	}
	
	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return ds.isWrapperFor(iface);
	}
	
	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return ds.getLogWriter();
	}
	
	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		ds.setLogWriter(out);
	}
	
	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		ds.setLoginTimeout(seconds);
	}
	
	@Override
	public int getLoginTimeout() throws SQLException {
		return ds.getLoginTimeout();
	}
	
	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return ds.getParentLogger();
	}
}
