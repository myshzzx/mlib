package mysh.sql.sqlite;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.google.common.base.Joiner;
import lombok.Getter;
import mysh.collect.Colls;
import mysh.os.Oss;
import mysh.util.Compresses;
import mysh.util.Range;
import mysh.util.Serializer;
import mysh.util.Tick;
import mysh.util.Times;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.sqlite.SQLiteConfig;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @since 2019-07-08
 */
public class SqliteDB implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(SqliteDB.class);
	
	private static final Serializer SERIALIZER = Serializer.BUILD_IN;
	
	private DataSource ds;
	@Getter
	private NamedParameterJdbcTemplate jdbcTemplate;
	private Set<String> existTables = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	public static class Item {
		private String key;
		private LocalDateTime writeTime, readTime;
		private Object value;
		
		public String getKey() {
			return key;
		}
		
		public LocalDateTime getWriteTime() {
			return writeTime;
		}
		
		public LocalDateTime getReadTime() {
			return readTime;
		}
		
		public <V> V getValue() {
			return (V) value;
		}
	}
	
	public interface DAO {
		String getTableName();
		
		boolean tableExists();
		
		int update(String namedSql, Map<String, ?> params);
		
		int insertOrReplace(Map<String, Object> tableColValues);
		
		List<Map<String, Object>> byRawSql(
				@Nullable String cols, @Nullable String conditions, @Nullable String clauses, @Nullable Map<String, ?> params);
	}
	
	public interface KvDAO extends DAO {
		boolean containsKey(String key);
		
		<V> V byKey(String key);
		
		<V> V byKey(String key, V defaultValue);
		
		List<Item> itemsByRawSql(
				@Nullable String cols, @Nullable String conditions, @Nullable String clauses, @Nullable Map<String, ?> params);
		
		Item infoByKey(String key, boolean queryValue, boolean queryTime);
		
		Item timeByKey(String key);
		
		default Item infoByKey(String key) {
			return infoByKey(key, true, true);
		}
		
		<V> V byKeyRemoveOnWriteExpired(String key, LocalDateTime validAfter);
		
		void removeReadExpired(LocalDateTime validAfter);
		
		void remove(String key);
		
		void save(String key, Object value);
		
		/**
		 * @param compressionLevel valid within [0,9], the higher level, the harder compress.
		 *                         a valid level will force compress.
		 */
		void save(String key, Object value, int compressionLevel);
		
		boolean exists(String key);
	}
	
	public SqliteDB(Path file) {
		this(file, false);
	}
	
	/**
	 * @see #newDataSource
	 */
	public SqliteDB(Path file, boolean useLock) {
		this(file, useLock, 0);
	}
	
	/**
	 * @see #newDataSource
	 */
	public SqliteDB(Path file, boolean useLock, int mmapSize) {
		ds = newDataSource(file, useLock, Math.max(0, mmapSize));
		jdbcTemplate = new NamedParameterJdbcTemplate(ds);
		
		// if (useLock)
		// 	jdbcTemplate.queryForList("PRAGMA locking_mode=EXCLUSIVE", Collections.emptyMap());
		// if (mmapSize > 0)
		// 	jdbcTemplate.queryForList("PRAGMA mmap_size=" + mmapSize, Collections.emptyMap());
	}
	
	public SqliteDB(DataSource ds) {
		this.ds = ds;
		jdbcTemplate = new NamedParameterJdbcTemplate(ds);
	}
	
	/**
	 * PRAGMA 优化配置见: <a href='https://www.cnblogs.com/cchust/p/4754619.html'>日志模式</a>
	 * 和 <a href='https://www.runoob.com/sqlite/sqlite-pragma.html'>pragma 说明</a>
	 *
	 * @param useLock  use lock to gain 10 times speed up IO performance, but this block file access from other processes.
	 * @param mmapSize Memory-Mapped file size(byte), 0 to disable. usually 268435456 or more. <a href='https://cloud.tencent.com/developer/section/1420023'>see more</a'>
	 */
	public static DataSource newDataSource(Path file, boolean useLock, int mmapSize) {
		if (!file.toFile().getParentFile().exists())
			file.toFile().getParentFile().mkdirs();
		
		String url = String.format("jdbc:sqlite:%s?locking_mode=%s&mmap_size=%d",
				file.toString(), useLock ? "EXCLUSIVE" : "NORMAL", mmapSize);
		if (Oss.isAndroid()) {
			SQLiteConnectionPoolDataSource ds = new SQLiteConnectionPoolDataSource();
			ds.setUrl(url);
			ds.setPageSize(4096); //in bytes
			ds.setSynchronous(SQLiteConfig.SynchronousMode.OFF.getValue());
			ds.setJournalMode(SQLiteConfig.JournalMode.OFF.getValue());
			return ds;
		} else {
			DruidDataSource ds = new DruidDataSource(true);
			ds.setUrl(url);
			ds.setMaxActive(10);
			ds.setTestWhileIdle(false);
			ds.setTestOnBorrow(true);
			ds.setTestOnReturn(false);
			ds.setValidationQuery("select 1");
			try (DruidPooledConnection conn = ds.getConnection()) {
				try (Statement stat = conn.createStatement()) {
					stat.execute("PRAGMA synchronous = OFF");
					stat.execute("PRAGMA journal_mode = OFF");
				}
			} catch (Exception e) {
				log.error("init-sqlite-db-fail", e);
			}
			
			return ds;
		}
	}
	
	@Override
	public void close() {
		if (ds instanceof Closeable) {
			try {
				((Closeable) ds).close();
			} catch (IOException e) {
				log.error("close-DS-fail", e);
			}
		}
	}
	
	private class DAOImpl implements KvDAO {
		final String table;
		final boolean suggestCompressValue;
		final boolean updateReadTime;
		
		/**
		 * {@link DAO} implementation
		 *
		 * @param table snake_case_group
		 */
		DAOImpl(String table) {
			this.table = table;
			this.suggestCompressValue = false;
			this.updateReadTime = false;
		}
		
		/**
		 * {@link KvDAO} implementation
		 *
		 * @param table                snake_case_group
		 * @param suggestCompressValue value may be zip compress before save, depends on reducing size or not
		 * @param updateReadTime       update rt on each read
		 */
		DAOImpl(String table, boolean suggestCompressValue, boolean updateReadTime) {
			this.table = table;
			this.suggestCompressValue = suggestCompressValue;
			this.updateReadTime = updateReadTime;
			this.ensureKvTable();
		}
		
		public boolean tableExists() {
			if (existTables.contains(table))
				return true;
			Integer tableCount = jdbcTemplate.queryForObject(
					"select count(1) from sqlite_master where type='table' and tbl_name=:name",
					Colls.ofHashMap("name", table), Integer.class
			);
			if (tableCount != null && tableCount > 0) {
				existTables.add(table);
				return true;
			} else
				return false;
		}
		
		private void ensureKvTable() {
			if (!tableExists()) {
				synchronized (table.intern()) {
					if (tableExists())
						return;
					
					String sql = "CREATE TABLE " + table +
							"(\n" +
							"k text not null constraint " + table + "_pk primary key,\n" +
							"v blob,\n" +
							"wt datetime default (datetime(CURRENT_TIMESTAMP,'localtime')),\n" +
							"rt datetime default (datetime(CURRENT_TIMESTAMP,'localtime'))\n" +
							")";
					jdbcTemplate.update(sql, Collections.emptyMap());
					existTables.add(table);
				}
			}
		}
		
		@Override
		public String getTableName() {
			return this.table;
		}
		
		@Override
		public int update(final String namedSql, final Map<String, ?> params) {
			return jdbcTemplate.update(namedSql, params);
		}
		
		@Override
		public int insertOrReplace(Map<String, Object> tableColValues) {
			Set<String> cols = tableColValues.keySet();
			Joiner cj = Joiner.on(",");
			Joiner vj = Joiner.on(",:");
			return jdbcTemplate.update(
					"insert or replace into " + table
							+ "(" + cj.join(cols) + ") values(:" + vj.join(cols) + ")",
					tableColValues
			);
		}
		
		@Override
		public List<Map<String, Object>> byRawSql(String cols, String conditions, String clauses, Map<String, ?> params) {
			String sql = "select " + ObjectUtils.firstNonNull(cols, "*") + " from " + table
					+ (conditions != null ? (" where " + conditions + " ") : " ")
					+ ObjectUtils.firstNonNull(clauses, "");
			params = params == null ? Collections.emptyMap() : params;
			Tick tick = Tick.tick();
			List<Map<String, Object>> lst = jdbcTemplate.queryForList(sql, params);
			if (tick.nip() > 30) {
				log.warn("sql-need-optimize,cost={}ms, sql={}, params={}", tick.lastNip(), sql, params);
			}
			return lst == null ? Collections.emptyList() : lst;
		}
		
		
		@Override
		public boolean containsKey(String key) {
			Integer count = jdbcTemplate.queryForObject(
					"select count(1) from " + table + " where k=:key",
					Colls.ofHashMap("key", key), Integer.class);
			return count > 0;
		}
		
		@Override
		public <V> V byKey(String key) {
			return byKey(key, null);
		}
		
		@Override
		public <V> V byKey(String key, V defaultValue) {
			Item item = infoByKey(key, true, false);
			return item == null ? defaultValue : item.getValue();
		}
		
		@Override
		public Item timeByKey(String key) {
			return infoByKey(key, false, true);
		}
		
		@Override
		public List<Item> itemsByRawSql(String cols, String conditions, String clauses, Map<String, ?> params) {
			List<Map<String, Object>> lst = this.byRawSql(cols, conditions, clauses, params);
			
			List<Item> items = new ArrayList<>();
			for (Map<String, Object> mi : lst) {
				Item item = new Item();
				item.key = (String) mi.get("k");
				this.assembleItem(mi, item, true, true);
				items.add(item);
			}
			return items;
		}
		
		@Override
		public Item infoByKey(String key, boolean queryValue, boolean queryTime) {
			List<Map<String, Object>> lst = jdbcTemplate.queryForList(
					"select " + (queryValue ? "v," : "") + (queryTime ? "wt,rt," : "") + "1 from " + table + " where k=:key",
					Colls.ofHashMap("key", key));
			if (Colls.isEmpty(lst))
				return null;
			else {
				Map<String, Object> r = lst.get(0);
				Item item = new Item();
				item.key = key;
				
				assembleItem(r, item, queryValue, queryTime);
				
				if (updateReadTime) {
					jdbcTemplate.update(
							"update " + table + " set rt=:now where k=:key",
							Colls.ofHashMap("key", key, "now", Times.formatNow(Times.Formats.DayTime)));
				}
				return item;
			}
		}
		
		private void assembleItem(Map<String, Object> r, Item item, boolean queryValue, boolean queryTime) {
			Object v = r.get("v");
			if (queryValue && v != null) {
				if (v instanceof String) {
					item.value = v;
				} else {
					byte[] blob = (byte[]) v;
					if (Compresses.isZip(blob)) {
						item.value = SERIALIZER.deserialize(Compresses.decompressZip(blob));
					} else if (Compresses.isXz(blob)) {
						item.value = SERIALIZER.deserialize(Compresses.decompressXz(blob));
					} else if (Serializer.isBuildInStream(blob))
						item.value = SERIALIZER.deserialize(blob);
					else
						item.value = new String(blob);
				}
			}
			
			if (queryTime) {
				String wt = (String) r.get("wt");
				if (wt != null)
					item.writeTime = Times.parseDayTime(Times.Formats.DayTime, wt)
					                      .atZone(ZoneId.systemDefault()).toLocalDateTime();
				
				String rt = (String) r.get("rt");
				if (rt != null)
					item.readTime = Times.parseDayTime(Times.Formats.DayTime, rt)
					                     .atZone(ZoneId.systemDefault()).toLocalDateTime();
			}
		}
		
		@Override
		public <V> V byKeyRemoveOnWriteExpired(String key, LocalDateTime validAfter) {
			Item item = infoByKey(key, true, true);
			if (item != null) {
				if (item.writeTime.compareTo(validAfter) < 0) {
					remove(key);
					return null;
				} else
					return item.getValue();
			} else
				return null;
		}
		
		@Override
		public void removeReadExpired(LocalDateTime validAfter) {
			jdbcTemplate.update(
					"delete from " + table + " where rt<:va",
					Colls.ofHashMap("va", Times.format(Times.Formats.DayTime, validAfter)));
		}
		
		@Override
		public void remove(String key) {
			jdbcTemplate.update(
					"delete from " + table + " where k=:key",
					Colls.ofHashMap("key", key));
		}
		
		@Override
		public void save(String key, Object value) {
			save(key, value, -1);
		}
		
		@Override
		public void save(String key, Object value, int compressionLevel) {
			byte[] buf = SERIALIZER.serialize(value);
			if ((suggestCompressValue || Range.isWithin(0, 9, compressionLevel)) && buf.length > 256) {
				byte[] cb = Range.isWithin(0, 9, compressionLevel) ?
						Compresses.compressXz(buf, compressionLevel) : Compresses.compressXz(buf);
				if (cb.length < buf.length)
					buf = cb;
			}
			// wt and rt will always be the default value
			jdbcTemplate.update(
					"insert or replace into " + table + "(k,v) values(:key,:value)",
					Colls.ofHashMap("key", key, "value", buf)
			);
		}
		
		@Override
		public boolean exists(String key) {
			return jdbcTemplate.queryForObject("select count(1) from " + table + " where k=:key",
					Colls.ofHashMap("key", key), Integer.class) > 0;
		}
	}
	
	/**
	 * @param table snake_case_group style
	 */
	public DAO genDAO(String table) {
		return new DAOImpl(table);
	}
	
	/**
	 * @param table snake_case_group style
	 */
	public KvDAO genKvDAO(String table, boolean suggestCompressValue, boolean updateReadTime) {
		return new DAOImpl(table, suggestCompressValue, updateReadTime);
	}
	
	/**
	 * @see <a href='http://www.sqlitetutorial.net/sqlite-vacuum/'>clear and rebuild db</a>
	 */
	public void maintain() {
		jdbcTemplate.update("VACUUM", Collections.emptyMap());
	}
}
