
package mysh.db;

import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据库连接数控制器. <br/>
 * 控制同时连接数据库的数量, 关闭连接时执行物理关闭.<br/>
 * 可手动请求数据库连接, 手动执行语句 (阻塞);<br/>
 * 也可将sql 语句加入执行队列 (非阻塞).
 *
 * @author ZhangZhx
 */
@ThreadSafe
public final class DBController {
	private static final Logger log = LoggerFactory.getLogger(DBController.class);

	/**
	 * sql 执行器. <br/>
	 * 执行 sqlQueue 中的语句, 永久占用一个数据库连接.
	 */
	private final class SQLExecutor extends Thread {

		private final BlockingQueue<String> sqlQueue;

		private Connection conn;

		/**
		 * 指明是否接受 sql 加入队列. 线程中断后将不接受sql 加入队列.
		 */
		private volatile boolean acceptSQL = true;

		public SQLExecutor(int sqlQueueSize) {

			super("SQLExecutor");
			// this.setDaemon(true);

			// 准备 sql 队列
			this.sqlQueue = new LinkedBlockingQueue<>(sqlQueueSize);
		}

		public void run() {

			Statement stmt = null;
			String sql = null;
			try {
				while (true) {
					try {
						this.prepareConnection();
						sql = this.sqlQueue.take();

						this.prepareConnection();

						if (this.conn == null || this.conn.isClosed()) {
							DBController.this.failToExecSQL(sql);
							continue;
						}

						stmt = this.conn.createStatement();
						stmt.execute(sql);

						log.info("SQL 执行成功: "
										+ sql.substring(0, sql.length() > 150 ? 150
										: sql.length()) + " ...");
						sql = null;
					} catch (InterruptedException e) {
						this.stopSQLQueueAndLog();
						Thread.currentThread().interrupt();
						break;
					} catch (SQLException e) {
						log.error("SQL 执行失败: "
										+ (sql.length() > 150 ? sql.substring(0,
										150) : sql), e);
					} catch (Exception e) {
						log.error("未知失败", e);
					} finally {
						if (sql != null) {
							DBController.this.failToExecSQL(sql);
							sql = null;
						}

						if (stmt != null) {
							try {
								stmt.close();
							} catch (SQLException e) {
							}
						}
					}
				}
			} finally {
				if (this.conn != null) {
					try {
						this.conn.close();
					} catch (SQLException e) {
					}
				}
			}
		}

		/**
		 * 准备数据库连接. 连接可能无法建立, this.conn 将为 null.
		 *
		 * @throws InterruptedException 线程中断.
		 */
		private void prepareConnection() throws InterruptedException {

			try {
				if (this.conn == null)
					this.conn = DBController.this.getConnection();
				else if (this.conn.isClosed()) {
					try {
						this.conn.close();
					} catch (Exception e) {
					}
					this.conn = null;
					this.prepareConnection();
				}
			} catch (SQLException e) {
				log.error("创建数据库连接失败", e);
				throw new InterruptedException();
			} catch (InterruptedException e) {
				this.stopSQLQueueAndLog();
				throw e;
			}
		}

		/**
		 * 将 sql 加入队列. 队列阻塞直到有可用空间.
		 */
		public void putSQL(String sql) {

			if (sql == null) {
				return;
			}

			if (this.acceptSQL) {
				try {
					this.sqlQueue.put(sql);
				} catch (IllegalArgumentException e) {
					DBController.this.failToExecSQL(sql);
				} catch (InterruptedException e2) {
					DBController.this.failToExecSQL(sql);
					Thread.currentThread().interrupt();
				}
			} else {
				DBController.this.failToExecSQL(sql);
			}
		}

		/**
		 * 阻止 sql 队列再接受数据, 并将未写入数据库的 sql 记录日志.
		 */
		private void stopSQLQueueAndLog() {

			this.acceptSQL = false;

			String sql;
			while ((sql = this.sqlQueue.poll()) != null)
				DBController.this.failToExecSQL(sql);
		}
	}

	/**
	 * 连接控制器信号量，控制并发访问数量.
	 */
	private final Semaphore poolGuide;

	/**
	 * 连接控制器数据源.
	 */
	private final DataSource ds;

	/**
	 * sql 执行器.
	 */
	private final SQLExecutor sqlExecutor;

	/**
	 * 保存未执行的 SQL. 在运行时始终占用 SQL 日志文件.
	 */
	private final PrintWriter sqlSaver;

	/**
	 * 初始化连接控制器.
	 */
	public DBController(DBConf conf) {

		this(conf.getDriverClassName(), conf.getUrl(), conf.getPoolSize(), conf
						.getSqlQueueSize());
	}

