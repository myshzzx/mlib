package mysh.codegen;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SQLHelperTest {

	@Test
	public void all() {
		SQLHelper sql = SQLHelper.create();
		sql
						.bi("givenName", "=", "mysh")
						.bi("name", "!=", "name2", "zzx")
						.like("title", "sde")
						.likeLeft("title", "title22", "sd")
						.notLike("title", "title2", "ste")
						.notLikeLeft("title", "title3", "st")
						.setTableAlias("tt")
						.in("seat", 1, "ab")
						.notIn("set", 2)
						.append("cc > :cc ", "cc", 100)
						.between("height", 170, 180)
						.notBetween("height", "hf", 10, "ht", 21)
						.isNull("officeAddress")
						.isNotNull("HomeAdd")
						.orderBy("createTime")
						.groupBy("name");

		assertEquals("1=1  AND GIVEN_NAME = :givenName  AND NAME != :name2  AND TITLE LIKE " +
										":title  AND TITLE LIKE :title22  AND TITLE NOT LIKE :title2  AND TITLE NOT LIKE :title3  AND tt.SEAT IN (1,'ab')  AND tt.SET NOT IN (2)  AND cc > :cc  AND tt.HEIGHT BETWEEN :heightfrom AND :heightto  AND tt.HEIGHT NOT BETWEEN :hf AND :ht  AND tt.OFFICE_ADDRESS IS NULL  AND tt.HOME_ADD IS NOT NULL  ORDER BY tt.CREATE_TIME  GROUP BY NAME ",
						sql.getCond().toString());
		assertEquals("{cc=100, givenName=mysh, title2=%ste%, heightto=180, title3=st%, name2=zzx, " +
						"title=%sde%, title22=sd%, heightfrom=170, ht=21, hf=10}", sql.getParams().toString());
	}

	@Test(expected = RuntimeException.class)
	public void paramOverwriteTest() {
		SQLHelper sql = SQLHelper.create();
		sql.eq("name", 2)
						.like("name", "abc");
	}

	@Test
	public void conditionTest() {
		SQLHelper sql = SQLHelper.create();
		sql
						.on(2 > 1).eq("name", "abc")
						.on(2 < 1).bi("age", ">", 10)
						.on(2 < 1).groupBy("age");
		assertEquals("1=1  AND NAME = :name ", sql.getCond().toString());
		assertEquals("{name=abc}", sql.getParams().toString());
	}

}
