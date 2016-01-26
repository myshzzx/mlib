package mysh.sql;

import mysh.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SqlRepo
 *
 * @author mysh
 * @since 2016/1/21
 */
public class SqlRepo {
	private static final Logger log = LoggerFactory.getLogger(SqlRepo.class);

	private final Map<String, String> repo = new HashMap<>();
	private NamedParamQueryImpl jdbc;

	public void setMapperLocations(Resource[] locations) throws IOException {
		if (locations != null) {
			for (Resource res : locations) {
				InputStream is = res.getInputStream();
				loadSqlFile(is);
			}
		}
	}

	private void loadSqlFile(InputStream is) throws IOException {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
			String line, id = null;
			StringBuilder sb = new StringBuilder();
			while (true) {
				line = in.readLine();
				if (line != null) line = line.trim();

				if (line == null || line.startsWith("--!")) { // sql sep
					if (id != null) {
						if (sb.charAt(sb.length() - 1) == ';')
							sb.deleteCharAt(sb.length() - 1);
						String sql = sb.toString();
						String old = repo.put(id, sql);
						if (old != null)
							throw new RuntimeException("duplicated sql: " + id);
						log.debug("repo-put: [" + id + "] " + sql);
					}

					if (line == null) return;
					if (Strings.isBlank(line)) continue;
					id = line.substring(3).trim();
				} else if (!line.startsWith("--")) { // non comment
					int commentIdx = line.indexOf("--");
					if (commentIdx > -1)
						line = line.substring(0, commentIdx).trim();
					if (line.length() > 0)
						sb.append(' ').append(line);
				}
			}
		}
	}

	@PostConstruct
	public void validate() {
		Objects.requireNonNull(jdbc, "jdbc template should not be null");
	}

	/**
	 * 从配置的 sql 创建 SqlHelper
	 *
	 * @param sqlId 配置的 sql 语句 id
	 */
	SqlHelper create(String sqlId) {
		String querySql = repo.get(sqlId);
		if (querySql == null)
			throw new RuntimeException(sqlId + " undefined");
		return SqlHelper.create(jdbc, querySql);
	}

	/**
	 * 从 sql 配置取查询结果.
	 *
	 * @param sqlId  配置的 sql 语句 id
	 * @param params sql 参数, 可 null
	 * @param cond   外部条件, 可 null
	 */
	public List<Map<String, Object>> fetchByConfig(String sqlId, Map<String, ?> params,
	                                               SqlCondition cond) throws Exception {
		SqlHelper sqlHelper = create(sqlId).appendParams(params).appendCond(cond);
		return sqlHelper.fetch();
	}

	/**
	 * 从 sql 配置取查询结果.
	 *
	 * @param sqlId  配置的 sql 语句 id
	 * @param params sql 参数, 可 null
	 * @param cond   外部条件, 可 null
	 * @param type   返回结果封装类型
	 */
	public <T> List<T> fetchByConfig(String sqlId, Map<String, ?> params,
	                                 SqlCondition cond, Class<T> type) throws Exception {
		SqlHelper sqlHelper = create(sqlId).appendParams(params).appendCond(cond);
		return sqlHelper.fetch(type);
	}

	/**
	 * 从 sql 取查询结果.
	 *
	 * @param sql    sql 语句
	 * @param params sql 参数, 可 null
	 * @param cond   外部条件, 可 null
	 */
	public List<Map<String, Object>> fetchBySql(String sql, Map<String, ?> params,
	                                            SqlCondition cond) throws Exception {
		SqlHelper sqlHelper = new SqlHelper(this.jdbc, new StringBuilder(sql), null)
						.appendParams(params).appendCond(cond);
		return sqlHelper.fetch();
	}

	/**
	 * 从 sql 取查询结果.
	 *
	 * @param sql    sql 语句
	 * @param params sql 参数, 可 null
	 * @param cond   外部条件, 可 null
	 * @param type   返回结果封装类型
	 */
	public <T> List<T> fetchBySql(String sql, Map<String, ?> params,
	                              SqlCondition cond, Class<T> type) throws Exception {
		SqlHelper sqlHelper = new SqlHelper(this.jdbc, new StringBuilder(sql), null)
						.appendParams(params).appendCond(cond);
		return sqlHelper.fetch(type);
	}

	public void setJdbc(NamedParamQueryImpl jdbc) {
		this.jdbc = jdbc;
	}
}
