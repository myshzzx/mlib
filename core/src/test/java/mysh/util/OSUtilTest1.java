package mysh.util;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.Test;

import javax.swing.*;
import java.util.Arrays;
import java.util.StringTokenizer;

@Ignore
public class OSUtilTest1 {

	@Test
	public void testGetOS() throws Exception {
		System.out.println(OSUtil.getOS());
	}

	@Test
	public void testGetPid() throws Exception {
		System.out.println(OSUtil.getPid());
	}


	@Test
	public void testGetCmdLine() throws Exception {
		System.out.println(OSUtil.getCmdLine());
	}

	@Test
	public void testRestart() throws Exception {
		JOptionPane.showMessageDialog(null, "testRestart.pid=" + OSUtil.getPid());
		Thread.sleep(5000);
		OSUtil.restart(true);
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


}
