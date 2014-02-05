
package mysh.db.test;

import mysh.db.DBController;

public class DBTest {

	public static void main(String[] args) {

		String className = "";
		String url = "";
		int poolSize = 6;
		int sqlQueueSize = 3000;

		DBController dbPool = new DBController(className, url, poolSize, sqlQueueSize);
		dbPool.putSQL2Exec("insert into eum5(trade_time) value(1)");
		dbPool.stopSQLExecutorNow();
	}
}
