package mysh.sql;

import com.google.common.collect.ImmutableMap;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * SqlRepoTest
 *
 * @author mysh
 * @since 2016/1/22
 */
@Ignore
public class SqlRepoTest {

	@Autowired
	SqlRepo sqlRepo;

	@Test
	public void sqlRepoFetchConf() throws Exception {
		TableCols.UmOperatorCols t = TableCols.UmOperatorCols;
		SqlCondition cond = new SqlCondition()
						.isNotNull(t.operatorId)
						.page(1, 10);

		List<Map<String, Object>> r = sqlRepo.fetchByConfig(
						"sqlRepoDemo.query", ImmutableMap.of("id", 1), cond);
		System.out.println(r);

		List<UmOperator> r2 = sqlRepo.fetchByConfig(
						"sqlRepoDemo.query", ImmutableMap.of("id", 1), cond, UmOperator.class);
		System.out.println(r2);
	}

	@Test
	public void sqlRepoFetchSql() throws Exception {
		TableCols.UmOperatorCols t = TableCols.UmOperatorCols;
		SqlCondition cond = new SqlCondition()
						.isNotNull(t.operatorId)
						.page(1, 10);

		List<Map<String, Object>> r = sqlRepo.fetchBySql(
						"select * from UM_OPERATOR where OPERATOR_ID=:id", ImmutableMap.of("id", 1), cond);
		System.out.println(r);

		List<UmOperator> r2 = sqlRepo.fetchBySql(
						"select * from UM_OPERATOR where OPERATOR_ID=:id", ImmutableMap.of("id", 1),
						cond, UmOperator.class);
		System.out.println(r2);
	}

}
