package mysh.codegen;

import mysh.util.Strings;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

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
@SuppressWarnings("unused")
public class SQLHelper {

	// expression handler below ==============================

	/**
	 * col = value
	 */
	public SQLHelper eq(String col, Object value) {
		return bi(col, "=", col, value);
	}

	/**
	 * col = value
	 */
	public SQLHelper eq(String col, String valueName, Object value) {
		return bi(col, "=", valueName, value);
	}

	/**
	 * col != value
	 */
	public SQLHelper notEq(String col, Object value) {
		return bi(col, "!=", col, value);
	}

	/**
	 * col != value
	 */
	public SQLHelper notEq(String col, String valueName, Object value) {
		return bi(col, "!=", valueName, value);
	}

	/**
	 * col like %value%
	 */
	public SQLHelper like(String col, String value) {
		if (ignoreChk(value)) return this;
		return bi(col, "LIKE", col, "%" + value + "%");
	}

	/**
	 * col like %value%
	 */
	public SQLHelper like(String col, String valueName, String value) {
		if (ignoreChk(value)) return this;
		return bi(col, "LIKE", valueName, "%" + value + "%");
	}

	/**
	 * col not like %value%
	 */
	public SQLHelper notLike(String col, String value) {
		if (ignoreChk(value)) return this;
		return bi(col, "NOT LIKE", col, "%" + value + "%");
	}

	/**
	 * col not like %value%
	 */
	public SQLHelper notLike(String col, String valueName, String value) {
		if (ignoreChk(value)) return this;
		return bi(col, "NOT LIKE", valueName, "%" + value + "%");
	}

	/**
	 * col like value%
	 */
	public SQLHelper likeLeft(String col, String value) {
		if (ignoreChk(value)) return this;
		return bi(col, "LIKE", col, value + "%");
	}

	/**
	 * col like value%
	 */
	public SQLHelper likeLeft(String col, String valueName, String value) {
		if (ignoreChk(value)) return this;
		return bi(col, "LIKE", valueName, value + "%");
	}

	/**
	 * col not like value%
	 */
	public SQLHelper notLikeLeft(String col, String value) {
		if (ignoreChk(value)) return this;
		return bi(col, "NOT LIKE", col, value + "%");
	}

	/**
	 * col not like value%
	 */
	public SQLHelper notLikeLeft(String col, String valueName, String value) {
		if (ignoreChk(value)) return this;
		return bi(col, "NOT LIKE", valueName, value + "%");
	}

	/**
	 * col > value
	 */
	public SQLHelper gt(String col, Object value) {
		return bi(col, ">", col, value);
	}

	/**
	 * col > value
	 */
	public SQLHelper gt(String col, String valueName, Object value) {
		return bi(col, ">", valueName, value);
	}

	/**
	 * col >= value
	 */
	public SQLHelper ge(String col, Object value) {
		return bi(col, ">=", col, value);
	}

	/**
	 * col >= value
	 */
	public SQLHelper ge(String col, String valueName, Object value) {
		return bi(col, ">=", valueName, value);
	}

	/**
	 * col < value
	 */
	public SQLHelper lt(String col, Object value) {
		return bi(col, "<", col, value);
	}

	/**
	 * col < value
	 */
	public SQLHelper lt(String col, String valueName, Object value) {
		return bi(col, "<", valueName, value);
	}

	/**
	 * col <= value
	 */
	public SQLHelper le(String col, Object value) {
		return bi(col, "<=", col, value);
	}

	/**
	 * col <= value
	 */
	public SQLHelper le(String col, String valueName, Object value) {
		return bi(col, "<=", valueName, value);
	}

