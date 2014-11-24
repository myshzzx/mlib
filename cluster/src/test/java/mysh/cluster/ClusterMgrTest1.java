package mysh.cluster;

import org.junit.Test;

import java.net.SocketException;

/**
 * @author Mysh
 * @since 2014/10/12 14:22
 */
public class ClusterMgrTest1 {

	private static final int cmdPort = 8030;

	@Test
	public void cancelTask1() throws InterruptedException, ClusterExp, SocketException {
		ClusterClient c = new ClusterClient(cmdPort);
		c.cancelTask(1);
	}

}
