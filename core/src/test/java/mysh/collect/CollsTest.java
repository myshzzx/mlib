package mysh.collect;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static mysh.collect.Colls.ofHashMap;
import static mysh.collect.Colls.ofHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * CollsTest
 *
 * @author mysh
 * @since 2016/3/1
 */
public class CollsTest {
	
	@Test
	public void map() {
		Map<Object, Object> m0 = ofHashMap();
		assertNotNull(m0);
		
		Map<Object, Object> m1 = ofHashMap();
		assertNotNull(m1);
		
		Map<String, Object> m2 = ofHashMap("a", "b");
		assertEquals("b", m2.get("a"));
	}
	
	@Test
	public void list() {
		Set<Object> s0 = ofHashSet();
		assertNotNull(s0);
		assertEquals(0, s0.size());
		
		Set<Object> s1 = ofHashSet();
		assertNotNull(s1);
		
		Set<?> s2 = ofHashSet("a", 3);
		assertEquals(2, s2.size());
	}
	
	@Test
	public void splitBySize() {
		System.out.println(Colls.splitBySize(Arrays.asList(1, 2, 3, 4), 3));
		System.out.println(Colls.splitBySize(Arrays.asList(1, 2, 3, 4), 1));
		System.out.println(Colls.splitBySize(Arrays.asList(1, 2, 3, 4, 5), 2));
	}
}
