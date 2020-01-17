package mysh.sql.sqlite;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import mysh.codegen.CodeUtil;
import mysh.collect.Colls;
import mysh.util.Compresses;
import mysh.util.Serializer;
import mysh.util.Tick;
import mysh.util.Times;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @since 2019-07-08
 */
@Slf4j
public class SqliteKV implements Closeable {
	private static final Serializer SERIALIZER = Serializer.BUILD_IN;
	
	private DruidDataSource ds;
	private NamedParameterJdbcOperations jdbcTemplate;
	private Set<String> tableExist = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	public static class Item {
		private String key;
		private Instant writeTime, readTime;
		private Object value;
		
		public String getKey() {
			return key;
		}
		
		public Instant getWriteTime() {
			return writeTime;
		}
		
		public Instant getReadTime() {
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
		
		List<Item> itemsByRawSql(String cols, String conditions, String clauses, Map<String, ?> params);
		
		List<Map<String, Object>> byRawSql(String cols, String conditions, String clauses, Map<String, ?> params);
		
		Item infoByKey(String key, boolean queryValue, boolean queryTime);
		
		Item timeByKey(String key);
		
		default Item infoByKey(String key) {
			return infoByKey(key, true, true);
		}
		
		<V> V byKeyRemoveOnWriteExpired(String key, Instant validAfter);
		
		void removeReadExpired(Instant validAfter);
		
		void remove(String key);
		
		void save(String key, Object value);
		
		boolean exists(String key);
	}
	
	public SqliteKV(Path file) {
		this(file, false);
	}
	
	/**
	 * @param useLock about 10 times speed up on query, but the db file will be locked by the process.
	 */
	public SqliteKV(Path file, boolean useLock) {
		ds = SqliteUtil.newDataSource(file, useLock);
		jdbcTemplate = new NamedParameterJdbcTemplate(ds);
	}
	
	public SqliteKV(DataSource ds) {
		jdbcTemplate = new NamedParameterJdbcTemplate(ds);
	}
	
	@Override
	public void close() {
		if (ds != null)
			ds.close();
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
								"wt datetime default CURRENT_TIMESTAMP,\n" +
								"rt datetime default CURRENT_TIMESTAMP\n" +
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
			
			String sql = "select " + cols + " from " + group + " where " + conditions + " " + ObjectUtils.firstNonNull(clauses, "");
			params = params == null ? Collections.emptyMap() : params;
			Tick tick = Tick.tick();
			List<Map<String, Object>> lst = jdbcTemplate.queryForList(sql, params);
			if (tick.nip() > 30) {
				log.warn("sql-need-optimize,cost={}ms, sql={}, params={}", tick.nip(), sql, params);
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
							"update " + group + " set rt=CURRENT_TIMESTAMP where k=:key",
							Colls.ofHashMap("key", key));
				}
				return item;
			}
		}
		
		private void assembleItem(Map<String, Object> r, Item item, boolean queryValue, boolean queryTime) {
			byte[] blob = (byte[]) r.get("v");
			if (queryValue && blob != null) {
				if (suggestCompressValue && blob.length > 2 && blob[0] == 'P' && blob[1] == 'K') {
					AtomicReference<Object> vr = new AtomicReference<>();
					Compresses.deCompress(
							(entry, in) -> {
								vr.set(SERIALIZER.deserialize(in));
							},
							new ByteArrayInputStream(blob));
					item.value = vr.get();
				} else
					item.value = SERIALIZER.deserialize(blob);
			}
			
			if (queryTime) {
				String wt = (String) r.get("wt");
				if (wt != null)
					item.writeTime = Times.parseDayTime(Times.Formats.DayTime, wt)
					                      .atZone(Times.zoneUTC).toInstant();
				
				String rt = (String) r.get("rt");
				if (rt != null)
					item.readTime = Times.parseDayTime(Times.Formats.DayTime, rt)
					                     .atZone(Times.zoneUTC).toInstant();
			}
		}
		
		@Override
		public <V> V byKeyRemoveOnWriteExpired(String key, Instant validAfter) {
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
		public void removeReadExpired(Instant validAfter) {
			ensureTable(group);
			
			jdbcTemplate.update(
					"delete from " + group + " where rt<:va",
					Colls.ofHashMap("va", validAfter));
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
			ensureTable(group);
			
			byte[] buf = SERIALIZER.serialize(value);
			if (suggestCompressValue) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Compresses.compress("d", new ByteArrayInputStream(buf), buf.length, out, -1);
				byte[] cb = out.toByteArray();
				
				if (cb.length < buf.length)
					buf = cb;
			}
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
