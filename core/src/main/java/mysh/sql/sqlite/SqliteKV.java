package mysh.sql.sqlite;

import com.alibaba.druid.pool.DruidDataSource;
import mysh.collect.Colls;
import mysh.collect.Pair;
import mysh.util.Compresses;
import mysh.util.Serializer;
import mysh.util.Times;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @since 2019-07-08
 */
public class SqliteKV implements Closeable {
	private static final Serializer SERIALIZER = Serializer.BUILD_IN;
	
	private DruidDataSource ds;
	private NamedParameterJdbcOperations jdbcTemplate;
	private Set<String> tableExist = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	public interface DAO {
		<V> V byKey(String key);
		
		<V> Pair<V, Instant> infoByKey(String key);
		
		<V> V byKeyRemoveExpired(String key, Instant validAfter);
		
		void remove(String key);
		
		void save(String key, Object value);
	}
	
	public SqliteKV(Path file) {
		ds = new DruidDataSource(true);
		ds.setUrl("jdbc:sqlite:" + file.toString());
		ds.setMaxActive(10);
		
		jdbcTemplate = new NamedParameterJdbcTemplate(ds);
	}
	
	@Override
	public void close() {
		if (ds != null)
			ds.close();
	}
	
	private class DAOImpl implements DAO {
		String group;
		boolean valueCompressed;
		
		DAOImpl(String group, boolean valueCompressed) {
			this.group = group;
			this.valueCompressed = valueCompressed;
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
								             "t datetime default CURRENT_TIMESTAMP\n" +
								             ")";
						jdbcTemplate.update(sql, Collections.emptyMap());
					}
					tableExist.add(group);
				}
			}
		}
		
		@Override
		public <V> V byKey(String key) {
			ensureTable(group);
			
			final Pair<V, Instant> info = infoByKey(key);
			return info == null ? null : info.getL();
		}
		
		@Override
		public <V> Pair<V, Instant> infoByKey(String key) {
			ensureTable(group);
			
			List<Map<String, Object>> lst = jdbcTemplate.queryForList(
					"select v,t from " + group + " where k=:key", Colls.ofHashMap("key", key));
			if (Colls.isEmpty(lst))
				return null;
			else {
				Map<String, Object> r = lst.get(0);
				byte[] blob = (byte[]) r.get("v");
				Instant t = Times.parseDayTime(Times.Formats.DayTime, (String) r.get("t"))
				                 .atZone(Times.zoneUTC).toInstant();
				
				V v;
				if (valueCompressed) {
					AtomicReference<V> vr = new AtomicReference<>();
					Compresses.deCompress(
							(entry, in) -> {
								vr.set(SERIALIZER.deserialize(in));
							},
							new ByteArrayInputStream(blob));
					v = vr.get();
				} else
					v = SERIALIZER.deserialize(blob);
				
				return Pair.of(v, t);
			}
		}
		
		@Override
		public <V> V byKeyRemoveExpired(String key, Instant validAfter) {
			ensureTable(group);
			
			final Pair<V, Instant> info = infoByKey(key);
			if (info != null) {
				if (info.getR().compareTo(validAfter) < 0) {
					remove(key);
					return null;
				} else
					return info.getL();
			} else
				return null;
		}
		
		@Override
		public void remove(String key) {
			ensureTable(group);
			jdbcTemplate.update(
					"delete from " + group + " where k=:key", Colls.ofHashMap("key", key));
		}
		
		@Override
		public void save(String key, Object value) {
			ensureTable(group);
			
			byte[] buf = SERIALIZER.serialize(value);
			if (valueCompressed) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Compresses.compress("d", new ByteArrayInputStream(buf), buf.length, out, -1);
				buf = out.toByteArray();
			}
			jdbcTemplate.update(
					"insert or replace into " + group + "(k,v) values(:key,:value)",
					Colls.ofHashMap("key", key, "value", buf)
			);
		}
	}
	
	/**
	 * @param group snake_case_group style
	 */
	public DAO genDAO(String group) {
		return new DAOImpl(group, false);
	}
	
	/**
	 * @param group snake_case_group style
	 */
	public DAO genDAO(String group, boolean valueCompressed) {
		return new DAOImpl(group, valueCompressed);
	}
}
