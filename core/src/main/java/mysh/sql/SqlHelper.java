package mysh.sql;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import mysh.codegen.CodeUtil;
import mysh.codegen.DynamicSql;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * SqlHelper
 *
 * @author mysh
 * @since 2015/8/25
 */
public class SqlHelper extends DynamicSql<SqlHelper> {
	private NamedParamQuery jdbc;

	private int pageNo;
	private int pageSize;
	private ResultHandler resultHandler;
	private CacheLevel cacheLevel;

	/**
	 * 拼接外部条件.
	 *
	 * @param cond 外部条件, 可 null
	 */
	public SqlHelper appendCond(SqlCondition cond) {
		if (cond.conds != null) {
			for (Object[] op : cond.conds) {
				switch ((SqlCondition.Op) op[0]) {
					case bi: bi((String) op[1], (String) op[2], (String) op[3], op[4]); break;
					case between: betweenExp((Boolean) op[1], (String) op[2], (String) op[3], op[4], (String) op[5], op[6]); break;
					case nil: nullExp((Boolean) op[1], (String) op[2]); break;
					case in: inExp((Boolean) op[1], (String) op[2], (Object[]) op[3]); break;
					case order: orderByExp((Boolean) op[1], (String) op[2]); break;
					case group: groupBy((String) op[1]); break;
					case page: page((Integer) op[1], (Integer) op[2]); break;
					default: throw new UnsupportedOperationException("undefined op: " + op[0]);
				}
			}
		}
		return this;
	}

	/**
	 * 拼接外部参数.
	 *
	 * @param params 外部参数, 可 null
	 */
	public SqlHelper appendParams(Map<String, ?> params) {
		if (params != null) {
			Map<String, Object> pm = getParamMap();
			for (Map.Entry<String, ?> entry : params.entrySet()) {
				Object oldValue = pm.put(entry.getKey(), entry.getValue());
				if (oldValue != null)
					throw new RuntimeException("duplicated param: " + entry.getKey());
			}
		}
		return this;
	}

	/**
	 * 分页信息。
	 */
	public SqlHelper page(int pageNo, int pageSize) {
		if (pageNo < 1 || pageSize < 1)
			throw new IllegalArgumentException("page info should be positive");

		this.pageNo = pageNo;
		this.pageSize = pageSize;
		return this;
	}

	private final ConcurrentHashMap<Class<?>, Map<String, Field>> classFields = new ConcurrentHashMap<>();

	/**
	 * 取数据，返回给定类型的 list.
	 */
	public <M> List<M> fetch(Class<M> type) throws Exception {
		List<Map<String, Object>> results = fetch();
		if (results == null) return Collections.emptyList();

		Map<String, Field> modelFields = getTypeFields(type);
		List<M> rs = new ArrayList<>(results.size());
		for (Map<String, Object> result : results) {
			M model = type.newInstance();
			assembleResult(result, model, modelFields);
			rs.add(model);
		}
		return rs;
	}

	private void assembleResult(Map<String, Object> result, Object model,
	                            Map<String, Field> modelFields) throws IllegalAccessException, InstantiationException {
		for (Map.Entry<String, Field> entry : modelFields.entrySet()) {
			Field field = entry.getValue();
			if ((field.getModifiers() & Modifier.FINAL) > 0
							|| (field.getModifiers() & Modifier.STATIC) > 0
							) continue;

			if (isBasicType(field.getType())) {
				Object value = result.get(entry.getKey());
				if (value != null)
					setFieldWithProperType(model, field, value);
			} else {
				Class<?> fieldType = field.getType();
				Map<String, Field> fieldFields = getTypeFields(fieldType);
				Object fieldInst = fieldType.newInstance();
				assembleResult(result, fieldInst, fieldFields);
				field.set(model, fieldInst);
			}
		}
	}

	/**
	 * basic type from jdbc result set, such as BigDecimal,Long,String etc.
	 */
	private boolean isBasicType(Class<?> type) {
		return Number.class.isAssignableFrom(type)
						|| String.class.isAssignableFrom(type)
						|| Date.class.isAssignableFrom(type)
						|| type.isPrimitive();
	}

	private void setFieldWithProperType(Object model, Field field, Object value) throws IllegalAccessException {
		Class<?> ft = field.getType();
		Class<?> vt = value.getClass();
		if (ft != vt) {
			if (vt == BigDecimal.class) {
				if (ft == Long.class)
					value = ((BigDecimal) value).longValue();
				else if (ft == Integer.class)
					value = ((BigDecimal) value).intValue();
				else if (ft == Double.class)
					value = ((BigDecimal) value).doubleValue();
				else if (ft == Short.class)
					value = ((BigDecimal) value).shortValue();
				else if (ft == Float.class)
					value = ((BigDecimal) value).floatValue();
				else if (ft == Byte.class)
					value = ((BigDecimal) value).byteValue();
			}
		}
		field.set(model, value);
	}

