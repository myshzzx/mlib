
package mysh.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class FileUtilTest {

	@Test
	public void getFileExtensionTest() {

		assertEquals("txt", FileUtil.getFileExtension("fea.txt"));
		assertEquals("txt", FileUtil.getFileExtension("  fe a  .txt     "));
		assertEquals("txt", FileUtil.getFileExtension(".txt"));
		assertEquals("txt", FileUtil.getFileExtension(".txt       "));
		assertEquals("", FileUtil.getFileExtension("txt"));
		assertEquals("", FileUtil.getFileExtension("txt.    "));
		assertEquals("    txt", FileUtil.getFileExtension("     .    txt.    "));
		assertEquals("txt", FileUtil.getFileExtension("   abc.def.txt     "));
		assertEquals(" t xt", FileUtil.getFileExtension("   abc.def. t xt "));
		assertEquals("", FileUtil.getFileExtension("."));
		assertEquals("", FileUtil.getFileExtension(""));
	}

	@Test
	public void getFileNameWithoutExtensionTest() {

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
