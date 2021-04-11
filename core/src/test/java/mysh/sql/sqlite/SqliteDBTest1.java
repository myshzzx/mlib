package mysh.sql.sqlite;

import mysh.util.Times;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

/**
 * @since 2019-07-08
 */
@Disabled
public class SqliteDBTest1 {
	
	SqliteDB db = new SqliteDB(Paths.get("l:/a.db"));
	SqliteDB.KvDAO dao = db.genKvDAO("test_group", true, false);
	
	@AfterEach
	public void end() {
		db.close();
	}
	
	@Test
	public void query() {
		System.out.println(dao.itemByKey("k1"));
		System.out.println(dao.itemByKey("k2"));
	}
	
	@Test
	public void save() {
		dao.save("k1", "myshzzx");
		System.out.println(dao.itemByKey("k1"));
	}
	
	@Test
	public void expire() {
		String v = dao.byKeyRemoveOnWriteExpired("k1", Times.parseDay(Times.Formats.Day, "2019-07-07").atStartOfDay());
		System.out.println(v);
		
		v = dao.byKeyRemoveOnWriteExpired("k1", Times.parseDay(Times.Formats.Day, "2019-07-10").atStartOfDay());
		System.out.println(v);
	}
}
