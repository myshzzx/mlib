package mysh.sql;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * SqlHelperTest
 *
 * @author mysh
 * @since 2016/1/22
 */
@Ignore
public class SqlHelperTest {

	@Autowired
	NamedParameterJdbcTemplate jdbcTemplate;

	@Test
	public void dynamicSqlRetTable() throws Exception {
		TableCols.UmOrgCols t = TableCols.UmOrgCols;
		List<UmOrg> r = SqlHelper
						.create(jdbcTemplate, "select * from um_org where 1=1 ")
						.eq(t.orgId, 324)
						.orderBy(t.createTime)
						.page(1, 10) // 分页
						.fetch(UmOrg.class);
		System.out.println(r);
	}

	private static final DateFormat dfFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Test
	public void dynamicSqlRetMap() throws Exception {
		List<Map<String, Object>> r = SqlHelper
						.create(jdbcTemplate, "select * from um_org  ")
						.page(1, 10)
						.onResult(item -> item.entrySet().stream()
										.filter(e -> e.getValue() instanceof Date)
										.forEach(e -> e.setValue(dfFull.format(e.getValue())))) // 将时间值转成字符串
						.fetch();
		System.out.println(r);
	}

	@Test
	@Ignore
	public void dynamicSqlCache() throws Exception {
		TableCols.UmOrgCols t = TableCols.UmOrgCols;
		while (true) {
			List<Map<String, Object>> r = SqlHelper
							.create(jdbcTemplate, "SELECT * FROM UM_ORG where 1=1")
							.eq(t.orgId, 324)
							.cacheLevel(SqlHelper.CacheLevel.M5) // 结果缓存5分钟
							.fetch();
			System.out.println(r);

			Thread.sleep(5000);
		}
	}

	public static class MultiTable extends UmOrg implements Serializable {
		private static final long serialVersionUID = 6800726618985569194L;
		UmOperator op;
	}

	@Test
	public void dynamicSqlMultiTable() throws Exception {
		List<MultiTable> r = SqlHelper
						.create(jdbcTemplate, "SELECT o.*,op.operator_code FROM UM_ORG o ,UM_OPERATOR op WHERE o.ORG_ID=op.ORG_ID")
						.page(1, 10)
						.fetch(MultiTable.class); // 多表聚合类
		System.out.println(r);
	}

	@Test
	public void dynamicSqlCond() throws Exception {
		TableCols.UmOrgCols t = TableCols.UmOrgCols;
		SqlCondition cond = new SqlCondition()
						.eq(t.orgId, 324);

		List<Map<String, Object>> r = SqlHelper
						.create(jdbcTemplate, "select * from um_org where 1=1")
						.appendCond(cond) // 外部条件
						.page(1, 10)
						.fetch();
		System.out.println(r);
	}
}
