package mysh.codegen;


import mysh.util.Strings;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SQL generator.
 * make it possible to write java code just like writing sql.
 * Or even more comfortable, if you are using JetBrains IntelliJ IDEA, by which you can get col name
 * from code completion wisely.
 * notice that null or blank value will cause current expression ignored.
 * for example:
 * <pre>
 *   sql.eq("max_Value", 10)
 *      .on(isNotEmpty(name())).like("GivenName", name())
 *      .like("address", "")
 *      .orderBy("createTime");
 * </pre>
 * and which you get is:
 * <pre>
 *   1=1  AND max_Value = :max_Value  AND GIVEN_NAME like :GivenName  ORDER BY CREATE_TIME
 * </pre>
 * with a param map may be {max_Value=10, GivenName=%mysh%}
 *
 * @author mysh
 * @since 2015/6/29.
 */
@SuppressWarnings({"unused", "unchecked"})
public class DynamicSql<T extends DynamicSql> {

	// expression handler below ==============================

	/**
	 * col = value
	 */
	public T eq(String col, Object value) {
		return bi(col, "=", col, value);
	}

	/**
	 * col = value
	 */
	public T eq(String col, String valueName, Object value) {
		return bi(col, "=", valueName, value);
	}

	/**
	 * col != value
	 */
	public T notEq(String col, Object value) {
		return bi(col, "!=", col, value);
	}

	/**
	 * col != value
	 */
	public T notEq(String col, String valueName, Object value) {
		return bi(col, "!=", valueName, value);
	}

	/**
	 * col like %value%
	 */
	public T like(String col, String value) {
		if (ignoreChk(value)) return (T) this;
		return bi(col, "LIKE", col, "%" + value + "%");
	}

	/**
	 * col like %value%
	 */
	public T like(String col, String valueName, String value) {
		if (ignoreChk(value)) return (T) this;
		return bi(col, "LIKE", valueName, "%" + value + "%");
	}

	/**
	 * col not like %value%
	 */
	public T notLike(String col, String value) {
		if (ignoreChk(value)) return (T) this;
		return bi(col, "NOT LIKE", col, "%" + value + "%");
	}

	/**
	 * col not like %value%
	 */
	public T notLike(String col, String valueName, String value) {
		if (ignoreChk(value)) return (T) this;
		return bi(col, "NOT LIKE", valueName, "%" + value + "%");
	}

	/**
	 * col like value%
	 */
	public T likeLeft(String col, String value) {
		if (ignoreChk(value)) return (T) this;
		return bi(col, "LIKE", col, value + "%");
	}

	/**
	 * col like value%
	 */
	public T likeLeft(String col, String valueName, String value) {
		if (ignoreChk(value)) return (T) this;
		return bi(col, "LIKE", valueName, value + "%");
	}

	/**
	 * col not like value%
	 */
	public T notLikeLeft(String col, String value) {
		if (ignoreChk(value)) return (T) this;
		return bi(col, "NOT LIKE", col, value + "%");
	}

	/**
	 * col not like value%
	 */
	public T notLikeLeft(String col, String valueName, String value) {
		if (ignoreChk(value)) return (T) this;
		return bi(col, "NOT LIKE", valueName, value + "%");
	}

	/**
	 * col > value
	 */
	public T gt(String col, Object value) {
		return bi(col, ">", col, value);
	}

	/**
	 * col > value
	 */
	public T gt(String col, String valueName, Object value) {
		return bi(col, ">", valueName, value);
	}

	/**
	 * col >= value
	 */
	public T ge(String col, Object value) {
		return bi(col, ">=", col, value);
	}

	/**
	 * col >= value
	 */
	public T ge(String col, String valueName, Object value) {
		return bi(col, ">=", valueName, value);
	}

	/**
	 * col < value
	 */
	public T lt(String col, Object value) {
		return bi(col, "<", col, value);
	}

	/**
	 * col < value
	 */
	public T lt(String col, String valueName, Object value) {
		return bi(col, "<", valueName, value);
	}

	/**
	 * col <= value
	 */
	public T le(String col, Object value) {
		return bi(col, "<=", col, value);
	}

	/**
	 * col <= value
	 */
	public T le(String col, String valueName, Object value) {
		return bi(col, "<=", valueName, value);
	}

	/**
	 * 二元表达式
	 *
	 * @param col        字段名
	 * @param op         操作符
	 * @param paramValue 参数值
	 */
	public T bi(String col, String op, Object paramValue) {
		return bi(col, op, col, paramValue);
	}

	/**
	 * 二元表达式
	 *
	 * @param col        字段名
	 * @param op         操作符
	 * @param paramName  参数表的参数名
	 * @param paramValue 参数值
	 */
	public T bi(String col, String op, String paramName, Object paramValue) {
		if (ignoreChk(paramValue)) return (T) this;

		cond.append(" AND ");
		autoConvertCol(cond, col);
		cond.append(" ");
		cond.append(op);
		cond.append(" :");
		cond.append(putParam(paramName, paramValue, false));
		cond.append(" ");
		return (T) this;
	}

