package mysh.util;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mysh
 * @since 14-3-13 下午5:37
 */
@Ignore
public class SensitiveWordsTest {

	private static SensitiveWords sw = new SensitiveWords();

	@BeforeClass
	public static void init() throws Exception {
		Arrays.asList("小泽玛莉亚", "武腾兰", "三級").forEach(sw::insert);
	}

	@Test
	public void blankTest() {
		assertTrue(sw.isBlankChar('。'));
		assertTrue(sw.isBlankChar('，'));
		assertTrue(sw.isBlankChar('"'));
		assertTrue(sw.isBlankChar('\''));
		assertTrue(sw.isBlankChar('“'));
		assertTrue(sw.isBlankChar('’'));
		assertTrue(sw.isBlankChar('.'));
		assertTrue(sw.isBlankChar(','));
		assertTrue(sw.isBlankChar(' '));
		assertTrue(sw.isBlankChar('．'));
		assertTrue(sw.isBlankChar('。'));
	}

	@Test
	public void t1() {
		assertEquals(6, sw.contains("fsdasf三級 武腾兰".toCharArray()));
	}

	@Test
	public void t2() {
		assertEquals(-1, sw.contains("fsdasf sdf s feiff".toCharArray()));
	}

	@Test
	public void t3() {
		assertEquals(0, sw.contains("三級 绝版 武腾兰".toCharArray()));
	}

	@Test
	public void t4() {
		assertEquals(5, sw.contains("abs绝 三級 三級片  武腾兰".toCharArray()));
	}

	@Test
	public void t5() {
		assertEquals(0, sw.contains("武腾兰".toCharArray()));
	}

	@Test
	public void t6() {
		assertEquals(0, sw.contains("武腾兰165".toCharArray()));
	}

	@Test
	public void t7() {
		assertEquals(-1, sw.contains("武腾165".toCharArray()));
	}

	@Test
	public void blank1() {
		assertEquals(0, sw.contains("武腾  兰".toCharArray()));
	}

	@Test
	public void blank2() {
		assertEquals(0, sw.contains("武腾   兰  ".toCharArray()));
	}

	@Test
	public void blank3() {
		assertEquals(-1, sw.contains("武腾    兰 赫本 ".toCharArray()));
	}

	@Test
	public void blank4() {
		assertEquals(0, sw.contains("武  腾   兰 载 ".toCharArray()));
	}

	@Test
	public void blank5() {
		assertEquals(2, sw.contains("  武  腾   兰地工  ".toCharArray()));
	}

	@Test
	public void blank6() {
		assertEquals(8, sw.contains(" 发发牢骚地  武 , 腾。 兰   ".toCharArray()));
	}
}
