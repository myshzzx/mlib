
package mysh.db.test;

import mysh.db.DBControler;

public class DBTest {

	public static void main(String[] args) {

		String className = "";
		String url = "";
		int poolSize = 6;
		int sqlQueueSize = 3000;

		DBControler dbPool = new DBControler(className, url, poolSize, sqlQueueSize);
		dbPool.putSQL2Exec("insert into eum5(trade_time) value(1)");
		dbPool.stopSQLExecutorNow();
	}
}
