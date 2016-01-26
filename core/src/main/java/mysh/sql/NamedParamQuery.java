package mysh.sql;

import java.util.List;
import java.util.Map;

/**
 * NamedParamQuery
 *
 * @author mysh
 * @since 2015/8/25
 */
public interface NamedParamQuery {

	List<Map<String, Object>> queryForList(String sql, Map<String, ?> params);

	int queryForInt(String sql, Map<String, ?> params);

	long queryForLong(String sql, Map<String, ?> params);
}