	/**
	 * get fields defined by type (and recurred super classes).
	 * fields with same name in super classes will be covered  by sub class.
	 */
	private Map<String, Field> getTypeFields(final Class<?> type) {
		Map<String, Field> fields = classFields.get(type);
		if (fields == null) {
			Stack<Class<?>> types = new Stack<>();
			Class<?> t = type;
			while (t != null) {
				types.push(t);
				t = t.getSuperclass();
			}

			fields = new HashMap<>();
			while (!types.isEmpty()) {
				t = types.pop();
				Field[] dfs = t.getDeclaredFields();
				for (Field df : dfs) {
					df.setAccessible(true);
					fields.put(CodeUtil.camel2underline(df.getName()), df);
				}
			}
			classFields.put(type, fields);
		}
		return fields;
	}

	private static final Map<CacheLevel, Cache<CacheKey, List<Map<String, Object>>>> caches = new HashMap<>();

	static {
		// 初始化缓存
		for (CacheLevel cl : CacheLevel.values()) {
			caches.put(cl,
							CacheBuilder.newBuilder()
											.maximumSize(1024)
											.expireAfterWrite(cl.seconds, TimeUnit.SECONDS)
											.<CacheKey, List<Map<String, Object>>>build()
			);
		}
	}

	private class CacheKey {
		private String sql;
		private Map<String, Object> param;

		CacheKey(String sql, Map<String, Object> param) {
			this.sql = sql;
			this.param = param;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			CacheKey cacheKey = (CacheKey) o;

			if (!sql.equals(cacheKey.sql)) return false;
			return param.equals(cacheKey.param);
		}

		@Override
		public int hashCode() {
			int result = sql.hashCode();
			result = 31 * result + param.hashCode();
			return result;
		}
	}

	/**
	 * 取数据
	 */
	public List<Map<String, Object>> fetch() throws Exception {
		String sql = getCondStr();
		if (pageNo > 0)
			sql = genSql(sql, pageNo, pageSize);
		final Map<String, Object> param = getParamMap();

		List<Map<String, Object>> result;
		if (this.cacheLevel != null) {
			final String finalSql = sql;
			result = caches.get(this.cacheLevel)
							.get(new CacheKey(sql, param), new Callable<List<Map<String, Object>>>() {
								@Override
								public List<Map<String, Object>> call() throws Exception {
									return jdbc.queryForList(finalSql, param);
								}
							});
		} else
			result = jdbc.queryForList(sql, param);

		handleResult(result);

		return result;
	}

	private String genSql(String sql, int pageNo, int pageSize) {
		if (DbUtil.isOracle()) {
			int noFirst = (pageNo - 1) * pageSize;
			int noLast = pageNo * pageSize;

			return "SELECT * FROM (SELECT ROWNUM R_, S_.* FROM ("
							+ sql + " ) S_ WHERE ROWNUM <= " + noLast + " ) WHERE R_ > " + noFirst;
		} else {
			int noFirst = (pageNo - 1) * pageSize;
			return sql + " limit " + noFirst + "," + pageSize;
		}
	}

	/**
	 * 结果处理器
	 */
	public SqlHelper onResult(ResultHandler rh) {
		this.resultHandler = rh;
		return this;
	}

	/**
	 * 缓存级别
	 */
	public enum CacheLevel {
		M1(60), M5(60 * 5), M15(60 * 15), M30(60 * 30), H1(60 * 60), D1(60 * 60 * 24);

		int seconds;

		CacheLevel(int seconds) {
			this.seconds = seconds;
		}
	}

	/**
	 * 结果缓存时间级别
	 */
	public SqlHelper cacheLevel(CacheLevel cacheLevel) {
		this.cacheLevel = cacheLevel;
		return this;
	}

	private List<Map<String, Object>> handleResult(List<Map<String, Object>> result) {
		if (result != null) {
			// Mysql: make sure value map is upper case key
			if (!DbUtil.isOracle()) {
				List<Map<String, Object>> newR = new ArrayList<>();
				for (Map<String, Object> r : result) {
					Map<String, Object> newM = new HashMap<>();
					for (Map.Entry<String, Object> e : r.entrySet()) {
						newM.put(e.getKey().toUpperCase(), e.getValue());
					}
					newR.add(newM);
				}
				result = newR;
			}

			for (Map<String, Object> map : result) {
				if (this.resultHandler != null)
					this.resultHandler.handle(map);
			}
		}
		return result;
	}

	// ========== create below =============
	public static SqlHelper create(NamedParamQuery jdbc, String querySql) {
		return new SqlHelper(jdbc,
						new StringBuilder(Objects.requireNonNull(querySql, "query sql can't be null")), null);
	}

	public SqlHelper(NamedParamQuery jdbc, StringBuilder cond, Map<String, Object> paramMap) {
		super(cond, paramMap);
		this.jdbc = Objects.requireNonNull(jdbc, "jdbc template can't be null");
	}

}
