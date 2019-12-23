package mysh.sql.sqlite;

import com.alibaba.druid.pool.DruidDataSource;

import java.nio.file.Path;

/**
 * @since 2019-12-23
 */
public abstract class SqliteUtil {
	public static DruidDataSource newDataSource(Path file, boolean useLock) {
		DruidDataSource ds = new DruidDataSource(true);
		ds.setUrl("jdbc:sqlite:" + file.toString() + (useLock ? "?locking_mode=EXCLUSIVE" : ""));
		ds.setMaxActive(10);
		ds.setTestWhileIdle(false);
		ds.setTestOnBorrow(true);
		ds.setTestOnReturn(false);
		ds.setValidationQuery("select 1");
		return ds;
	}
}
