package mysh.sql.sqlite;

import mysh.util.Times;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.time.ZoneId;

/**
 * @since 2019-07-08
 */
@Ignore
public class SqliteKVTest1 {
	
	SqliteKV kv = new SqliteKV(Path.of("l:/a.db"));
	SqliteKV.DAO dao = kv.genDAO("test_group", true, false);
	
	@After
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
		String v = dao.byKeyRemoveWriteExpired("k1", Times.parseDay(Times.Formats.Day, "2019-07-07").atStartOfDay(ZoneId.systemDefault()).toInstant());
		System.out.println(v);
		
		v = dao.byKeyRemoveWriteExpired("k1", Times.parseDay(Times.Formats.Day, "2019-07-10").atStartOfDay(ZoneId.systemDefault()).toInstant());
		System.out.println(v);
	}
}
