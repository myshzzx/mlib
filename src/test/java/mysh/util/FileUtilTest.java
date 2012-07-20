
package mysh.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FileUtilTest {

	@Test
	public void getFileExtentionTest() {

		assertEquals("txt", FileUtil.getFileExtention("fea.txt"));
		assertEquals("txt", FileUtil.getFileExtention("  fe a  .txt     "));
		assertEquals("txt", FileUtil.getFileExtention(".txt"));
		assertEquals("txt", FileUtil.getFileExtention(".txt       "));
		assertEquals("txt", FileUtil.getFileExtention("txt"));
		assertEquals("txt", FileUtil.getFileExtention("txt.    "));
		assertEquals("    txt", FileUtil.getFileExtention("     .    txt.    "));
		assertEquals("txt", FileUtil.getFileExtention("   abc.def.txt     "));
		assertEquals(" t xt", FileUtil.getFileExtention("   abc.def. t xt "));
		assertEquals("", FileUtil.getFileExtention("."));
		assertEquals("", FileUtil.getFileExtention(""));
	}

}
