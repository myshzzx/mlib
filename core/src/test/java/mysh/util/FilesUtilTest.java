
package mysh.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static mysh.util.FilesUtil.folderSize;
import static mysh.util.FilesUtil.getFileExtension;
import static mysh.util.FilesUtil.getFileNameWithoutExtension;
import static mysh.util.FilesUtil.writeFile;

public class FilesUtilTest extends Assertions {
	
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
		assertEquals("txt", getFileExtension(Paths.get("fea.txt")));
		assertEquals("txt", getFileExtension(Paths.get("  fe a  .txt")));
		assertEquals("txt", getFileExtension(Paths.get(".txt")));
		assertEquals("txt", getFileExtension(Paths.get(".txt")));
		assertEquals("", getFileExtension(Paths.get("txt")));
		assertEquals("", getFileExtension(Paths.get("txt.")));
		assertEquals("    txt", getFileExtension(Paths.get("     .    txt.")));
		assertEquals("txt", getFileExtension(Paths.get("   abc.def.txt")));
		assertEquals(" t xt", getFileExtension(Paths.get("   abc.def. t xt")));
		assertEquals("", getFileExtension(Paths.get(".")));
		assertEquals("", getFileExtension(Paths.get("")));
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
		
		assertNotNull(new File("").getAbsoluteFile().getParent());
		System.out.println(new File("").getAbsoluteFile().getParent());
		assertNotNull(new File("abc/def").getAbsoluteFile().getParent());
		System.out.println((new File("abc/def").getAbsoluteFile().getParent()));
	}
	
	@Test
	@Disabled
	public void pathTest() throws IOException {
		String file = "pom.xml";
		System.out.println(Paths.get(file).toFile().getAbsolutePath());
		System.out.println(new String(java.nio.file.Files.readAllBytes(Paths.get(file))));
	}
	
	@Test
	@Disabled
	public void writeTest() throws IOException {
		java.nio.file.Files.write(Paths.get("l:/aa/b/c.txt"), "mysh".getBytes(),
				StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}
	
	@Test
	@Disabled
	public void writeTest2() throws IOException {
		writeFile(new File("l:/aa/b/c.txt"), "mysh".getBytes());
	}
	
	
	@Test
	@Disabled
	public void testFolderSize() throws Exception {
		System.out.println(folderSize(new File("l:/temp")));
	}
	
	@Test
	@Disabled
	public void testGetObjectFromFileWithBuf() throws Exception {
		File f = File.createTempFile("test", ".txt");
		String obj = "mysh";
		FilesUtil.writeObjectToFile(f, obj);
		Object obj1 = FilesUtil.getObjectFromFileWithFileMap(f);
		assertEquals(obj, obj1);
	}
}
