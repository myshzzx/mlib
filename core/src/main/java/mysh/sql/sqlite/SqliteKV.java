package mysh.sql.sqlite;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.Getter;
import mysh.codegen.CodeUtil;
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
public class SqliteKV implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(SqliteKV.class);
	
	private static final Serializer SERIALIZER = Serializer.BUILD_IN;
	
	private DataSource ds;
	@Getter
	private NamedParameterJdbcTemplate jdbcTemplate;
	private Set<String> tableExist = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
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
		boolean containsKey(String key);
		
		<V> V byKey(String key);
		
		<V> V byKey(String key, V defaultValue);
		
		List<Item> itemsByRawSql(
				@Nullable String cols, @Nullable String conditions, @Nullable String clauses, @Nullable Map<String, ?> params);
		
		List<Map<String, Object>> byRawSql(
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
	
	public SqliteKV(Path file) {
		this(file, false);
	}
	
	/**
	 * @see #newDataSource
	 */
	public SqliteKV(Path file, boolean useLock) {
		this(file, useLock, 0);
	}
	
	/**
	 * @see #newDataSource
	 */
	public SqliteKV(Path file, boolean useLock, int mmapSize) {
		ds = newDataSource(file, useLock, Math.max(0, mmapSize));
		jdbcTemplate = new NamedParameterJdbcTemplate(ds);
		
		// if (useLock)
		// 	jdbcTemplate.queryForList("PRAGMA locking_mode=EXCLUSIVE", Collections.emptyMap());
		// if (mmapSize > 0)
		// 	jdbcTemplate.queryForList("PRAGMA mmap_size=" + mmapSize, Collections.emptyMap());
	}
	
	public SqliteKV(DataSource ds) {
		this.ds = ds;
		jdbcTemplate = new NamedParameterJdbcTemplate(ds);
	}
	
	/**
	 * @param useLock  use lock to gain 10 times speed up IO performance, but this block file access from other processes.
	 * @param mmapSize Memory-Mapped file size(byte), 0 to disable. <a href='https://cloud.tencent.com/developer/section/1420023'>see more</a'>
	 */
	public static DataSource newDataSource(Path file, boolean useLock, int mmapSize) {
		if (!file.toFile().getParentFile().exists())
			file.toFile().getParentFile().mkdirs();
		
		String url = String.format("jdbc:sqlite:%s?locking_mode=%s&mmap_size=%d",
				file.toString(), useLock ? "EXCLUSIVE" : "NORMAL", mmapSize);
		if (Oss.isAndroid()) {
			SQLiteConnectionPoolDataSource ds = new SQLiteConnectionPoolDataSource();
			ds.setUrl(url);
			// https://www.runoob.com/sqlite/sqlite-pragma.html
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
	
	private class DAOImpl implements DAO {
		final String group;
		final boolean suggestCompressValue;
		final boolean updateReadTime;
		
		/**
		 * @param group                snake_case_group
		 * @param suggestCompressValue value may be zip compress before save, depends on reducing size or not
		 * @param updateReadTime       update rt on each read
		 */
		DAOImpl(String group, boolean suggestCompressValue, boolean updateReadTime) {
			this.group = group;
			this.suggestCompressValue = suggestCompressValue;
			this.updateReadTime = updateReadTime;
		}
		
		private void ensureTable(String group) {
			if (!tableExist.contains(group)) {
				synchronized (group.intern()) {
					if (tableExist.contains(group))
						return;
					
					int tableCount = jdbcTemplate.queryForObject(
							"select count(2) from sqlite_master where type='table' and tbl_name=:name",
							Colls.ofHashMap("name", group), Integer.class
					);
					if (tableCount < 1) {
						String sql = "CREATE TABLE " + group +
								"(\n" +
								"k text constraint " + group + "_pk primary key,\n" +
								"v blob,\n" +
								"wt datetime default (datetime(CURRENT_TIMESTAMP,'localtime')),\n" +
								"rt datetime default (datetime(CURRENT_TIMESTAMP,'localtime'))\n" +
								")";
						jdbcTemplate.update(sql, Collections.emptyMap());
					}
					tableExist.add(group);
				}
			}
		}
		
		@Override
		public boolean containsKey(String key) {
			ensureTable(group);
			
			Integer count = jdbcTemplate.queryForObject(
					"select count(1) from " + group + " where k=:key",
					Colls.ofHashMap("key", key), Integer.class);
			return count > 0;
		}
		
		@Override
		public <V> V byKey(String key) {
			return byKey(key, null);
		}
		
		@Override
		public <V> V byKey(String key, V defaultValue) {
			ensureTable(group);
			
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
		public List<Map<String, Object>> byRawSql(String cols, String conditions, String clauses, Map<String, ?> params) {
			ensureTable(group);
			
			String sql = "select " + ObjectUtils.firstNonNull(cols, "*") + " from " + group
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
		public Item infoByKey(String key, boolean queryValue, boolean queryTime) {
			ensureTable(group);
			
			List<Map<String, Object>> lst = jdbcTemplate.queryForList(
					"select " + (queryValue ? "v," : "") + (queryTime ? "wt,rt," : "") + "1 from " + group + " where k=:key",
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
							"update " + group + " set rt=:now where k=:key",
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
			ensureTable(group);
			
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
			ensureTable(group);
			
			jdbcTemplate.update(
					"delete from " + group + " where rt<:va",
					Colls.ofHashMap("va", Times.format(Times.Formats.DayTime, validAfter)));
		}
		
		@Override
		public void remove(String key) {
			ensureTable(group);
			jdbcTemplate.update(
					"delete from " + group + " where k=:key",
					Colls.ofHashMap("key", key));
		}
		
		@Override
		public void save(String key, Object value) {
			save(key, value, -1);
		}
		
		@Override
		public void save(String key, Object value, int compressionLevel) {
			ensureTable(group);
			
			byte[] buf = SERIALIZER.serialize(value);
			if ((suggestCompressValue || Range.isWithin(0, 9, compressionLevel)) && buf.length > 256) {
				byte[] cb = Range.isWithin(0, 9, compressionLevel) ?
						Compresses.compressXz(buf, compressionLevel) : Compresses.compressXz(buf);
				if (cb.length < buf.length)
					buf = cb;
			}
			// wt and rt will always be the default value
			jdbcTemplate.update(
					"insert or replace into " + group + "(k,v) values(:key,:value)",
					Colls.ofHashMap("key", key, "value", buf)
			);
		}
		
		@Override
		public boolean exists(String key) {
			ensureTable(group);
			return jdbcTemplate.queryForObject("select count(1) from " + group + " where k=:key",
					Colls.ofHashMap("key", key), Integer.class) > 0;
		}
	}
	
	/**
	 * @param group snake_case_group style
	 */
	public DAO genDAO(String group) {
		return new DAOImpl(group, false, false);
	}
	
	/**
	 * @param group snake_case_group style
	 */
	public DAO genDAO(String group, boolean suggestCompressValue, boolean updateReadTime) {
		return new DAOImpl(group, suggestCompressValue, updateReadTime);
	}
	
	/**
	 * @param daoName use snake_case style
	 */
	public DAO genDAO(Class<?> c, String daoName, boolean suggestCompressValue, boolean updateReadTime) {
		String group = CodeUtil.camel2underline(c.getSimpleName()).toLowerCase() + "_" + daoName;
		return new DAOImpl(group, suggestCompressValue, updateReadTime);
	}
	
	/**
	 * @see <a href='http://www.sqlitetutorial.net/sqlite-vacuum/'>clear and rebuild db</a>
	 */
	public void maintain() {
		jdbcTemplate.update("VACUUM", Collections.emptyMap());
	}
}