	/**
	 * 初始化.
	 *
	 * @param driverClassName 数据库驱动类名.
	 * @param url             数据库连接串.
	 * @param poolSize        连接控制器允许并发大小，允许的数量为 [2, proc*2].
	 */
	public DBController(String driverClassName, String url, int poolSize, int sqlQueueSize) {

		try {
			Class.forName(driverClassName);
		} catch (ClassNotFoundException e) {
			log.error("加载数据库驱动失败: " + e);
		}

		// 允许的并发数：并发数太小可能导致吞吐量下降，太大将使每个任务效率降低
		if (poolSize < 2 || poolSize > Runtime.getRuntime().availableProcessors() * 2) {
			// throw new IllegalArgumentException("pool size out of scope [2, "
			// + Runtime.getRuntime().availableProcessors() * 2 + "] : "
			// + poolSize);
			poolSize = Runtime.getRuntime().availableProcessors() + 1;
			log.info("给定连接控制器并发大小超出默认范围：[2, " + Runtime.getRuntime().availableProcessors()
							* 2 + "]！使用预设值：" + poolSize);
		}

		this.poolGuide = new Semaphore(poolSize);
		this.ds = this.initDataSource(url);

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File("unExecSQL.sql"), true));

		} catch (Exception e) {
			writer = null;
			log.error("SQL 保存文件访问失败.");
		} finally {
			this.sqlSaver = writer;
		}

		log.info("创建连接控制器：" + driverClassName + ": " + url + " [" + poolSize + "]");

		// 创建执行器
		this.sqlExecutor = new SQLExecutor(sqlQueueSize);
		this.sqlExecutor.start();
		log.info("sql 执行器已启动");

		// 注册终止钩子
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {

				DBController.this.stopSQLExecutorNow();
			}
		});
	}

	/**
	 * 取得数据库连接，方法将阻塞直到有可用连接. 方法响应中断请求。<br/>
	 * 若取数据库连接失败, 返回 null
	 */
	public Connection getConnection() throws InterruptedException {

		this.poolGuide.acquire();

		try {
			// 取得连接
			final Connection conn = this.ds.getConnection();

			// 返回代理对象，代理用于控制信号量
			return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
							new Class<?>[]{Connection.class}, new InvocationHandler() {

								private final AtomicBoolean isClosed = new AtomicBoolean(
												false);

								@Override
								public Object invoke(Object proxy, Method method,
								                     Object[] args) throws Throwable {

									Object result = null;
									try {
										result = method.invoke(conn, args);
									} finally {
										if (method.getName().equals("close")) {
											if (!this.isClosed.getAndSet(true)) {
												DBController.this.poolGuide.release();
											}
										}
									}

									return result;
								}
							});
		} catch (SQLException e) {
			this.poolGuide.release();
			return null;
		}

	}

	/**
	 * 初始化连接控制器.
	 *
	 * @param connectURL 连接串
	 */
	private DataSource initDataSource(String connectURL) {

		ConnectionFactory connFact = new DriverManagerConnectionFactory(connectURL, null);
		PoolableConnectionFactory fact = new PoolableConnectionFactory(connFact, null);
		ObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(fact);

		return new PoolingDataSource(connectionPool);
	}

	/**
	 * sql 字符串中单引号和反斜杠转义.
	 */
	public static String sqlStringEscape(String str) {

		return str == null ? "" : str.replace("\\", "\\\\").replace("'", "\\'");
	}

	/**
	 * 将要执行的语句放入 SQL 执行队列, 此方法立即返回.<br/>
	 * 不能保证写入成功的 sql 一定会被执行. 在大多数情况下, 不能执行的 sql 至少会被写入日志.
	 */
	public final void putSQL2Exec(String sql) {

		if (!this.sqlExecutor.isAlive() || this.sqlExecutor.isInterrupted()) {
			DBController.this.failToExecSQL(sql);
		}

		this.sqlExecutor.putSQL(sql);
	}

	/**
	 * 执行 SQL 失败后写日志, 并将未执行的 SQL 保存.<br/>
	 * 线程安全的写入, 无需同步.
	 */
	private void failToExecSQL(String sql) {

		// log.error("SQL 执行失败: " + (sql.length() > 150 ? sql.substring(0, 150) : sql));

		// 写入 sql 数据到文件
		if (this.sqlSaver != null) {
			this.sqlSaver.println(sql);
			this.sqlSaver.flush();
		}
	}

	/**
	 * 立刻停止 sql 执行器服务.
	 */
	public void stopSQLExecutorNow() {

		if (this.sqlExecutor.isAlive() && (!this.sqlExecutor.isInterrupted())) {
			log.info("正在关闭 SQL 执行器...");
			this.sqlExecutor.interrupt();
		}
	}

	/**
	 * 取当前可申请的连接数 ( 仅用于测试, 因为在并发环境下, 返回的值可能立刻失效 ).
	 */
	int getAvailableConnectionNum() {

		return this.poolGuide.availablePermits();
	}
}
