package mysh.cluster;

import org.junit.Test;

import java.io.Serializable;
import java.net.SocketException;

/**
 * @author Mysh
 * @since 2014/10/12 14:22
 */
public class ClusterMgrTest1 implements Serializable {

	private static final int cmdPort = 8030;

	public static void main(String[] args) throws Exception {
		new ClusterNode(cmdPort, null, 0);
	}

	@Test
	public void t1() throws InterruptedException, ClusterExp, SocketException {
		ClusterClient c = new ClusterClient(cmdPort);
		c.runTask(new IClusterMgr() {
			private static final long serialVersionUID = 7356463428490738417L;

			@Override
			public SubTasksPack<Object> fork(Object task, int workerNodeCount) {
				master.cancelTask(1);
				return pack(new Object[1], null);
			}

			@Override
			public Class<Object> getSubResultType() {
				return Object.class;
			}

			@Override
			public Object procSubTask(Object subTask, int timeout) throws InterruptedException {
				return "";
			}

			@Override
			public Object join(Object[] subResults, String[] assignedNodeIds) {
				return null;
			}
		}, null, 0, 0);

		Thread.sleep(1000000000);
	}
}
