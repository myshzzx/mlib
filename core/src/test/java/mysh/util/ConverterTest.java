package mysh.util;

import lombok.Data;
import mysh.collect.Colls;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author <a href="mailto:zhixian.zzx@alibaba-inc.com">凯泓</a>
 * @since 2021-03-31
 */
class ConverterTest extends Assertions {
	
	@Data
	static class A {
		String userName = "1234";
		int age = 22;
		Integer salary;
	}
	
	@Test
	void object2Map() {
		Map<String, Object> am = Converter.object2Map(new A());
		assertEquals("1234", am.get("userName"));
		System.out.println(am);
	}
	
	@Test
	void map2Object() {
		A a = Converter.map2Object(Colls.ofHashMap("userName", "abc"), A.class);
		assertEquals("abc", a.userName);
		System.out.println(a);
	}
	
	@Test
	void object2Map2() {
		Map<String, Object> am = Converter.object2Map(new A(), true);
		assertEquals("1234", am.get("user_name"));
		System.out.println(am);
	}
	
	@Test
	void map2Object2() {
		A a = Converter.map2Object(Colls.ofHashMap("user_name", "abc"), true, A.class);
		assertEquals("abc", a.userName);
		System.out.println(a);
	}
}