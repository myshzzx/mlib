package mysh.sql;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import mysh.codegen.CodeUtil;
import mysh.codegen.DynamicSql;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * SqlHelper
 *
 * @author mysh
 * @since 2015/8/25
 */
public class SqlHelper extends DynamicSql<SqlHelper> {
	private NamedParameterJdbcTemplate jdbc;
	
	private int pageNo;
	private int pageSize;
	private List<ResultHandler> resultHandlers;
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
					case bi:
						bi((String) op[1], (String) op[2], (String) op[3], op[4]); break;
					case between:
						betweenExp((Boolean) op[1], (String) op[2], (String) op[3], op[4], (String) op[5], op[6]); break;
					case nil:
						nullExp((Boolean) op[1], (String) op[2]); break;
					case in:
						inExp((Boolean) op[1], (String) op[2], (Object[]) op[3]); break;
					case order:
						orderByExp((Boolean) op[1], (String) op[2]); break;
					case group:
						groupBy((String) op[1]); break;
					case page:
						page((Integer) op[1], (Integer) op[2]); break;
					default:
						throw new UnsupportedOperationException("undefined op: " + op[0]);
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
	
	private static final EnumSet<KeyStrategy> upperCaseKey = EnumSet.of(KeyStrategy.UPPER_CASE);
	
	/**
	 * 取数据，返回给定类型的 list.
	 */
	public <M> List<M> fetch(Class<M> type) throws Exception {
		List<Map<String, Object>> results = fetch(upperCaseKey);
		if (results == null) return Collections.emptyList();
		
		Map<String, Field> modelFields = getTypeFields(type);
		List<M> rs = new ArrayList<>(results.size());
		for (Map<String, Object> result : results) {
			M model = type.getConstructor().newInstance();
			assembleResult(result, model, modelFields);
			rs.add(model);
		}
		return rs;
	}
	
	/**
	 * assemble result(row set) to given instance.
	 *
	 * @param result      row set result
	 * @param model       instance
	 * @param modelFields include fields defined in recurring super classes,
	 *                    which can be primitive type or complex type.
	 */
	private void assembleResult(
			Map<String, Object> result, Object model, Map<String, Field> modelFields)
			throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// iterate fields but not results to make sure all complex-type fields be assembled
		for (Map.Entry<String, Field> fe : modelFields.entrySet()) {
			Field field = fe.getValue();
			if ((field.getModifiers() & Modifier.FINAL) > 0
					    || (field.getModifiers() & Modifier.STATIC) > 0
			) continue;
			
			Class<?> fieldType = field.getType();
			if (isBasicType(fieldType)) {
				Object value = result.get(fe.getKey());
				if (value != null)
					setFieldWithProperType(model, field, value);
			} else {
				Map<String, Field> fieldFields = getTypeFields(fieldType);
				Object fieldInst = fieldType.getConstructor().newInstance();
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
					            .build()
			);
		}
	}
	
	private class CacheKey {
		private String sql;
		private Map<String, Object> param;
		private EnumSet<KeyStrategy> ks;
		
		CacheKey(String sql, Map<String, Object> param, EnumSet<KeyStrategy> ks) {
			this.sql = sql;
			this.param = param;
			this.ks = ks;
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			
			CacheKey cacheKey = (CacheKey) o;
			
			if (!sql.equals(cacheKey.sql)) return false;
			if (!param.equals(cacheKey.param)) return false;
			return ks != null ? ks.equals(cacheKey.ks) : cacheKey.ks == null;
		}
		
		@Override
		public int hashCode() {
			int result = sql.hashCode();
			result = 31 * result + param.hashCode();
			result = 31 * result + (ks != null ? ks.hashCode() : 0);
			return result;
		}
	}
	
	/**
	 * 取数据为 map 时 key 策略.
	 */
	public enum KeyStrategy {
		/**
		 * key is converted to upper case
		 */
		UPPER_CASE,
		/**
		 * key is converted upper case, then camel case.
		 */
		CAMEL
	}
	
	/**
	 * fetch <code>list&lt;map&gt;</code>
	 */
	public List<Map<String, Object>> fetch() throws Exception {
		return fetch((EnumSet<KeyStrategy>) null);
	}
	
	/**
	 * fetch <code>list&lt;map&gt;</code>.
	 *
	 * @param ks key strategy of map.
	 */
	public List<Map<String, Object>> fetch(final EnumSet<KeyStrategy> ks) throws Exception {
		String sql = getCondStr();
		if (pageNo > 0)
			sql = genSql(sql, pageNo, pageSize);
		final Map<String, Object> param = getParamMap();
		
		List<Map<String, Object>> result;
		if (this.cacheLevel != null) {
			final String finalSql = sql;
			result = caches.get(this.cacheLevel)
					         .get(new CacheKey(sql, param, ks), () -> {
						         List<Map<String, Object>> r = jdbc.queryForList(finalSql, param);
						         r = SqlHelper.this.handleResult(r, ks);
						         return r;
					         });
		} else {
			result = jdbc.queryForList(sql, param);
			result = handleResult(result, ks);
		}
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
	 * 结果处理器, 可调用多次添加多个结果处理器, 处理结果时按 handler 添加顺序依次调用.
	 */
	public SqlHelper onResult(ResultHandler rh) {
		if (this.resultHandlers == null)
			this.resultHandlers = new ArrayList<>();
		this.resultHandlers.add(rh);
		return this;
	}
	
	/**
	 * 将返回结果中的时间数据转成字符串。
	 * yyyy-MM-dd HH:mm:ss
	 */
	public static final ResultHandler dateValue2Str = new ResultHandler() {
		private final DateFormat dfFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		@Override
		public void handle(Map<String, Object> map) {
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				if (entry.getValue() instanceof Date)
					entry.setValue(dfFull.format(entry.getValue()));
			}
		}
	};
	
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
	
	private List<Map<String, Object>> handleResult(List<Map<String, Object>> result, EnumSet<KeyStrategy> ks) {
		if (result != null) {
			if (ks != null && (ks.contains(KeyStrategy.UPPER_CASE) || ks.contains(KeyStrategy.CAMEL))) {
				List<Map<String, Object>> newR = new ArrayList<>();
				for (Map<String, Object> r : result) {
					Map<String, Object> newM = new HashMap<>();
					for (Map.Entry<String, Object> e : r.entrySet()) {
						String newKey = e.getKey().toUpperCase();
						if (ks.contains(KeyStrategy.CAMEL))
							newKey = CodeUtil.underline2FieldCamel(newKey);
						newM.put(newKey, e.getValue());
					}
					newR.add(newM);
				}
				result = newR;
			}
			
			if (this.resultHandlers != null)
				for (Map<String, Object> map : result) {
					for (ResultHandler handler : this.resultHandlers) {
						handler.handle(map);
					}
				}
		}
		return result;
	}
	
	// ========== create below =============
	public static SqlHelper create(NamedParameterJdbcTemplate jdbc, String querySql) {
		return new SqlHelper(jdbc,
				new StringBuilder(Objects.requireNonNull(querySql, "query sql can't be null")), null);
	}
	
	public SqlHelper(NamedParameterJdbcTemplate jdbc, StringBuilder cond, Map<String, Object> paramMap) {
		super(cond, paramMap);
		this.jdbc = Objects.requireNonNull(jdbc, "jdbc template can't be null");
	}
	
}
