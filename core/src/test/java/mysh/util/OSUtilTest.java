package mysh.util;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.Test;

import javax.swing.*;

@Ignore
public class OSUtilTest {

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
		OSUtil.restart();
	}

}
