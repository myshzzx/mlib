package mysh.cluster;

import org.junit.Test;

import java.net.SocketException;
import java.rmi.RemoteException;

/**
 * @author Mysh
 * @since 2014/10/12 14:22
 */
public class ClusterMgrTest1 {

	private static final int cmdPort = 8030;

	public static void main(String[] args) throws Exception {
		new ClusterNode(cmdPort, null);
	}

	@Test
	public void t1() throws SocketException, RemoteException, ClusterExcp.Unready, InterruptedException, ClusterExcp.TaskTimeout {
		ClusterClient c = new ClusterClient(cmdPort);
		while (true) {
			System.out.println(c.getWorkerStates(WorkerState.class, 1000, 1000));
			Thread.sleep(3000);
		}
	}
}
