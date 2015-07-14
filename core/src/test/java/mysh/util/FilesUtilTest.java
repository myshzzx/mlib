
package mysh.util;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static mysh.util.FilesUtil.*;
import static org.junit.Assert.assertEquals;

public class FilesUtilTest {

	@Test
	public void getFileExtensionTest() {

		assertEquals("txt", getFileExtension("fea.txt"));
		assertEquals("txt", getFileExtension("  fe a  .txt     "));
		assertEquals("txt", getFileExtension(".txt"));
		assertEquals("txt", getFileExtension(".txt       "));
		assertEquals("", getFileExtension("txt"));
		assertEquals("", getFileExtension("txt.    "));
		assertEquals("    txt", getFileExtension("     .    txt.    "));
		assertEquals("txt", getFileExtension("   abc.def.txt     "));
		assertEquals(" t xt", getFileExtension("   abc.def. t xt "));
		assertEquals("", getFileExtension("."));
		assertEquals("", getFileExtension(""));
	}

	@Test
	public void getFileNameWithoutExtensionTest() {

		assertEquals("fea", getFileNameWithoutExtention("fea.txt"));
		assertEquals("fe a  ", getFileNameWithoutExtention("  fe a  .txt     "));
		assertEquals("", getFileNameWithoutExtention(".txt"));
		assertEquals("", getFileNameWithoutExtention(".txt       "));
		assertEquals("txt", getFileNameWithoutExtention("txt"));
		assertEquals("txt", getFileNameWithoutExtention("txt.    "));
		assertEquals("", getFileNameWithoutExtention("     .    txt.    "));
		assertEquals("abc.def", getFileNameWithoutExtention("   abc.def.txt     "));
		assertEquals("abc.def", getFileNameWithoutExtention("   abc.def. t xt "));
		assertEquals("", getFileNameWithoutExtention("."));
		assertEquals("", getFileNameWithoutExtention(""));
	}

	@Test
	public void getWritableFileTest1() {

		Assert.assertNotNull(new File("").getAbsoluteFile().getParent());
		System.out.println(new File("").getAbsoluteFile().getParent());
		Assert.assertNotNull(new File("abc/def").getAbsoluteFile().getParent());
		System.out.println((new File("abc/def").getAbsoluteFile().getParent()));
	}

	@Test
	@Ignore
	public void pathTest() throws IOException {
		String file = "pom.xml";
		System.out.println(Paths.get(file).toFile().getAbsolutePath());
		System.out.println(new String(java.nio.file.Files.readAllBytes(Paths.get(file))));
	}

	@Test
	@Ignore
	public void writeTest() throws IOException {
		java.nio.file.Files.write(Paths.get("l:/aa/b/c.txt"), "mysh".getBytes(),
				StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}

	@Test
	@Ignore
	public void writeTest2() throws IOException {
		writeFile("l:/aa/b/c.txt", "mysh".getBytes());
	}


	@Test
	@Ignore
	public void testFolderSize() throws Exception {
		System.out.println(folderSize("l:/temp"));
	}

	@Test
	@Ignore
	public void testGetObjectFromFileWithBuf() throws Exception {
		File f = File.createTempFile("test", ".txt");
		String obj = "mysh";
		FilesUtil.writeObjectToFile(f.getAbsolutePath(), obj);
		Object obj1 = FilesUtil.getObjectFromFileWithBuf(f.getAbsolutePath());
		Assert.assertEquals(obj, obj1);
	}
}