	/**
	 * 二元表达式
	 *
	 * @param col        字段名
	 * @param op         操作符
	 * @param paramValue 参数值
	 */
	public SQLHelper bi(String col, String op, Object paramValue) {
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
	public SQLHelper bi(String col, String op, String paramName, Object paramValue) {
		if (ignoreChk(paramValue)) return this;

		cond.append(" AND ");
		cond.append(autoConvertCol(col));
		cond.append(" ");
		cond.append(op);
		cond.append(" :");
		cond.append(paramName);
		cond.append(" ");
		putParam(paramName, paramValue);
		return this;
	}

	/**
	 * col between from and to
	 */
	public SQLHelper between(String col, Object from, Object to) {
		return betweenExp(true, col, col + "from", from, col + "to", to);
	}

	/**
	 * col between from and to
	 */
	public SQLHelper between(String col, String fromName, Object from, String toName, Object to) {
		return betweenExp(true, col, fromName, from, toName, to);
	}

	/**
	 * col not between from and to
	 */
	public SQLHelper notBetween(String col, Object from, Object to) {
		return betweenExp(false, col, col + "from", from, col + "to", to);
	}

	/**
	 * col not between from and to
	 */
	public SQLHelper notBetween(String col, String fromName, Object from, String toName, Object to) {
		return betweenExp(false, col, fromName, from, toName, to);
	}

	/**
	 * col between from and to
	 *
	 * @param flag is between or not between
	 */
	private SQLHelper betweenExp(boolean flag,
	                             String col, String fromName, Object from, String toName, Object to) {
		if (ignoreChk(from, to)) return this;

		cond.append(" AND ");
		cond.append(autoConvertCol(col));
		cond.append(flag ? " BETWEEN :" : " NOT BETWEEN :");
		cond.append(fromName);
		cond.append(" AND :");
		cond.append(toName);
		cond.append(" ");
		putParam(fromName, from);
		putParam(toName, to);
		return this;
	}

	/**
	 * col is null
	 */
	public SQLHelper isNull(String col) {
		return nullExp(true, col);
	}

	/**
	 * col is not null
	 */
	public SQLHelper isNotNull(String col) {
		return nullExp(false, col);
	}

	private SQLHelper nullExp(boolean flag, String col) {
		if (ignoreChk()) return this;

		cond.append(" AND ");
		cond.append(autoConvertCol(col));
		cond.append(flag ? " IS NULL " : " IS NOT NULL ");

		return this;
	}

	/**
	 * append to current sql directly. you don't need to put a prefix "and".
	 * params should be [paramName, paramValue] pairs.
	 */
	public SQLHelper append(String sql, Object... params) {
		if (ignoreChk(params)) return this;

		cond.append(" AND ");
		cond.append(sql);

		if (params != null) {
			if (params.length % 2 != 0)
				throw new IllegalArgumentException("params should be name-value pairs");
			for (int i = 0; i < params.length; i += 2) {
				putParam((String) params[i], params[i + 1]);
			}
		}
		return this;
	}

	/**
	 * col in (e1, e2)
	 */
	public SQLHelper in(String col, Object... enums) {
		return inExp(true, col, enums);
	}

	/**
	 * col not in (e1, e2)
	 */
	public SQLHelper notIn(String col, Object... enums) {
		return inExp(false, col, enums);
	}

	private SQLHelper inExp(boolean flag, String col, Object[] enums) {
		if (ignoreChk(enums)) return this;

		cond.append(" AND ");
		cond.append(autoConvertCol(col));
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

		return this;
	}

	/**
	 * order by col
	 */
	public SQLHelper orderBy(String col) {
		return orderByExp(true, col);
	}

	/**
	 * order by col desc
	 */
	public SQLHelper orderByDesc(String col) {
		return orderByExp(false, col);
	}

	private SQLHelper orderByExp(boolean flag, String col) {
		if (ignoreChk()) return this;

		cond.append(" ORDER BY ");
		cond.append(autoConvertCol(col));
		cond.append(flag ? " " : " DESC ");

		return this;
	}

	/**
	 * group by col
	 */
	public SQLHelper groupBy(String col) {
		if (ignoreChk()) return this;

		cond.append(" GROUP BY ");
		cond.append(autoConvertCol(col));
		cond.append(" ");

		return this;
	}

	// expression handler above ==============================


	// col convert below =============================
	private String tableAlias;
	private static final WeakHashMap<String, String> hump2UnderlineCols = new WeakHashMap<>();

	/**
	 * set table alias. to clean old alias, use null.
	 */
	public SQLHelper setTableAlias(String alias) {
		tableAlias = Strings.isBlank(alias) ? null : alias.trim() + '.';
		return this;
	}

	/**
	 * auto convert hump col name to underline connected name,
	 * like maxValue -> MAX_VALUE.
	 */
	private String autoConvertCol(String col) {
		String underLine = hump2UnderlineCols.get(col);

		if (underLine == null) {
			for (int i = 0; i < col.length(); i++) {
				if (col.charAt(i) == '_') {
					underLine = col;
					break;
				}
			}

			if (underLine == null)
				underLine = CodeUtil.hump2underline(col);

			if (tableAlias != null)
				underLine = tableAlias + underLine;
			hump2UnderlineCols.put(col, underLine);
			return underLine;
		} else
			return underLine;
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
	public SQLHelper on(boolean condition) {
		ignoreNext = !condition;
		return this;
	}

	/**
	 * same as on(isNotBlank(value). see {@link #on(boolean)}
	 */
	public SQLHelper onNotBlank(String value) {
		ignoreNext = Strings.isBlank(value);
		return this;
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
	 * if previous on-condition is true, or any param is null(or blank), this will return true.
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


	// ignoreParamOverwritten below ==============================

	private boolean ignoreParamOverwritten = false;

	/**
	 * param overwritten will cause a runtime exception by default.
	 */
	public SQLHelper ignoreParamOverwritten() {
		this.ignoreParamOverwritten = true;
		return this;
	}

	private void putParam(String name, Object value) {
		if (!ignoreParamOverwritten && paramMap.containsKey(name)) {
			throw new RuntimeException("param[" + name + "] overwritten with value:" + value);
		}
		paramMap.put(name, value);
	}

	// ignoreParamOverwritten above ==============================


	// create fields below =========================================

	public static SQLHelper create() {
		return create(null, null);
	}

	public static SQLHelper create(Map<String, Object> params) {
		return create(null, params);
	}

	public static SQLHelper create(StringBuilder cond, Map<String, Object> params) {
		if (cond == null) cond = new StringBuilder("1=1 ");
		if (params == null) params = new HashMap<>();
		return new SQLHelper(cond, params);
	}

	private StringBuilder cond;
	private Map<String, Object> paramMap;

	private SQLHelper(StringBuilder cond, Map<String, Object> paramMap) {
		this.cond = cond;
		this.paramMap = paramMap;
	}

	public StringBuilder getCond() {
		return cond;
	}

	public Map<String, Object> getParamMap() {
		return paramMap;
	}
}
