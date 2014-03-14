package mysh.util;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mysh
 * @since 14-3-13 下午5:37
 */
@Ignore
public class SensitiveWordTest {

	public static class SensitiveWord {
		boolean isEnd = false;
		Map<Character, SensitiveWord> surfix = new HashMap<>();

		public void insert(char[] word) {
			SensitiveWord t = this, tNew = null;
			for (char w : word) {
				tNew = t.surfix.get(w);
				if (tNew == null) {
					tNew = new SensitiveWord();
					t.surfix.put(w, tNew);
				}
				t = tNew;
			}
			if (tNew != null) tNew.isEnd = true;
		}

		public int contains(char[] content) {
			GO:
			for (int i = 0; i < content.length; i++) {
				SensitiveWord t = this;
				for (int j = i; j < content.length; j++) {
					if (t.isEnd) return i;
					t = t.surfix.get(content[j]);
					if (t == null) continue GO;
				}
				if (t.surfix.size() == 0) return i;
			}

			return -1;
		}

		public SensitiveWord() {
		}
	}

	private static SensitiveWord sw = new SensitiveWord();

	@BeforeClass
	public static void init() throws Exception {
		System.out.println("init");
		try (BufferedReader r = new BufferedReader(new FileReader("target/test-classes/sensitiveWords.txt"))) {
			String line;
			while ((line = r.readLine()) != null) {
				sw.insert(line.toCharArray());
			}
		}
		System.out.println("init over");
		Thread.sleep(3000000);
	}

	@Test
	public void t1() {
		Assert.assertEquals(6, sw.contains("fsdasf绝版 三級 武腾兰".toCharArray()));
	}

	@Test
	public void t2() {
		Assert.assertEquals(-1, sw.contains("fsdasf sdf s feiff".toCharArray()));
	}

	@Test
	public void t3() {
		Assert.assertEquals(0, sw.contains("绝版 三級 武腾兰".toCharArray()));
	}

	@Test
	public void t4() {
		Assert.assertEquals(5, sw.contains("abs绝 三級 三級片  武腾兰".toCharArray()));
	}

	@Test
	public void t5() {
		Assert.assertEquals(0, sw.contains("武腾兰".toCharArray()));
	}

	@Test
	public void t6() {
		Assert.assertEquals(0, sw.contains("武腾兰165".toCharArray()));
	}

	@Test
	public void t7() {
		Assert.assertEquals(-1, sw.contains("武腾165".toCharArray()));
	}
}
