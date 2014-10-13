package mysh.cluster;

import mysh.util.OSUtil;
import org.junit.Test;

/**
 * @author Mysh
 * @since 2014/10/12 21:32
 */
public class ClusterRestartTest {
	@Test
	public void t1() throws Exception {
		ClusterNode node = new ClusterNode(8030, null);
		Thread.sleep(10000);
		node.shutdownNode();
		Thread.sleep(100000);
		OSUtil.restart();
	}
}