	/**
	 * col between from and to
	 */
	public T between(String col, Object from, Object to) {
		return betweenExp(true, col, col + "from", from, col + "to", to);
	}

	/**
	 * col between from and to
	 */
	public T between(String col, String fromName, Object from, String toName, Object to) {
		return betweenExp(true, col, fromName, from, toName, to);
	}

	/**
	 * col not between from and to
	 */
	public T notBetween(String col, Object from, Object to) {
		return betweenExp(false, col, col + "from", from, col + "to", to);
	}

	/**
	 * col not between from and to
	 */
	public T notBetween(String col, String fromName, Object from, String toName, Object to) {
		return betweenExp(false, col, fromName, from, toName, to);
	}

	/**
	 * col between from and to
	 *
	 * @param flag is between or not between
	 */
	private T betweenExp(boolean flag,
	                     String col, String fromName, Object from, String toName, Object to) {
		if (ignoreChk(fromName, from, toName, to)) return (T) this;

		cond.append(" AND ");
		autoConvertCol(cond, col);
		cond.append(flag ? " BETWEEN :" : " NOT BETWEEN :");
		cond.append(putParam(fromName, from, false));
		cond.append(" AND :");
		cond.append(putParam(toName, to, false));
		cond.append(" ");
		return (T) this;
	}

	/**
	 * col is null
	 */
	public T isNull(String col) {
		return nullExp(true, col);
	}

	/**
	 * col is not null
	 */
	public T isNotNull(String col) {
		return nullExp(false, col);
	}

	@SuppressWarnings("Duplicates")
	private T nullExp(boolean flag, String col) {
		if (ignoreChk()) return (T) this;

		cond.append(" AND ");
		autoConvertCol(cond, col);
		cond.append(flag ? " IS NULL " : " IS NOT NULL ");

		return (T) this;
	}

	/**
	 * append to current sql directly. you don't need to put a prefix "and".
	 * params should be [paramName, paramValue] pairs.
	 */
	public T append(String sql, Object... params) {
		if (ignoreChk(params)) return (T) this;

		cond.append(" AND ");
		cond.append(sql);
		cond.append(' ');

		if (params != null) {
			if (params.length % 2 != 0)
				throw new IllegalArgumentException("params should be name-value pairs");
			for (int i = 0; i < params.length; i += 2) {
				putParam((String) params[i], params[i + 1], true);
			}
		}
		return (T) this;
	}

	/**
	 * col in (e1, e2)
	 */
	public T in(String col, Object... enums) {
		return inExp(true, col, enums);
	}

	/**
	 * col in (e1, e2)
	 */
	public T inSepStr(String col, String enumStr, String sepRegex) {
		if (enumStr == null) return (T) this;
		return inExp(true, col, enumStr.split(sepRegex));
	}

	/**
	 * col not in (e1, e2)
	 */
	public T notIn(String col, Object... enums) {
		return inExp(false, col, enums);
	}

	/**
	 * col not in (e1, e2)
	 */
	public T notInSepStr(String col, String enumStr, String sepRegex) {
		if (enumStr == null) return (T) this;
		return inExp(false, col, enumStr.split(sepRegex));
	}

	private T inExp(boolean flag, String col, Object[] enums) {
		if (enums == null || enums.length == 0 || ignoreChk(enums)) return (T) this;

		cond.append(" AND ");
		autoConvertCol(cond, col);
		cond.append(flag ? " IN (" : " NOT IN (");
		for (int i = 0; i < enums.length; i++) {
			if (i > 0) cond.append(',');

			Object e = enums[i];
			if (e instanceof String) {
				cond.append('\'');
				cond.append(((String) e).replace("'", "''"));
				cond.append('\'');
			} else
				cond.append(e);
		}
		cond.append(") ");

		return (T) this;
	}

	/**
	 * order by col
	 */
	public T orderBy(String col) {
		return orderByExp(true, col);
	}

	/**
	 * order by col desc
	 */
	public T orderByDesc(String col) {
		return orderByExp(false, col);
	}

	@SuppressWarnings("Duplicates")
	private T orderByExp(boolean flag, String col) {
		if (ignoreChk()) return (T) this;

		cond.append(" ORDER BY ");
		autoConvertCol(cond, col);
		cond.append(flag ? " " : " DESC ");

		return (T) this;
	}

	/**
	 * group by col
	 */
	public T groupBy(String col) {
		if (ignoreChk()) return (T) this;

		cond.append(" GROUP BY ");
		autoConvertCol(cond, col);
		cond.append(" ");

		return (T) this;
	}

	// expression handler above ==============================


