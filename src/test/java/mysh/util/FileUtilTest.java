
package mysh.util;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class FileUtilTest {

	@Test
	public void getFileExtentionTest() {

		assertEquals("txt", FileUtil.getFileExtention("fea.txt"));
		assertEquals("txt", FileUtil.getFileExtention("  fe a  .txt     "));
		assertEquals("txt", FileUtil.getFileExtention(".txt"));
		assertEquals("txt", FileUtil.getFileExtention(".txt       "));
		assertEquals("", FileUtil.getFileExtention("txt"));
		assertEquals("", FileUtil.getFileExtention("txt.    "));
		assertEquals("    txt", FileUtil.getFileExtention("     .    txt.    "));
		assertEquals("txt", FileUtil.getFileExtention("   abc.def.txt     "));
		assertEquals(" t xt", FileUtil.getFileExtention("   abc.def. t xt "));
		assertEquals("", FileUtil.getFileExtention("."));
		assertEquals("", FileUtil.getFileExtention(""));
	}

	@Test
	public void getFileNameWithoutExtentionTest() {

		assertEquals("fea", FileUtil.getFileNameWithoutExtention("fea.txt"));
		assertEquals("fe a  ", FileUtil.getFileNameWithoutExtention("  fe a  .txt     "));
		assertEquals("", FileUtil.getFileNameWithoutExtention(".txt"));
		assertEquals("", FileUtil.getFileNameWithoutExtention(".txt       "));
		assertEquals("txt", FileUtil.getFileNameWithoutExtention("txt"));
		assertEquals("txt", FileUtil.getFileNameWithoutExtention("txt.    "));
		assertEquals("", FileUtil.getFileNameWithoutExtention("     .    txt.    "));
		assertEquals("abc.def", FileUtil.getFileNameWithoutExtention("   abc.def.txt     "));
		assertEquals("abc.def", FileUtil.getFileNameWithoutExtention("   abc.def. t xt "));
		assertEquals("", FileUtil.getFileNameWithoutExtention("."));
		assertEquals("", FileUtil.getFileNameWithoutExtention(""));
	}

	@Test
	public void getWritableFileTest1() {

		Assert.assertNotNull(new File("").getAbsoluteFile().getParent());
		System.out.println(new File("").getAbsoluteFile().getParent());
		Assert.assertNotNull(new File("abc/def").getAbsoluteFile().getParent());
		System.out.println((new File("abc/def").getAbsoluteFile().getParent()));
	}
}
