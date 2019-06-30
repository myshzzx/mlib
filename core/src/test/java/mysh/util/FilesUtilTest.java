
package mysh.util;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static mysh.util.FilesUtil.*;
import static org.junit.Assert.assertEquals;

public class FilesUtilTest {
	
	@Test
	public void getFileExtensionTest() {
		assertEquals("txt", getFileExtension(new File("fea.txt")));
		assertEquals("txt", getFileExtension(new File("  fe a  .txt     ")));
		assertEquals("txt", getFileExtension(new File(".txt")));
		assertEquals("txt", getFileExtension(new File(".txt       ")));
		assertEquals("", getFileExtension(new File("txt")));
		assertEquals("", getFileExtension(new File("txt.    ")));
		assertEquals("    txt", getFileExtension(new File("     .    txt.    ")));
		assertEquals("txt", getFileExtension(new File("   abc.def.txt     ")));
		assertEquals(" t xt", getFileExtension(new File("   abc.def. t xt ")));
		assertEquals("", getFileExtension(new File(".")));
		assertEquals("", getFileExtension(new File("")));
	}
	
	@Test
	public void getFileExtensionTest2() {
		assertEquals("txt", getFileExtension(Path.of("fea.txt")));
		assertEquals("txt", getFileExtension(Path.of("  fe a  .txt")));
		assertEquals("txt", getFileExtension(Path.of(".txt")));
		assertEquals("txt", getFileExtension(Path.of(".txt")));
		assertEquals("", getFileExtension(Path.of("txt")));
		assertEquals("", getFileExtension(Path.of("txt.")));
		assertEquals("    txt", getFileExtension(Path.of("     .    txt.")));
		assertEquals("txt", getFileExtension(Path.of("   abc.def.txt")));
		assertEquals(" t xt", getFileExtension(Path.of("   abc.def. t xt")));
		assertEquals("", getFileExtension(Path.of(".")));
		assertEquals("", getFileExtension(Path.of("")));
	}
	
	@Test
	public void getFileNameWithoutExtensionTest() {
		
		assertEquals("fea", getFileNameWithoutExtension(new File("fea.txt")));
		assertEquals("fe a  ", getFileNameWithoutExtension(new File("  fe a  .txt     ")));
		assertEquals("", getFileNameWithoutExtension(new File(".txt")));
		assertEquals("", getFileNameWithoutExtension(new File(".txt       ")));
		assertEquals("txt", getFileNameWithoutExtension(new File("txt")));
		assertEquals("txt", getFileNameWithoutExtension(new File("txt.    ")));
		assertEquals("", getFileNameWithoutExtension(new File("     .    txt.    ")));
		assertEquals("abc.def", getFileNameWithoutExtension(new File("   abc.def.txt     ")));
		assertEquals("abc.def", getFileNameWithoutExtension(new File("   abc.def. t xt ")));
		assertEquals("", getFileNameWithoutExtension(new File(".")));
		assertEquals("", getFileNameWithoutExtension(new File("")));
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
		writeFile(new File("l:/aa/b/c.txt"), "mysh".getBytes());
	}
	
	
	@Test
	@Ignore
	public void testFolderSize() throws Exception {
		System.out.println(folderSize(new File("l:/temp")));
	}
	
	@Test
	@Ignore
	public void testGetObjectFromFileWithBuf() throws Exception {
		File f = File.createTempFile("test", ".txt");
		String obj = "mysh";
		FilesUtil.writeObjectToFile(f, obj);
		Object obj1 = FilesUtil.getObjectFromFileWithFileMap(f);
		Assert.assertEquals(obj, obj1);
	}
}
