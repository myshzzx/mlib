package mysh.util;

import mysh.os.Oss;
import org.junit.Ignore;
import org.junit.Test;

import javax.swing.*;
import java.util.Arrays;
import java.util.StringTokenizer;

import static org.junit.Assert.assertEquals;

@Ignore
public class OSUtilTest1 {

	@Test
	public void testGetOS() throws Exception {
		System.out.println(Oss.getOS());
	}

	@Test
	public void testGetPid() throws Exception {
		System.out.println(Oss.getPid());
	}


	@Test
	public void testGetCmdLine() throws Exception {
		System.out.println(Oss.getCmdLine());
	}

	@Test
	public void testRestart() throws Exception {
		JOptionPane.showMessageDialog(null, "testRestart.pid=" + Oss.getPid());
		Thread.sleep(5000);
		Oss.restart(true);
	}

	@Test
	public void testCmdParse() {
		String cmd = "java -cp /abc/a.jar -Dfile.encoding=utf-8 MyClass 'value 1' \"value 2\"";
		StringTokenizer st = new StringTokenizer(cmd);
		String[] cmdArray = new String[st.countTokens()];
		for (int i = 0; st.hasMoreTokens(); i++)
			cmdArray[i] = st.nextToken();
		System.out.println(Arrays.toString(cmdArray));
	}


	@Test
	public void testParseCmdLine() throws Exception {
		assertEquals(Arrays.asList("p1", "p2", "p3"), Oss.parseCmdLine("p1 p2 p3"));
		assertEquals(Arrays.asList("p1", "p2", "p3"), Oss.parseCmdLine(" p1 p2 p3"));
		assertEquals(Arrays.asList("p1", "p2", "p3"), Oss.parseCmdLine("p1 p2 p3 "));
		assertEquals(Arrays.asList("p1", "p2", "p3"), Oss.parseCmdLine("p1 'p2' p3 "));
		assertEquals(Arrays.asList("p1", "p2  p3"), Oss.parseCmdLine("   p1  'p2  p3' "));
		assertEquals(Arrays.asList("p1", "p2  p3"), Oss.parseCmdLine("   p1  \"p2  p3\" "));
		assertEquals(Arrays.asList("p1", "p2 \\ p3"), Oss.parseCmdLine("   p1  \"p2 \\ p3\" "));
		assertEquals(Arrays.asList("p1", "p2 ' p3"), Oss.parseCmdLine("   p1  \"p2 ' p3\" "));
		assertEquals(Arrays.asList("p1", "p2 \" p3"), Oss.parseCmdLine("   p1  'p2 \" p3' "));
		assertEquals(Arrays.asList("p1", "p2 \" p3"), Oss.parseCmdLine("   p1  \"p2 \\\" p3\" "));
		assertEquals(Arrays.asList("p1", "p2 ' p3"), Oss.parseCmdLine("   p1  \"p2 \\' p3\" "));
		assertEquals(Arrays.asList("p1", "p2 ' p3"), Oss.parseCmdLine("   p1  \"p2 ' p3 "));
		assertEquals(Arrays.asList("p1", "p2 ' p3 2"), Oss.parseCmdLine("   p1  \"p2 \\' p3 2"));
		assertEquals(Arrays.asList("  p1  \"p2 ' p3\""), Oss.parseCmdLine(" '  p1  \"p2 \\' p3\"' "));
	}
}
