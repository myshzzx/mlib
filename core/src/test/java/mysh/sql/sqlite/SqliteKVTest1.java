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
public class SqliteKVTest1 {
	
	SqliteKV kv = new SqliteKV(Paths.get("l:/a.db"));
	SqliteKV.DAO dao = kv.genDAO("test_group", true, false);
	
	@AfterEach
	public void end() {
		kv.close();
	}
	
	@Test
	public void query() {
		System.out.println(dao.infoByKey("k1"));
		System.out.println(dao.infoByKey("k2"));
	}
	
	@Test
	public void save() {
		dao.save("k1", "myshzzx");
		System.out.println(dao.infoByKey("k1"));
	}
	
	@Test
	public void expire() {
		String v = dao.byKeyRemoveOnWriteExpired("k1", Times.parseDay(Times.Formats.Day, "2019-07-07").atStartOfDay());
		System.out.println(v);
		
		v = dao.byKeyRemoveOnWriteExpired("k1", Times.parseDay(Times.Formats.Day, "2019-07-10").atStartOfDay());
		System.out.println(v);
	}
}