	// col convert below =============================
	private String tableAlias;
	private static final WeakHashMap<String, String> camel2UnderlineCols = new WeakHashMap<>();
	private static final ReentrantReadWriteLock camel2UnderlineColsLock = new ReentrantReadWriteLock();

	/**
	 * set table alias. to clean old alias, use null.
	 */
	public T setTableAlias(String alias) {
		tableAlias = Strings.isBlank(alias) ? null : alias.trim() + '.';
		return (T) this;
	}

	/**
	 * auto convert hump col name to underline connected name,
	 * like maxValue -> MAX_VALUE.
	 */
	private void autoConvertCol(StringBuilder cond, final String col) {
		String underLine = null;
		camel2UnderlineColsLock.readLock().lock();
		try {
			underLine = camel2UnderlineCols.get(col);
		} finally {
			camel2UnderlineColsLock.readLock().unlock();
		}

		if (underLine == null) {
			if (Strings.isBlank(col))
				throw new IllegalArgumentException("column name is blank");

			underLine = col;
			for (int i = 0; i < col.length(); i++) {
				if (CodeUtil.isLowerCase(col.charAt(i))) {
					underLine = null;
					break;
				}
			}

			if (underLine == null)
				underLine = CodeUtil.camel2underline(col);

			camel2UnderlineColsLock.writeLock().lock();
			try {
				camel2UnderlineCols.put(col, underLine);
			} finally {
				camel2UnderlineColsLock.writeLock().unlock();
			}
		}

		if (tableAlias != null)
			cond.append(tableAlias);
		cond.append(underLine);
	}

	// col convert above =============================


	// ignoreNext below =============================

	private boolean ignoreNext = false;

	/**
	 * if the condition is false, then the next expression will be ignored.
	 * common use should look like:
	 * <pre>
	 *   // if the condition is false, the equal expression will not be appended.
	 *   helper.on(condtion).eq("age", 100);
	 * </pre>
	 */
	public T on(boolean condition) {
		ignoreNext = !condition;
		return (T) this;
	}

	/**
	 * the same as on(isNotBlank(obj).
	 * see {@link #on(boolean)}
	 */
	public T onNonBlank(Object obj) {
		ignoreNext = isBlank(obj);
		return (T) this;
	}

	/**
	 * see {@link #ignoreChk(Object...)}
	 */
	private boolean ignoreChk() {
		boolean ignore = ignoreNext;
		ignoreNext = false;

		return ignore;
	}

	/**
	 * see {@link #ignoreChk(Object...)}
	 */
	private boolean ignoreChk(Object param) {
		boolean ignore = ignoreNext;
		ignoreNext = false;

		return ignore || isBlank(param);
	}

	/**
	 * should current expression been ignored.
	 * if previous on-condition is false, or any param is null(or blank), this will return true.
	 * the flag will be reset after this invoke.
	 */
	private boolean ignoreChk(Object... params) {
		boolean ignore = ignoreNext;
		ignoreNext = false;

		if (!ignore && params != null) {
			for (Object param : params) {
				if (isBlank(param)) return true;
			}
		}
		return ignore;
	}

	/**
	 * param==null or param is a blank string
	 */
	private boolean isBlank(Object param) {
		if (param == null) return true;
		if (param instanceof String) {
			if (Strings.isBlank((String) param)) return true;
		}
		return false;
	}

	// ignoreNext above ===========================

	// check param name below ===========================
	private int paramNameSf = 1;

	/**
	 * put param to param map. if chkOverride, an exception will be thrown when param overridden,
	 * or a new param name will be used when the given name exists.
	 *
	 * @return param name that real used in param map.
	 */
	private String putParam(final String name, Object value, boolean chkOverride) {
		if (chkOverride && paramMap.containsKey(name))
			throw new IllegalArgumentException("param[" + name + "] overridden with value:" + value);

		String paramName = name;
		while (paramMap.containsKey(paramName)) {
			paramName = name + paramNameSf++;
		}
		paramMap.put(paramName, value);
		return paramName;
	}
	// check param name above ===========================

	// create fields below =========================================

	public static DynamicSql create() {
		return create(null, null);
	}

	public static DynamicSql create(Map<String, Object> params) {
		return create(null, params);
	}

	public static DynamicSql create(StringBuilder cond, Map<String, Object> params) {
		return new DynamicSql(cond, params);
	}

	private StringBuilder cond;
	private Map<String, Object> paramMap;

	protected DynamicSql(StringBuilder cond, Map<String, Object> paramMap) {
		if (cond == null) cond = new StringBuilder("1=1 ");
		if (paramMap == null) paramMap = new HashMap<>();
		this.cond = cond;
		this.paramMap = paramMap;
	}

	public StringBuilder getCond() {
		return cond;
	}

	public String getCondStr() {
		return cond.toString();
	}

	public Map<String, Object> getParamMap() {
		return paramMap;
	}
}
