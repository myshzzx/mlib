
package mysh.db;

import mysh.util.PropConf;

/**
 * 数据库连接池配置.
 *
 * @author ZhangZhx
 */
public class DBConf {

	private String driverClassName;

	private String url;

	private int poolSize;

	private int sqlQueueSize;

	/**
	 * 根据默认的属性生成此实例.<br/>
	 * 默认属性示例如下:<br/>
	 * <p>
	 * <pre>
	 *
	 * #数据库驱动
	 * dbpool.driverClassName=com.mysql.jdbc.Driver
	 *
	 * #数据库连接串
	 * dbpool.url=jdbc:mysql://localhost/mycrawler?user=root&password=myshzzx
	 *
	 * #数据库连接池大小
	 * dbpool.poolSize=6
	 *
	 * #非阻塞 sql 执行器队列
	 * dbpool.sqlQueueSize=1000
	 * </pre>
	 */
	public static DBConf getDefaultConfig(PropConf conf) {

		DBConf dbConf = new DBConf();

		dbConf.setDriverClassName(conf.getPropString("dbpool.driverClassName"));
		dbConf.setPoolSize(conf.getPropInt("dbpool.poolSize"));
		dbConf.setSqlQueueSize(conf.getPropInt("dbpool.sqlQueueSize"));
		dbConf.setUrl(conf.getPropString("dbpool.url"));

		return dbConf;

	}

	/**
	 * @return the driverClassName
	 */
	public String getDriverClassName() {

		return driverClassName;
	}

	/**
	 * @param driverClassName the driverClassName to set
	 */
	public void setDriverClassName(String driverClassName) {

		this.driverClassName = driverClassName;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {

		return url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {

		this.url = url;
	}

	/**
	 * @return the poolSize
	 */
	public int getPoolSize() {

		return poolSize;
	}

	/**
	 * @param poolSize the poolSize to set
	 */
	public void setPoolSize(int poolSize) {

		this.poolSize = poolSize;
	}

	/**
	 * @return the sqlQueueSize
	 */
	public int getSqlQueueSize() {

		return sqlQueueSize;
	}

	/**
	 * @param sqlQueueSize the sqlQueueSize to set
	 */
	public void setSqlQueueSize(int sqlQueueSize) {

		this.sqlQueueSize = sqlQueueSize;
	}

}
