package mysh.cluster;

import mysh.cluster.update.FilesMgr;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Mysh
 * @since 2014/10/12 14:22
 */
public class ClusterMgrTest1 {

	private static final int cmdPort = 8030;

	@Test
	public void getWorkersState1() throws Exception {
		ClusterClient c = new ClusterClient(cmdPort);
		final Map<String, WorkerState> workerStates = c.mgrGetWorkerStates(WorkerState.class);
		System.out.println(workerStates);
	}

	@Test
	public void cancelTask1() throws Exception {
		ClusterClient c = new ClusterClient(cmdPort);
		c.mgrCancelTask(1);
	}

	@Test
	public void restart1() throws Exception {
		ClusterClient c = new ClusterClient(cmdPort);
		c.mgrRestartMaster();
	}

	@Test
	public void shutdown1() throws Exception {
		ClusterClient c = new ClusterClient(cmdPort);
		c.mgrShutdownNodes(null);
	}

	@Test
	public void shutdown2() throws Exception {
		ClusterClient c = new ClusterClient(cmdPort);
		c.mgrShutdownNodes(Arrays.asList("cn_/192.168.80.130_1418572712444", "cn_/169.254.154.173_1418572714483"));
	}

	@Test
	public void update1() throws Exception {
		ClusterClient c = new ClusterClient(8030);

		byte[] ctx = Files.readAllBytes(Paths.get("l:", "a.jar"));
		c.mgrUpdateFile(FilesMgr.UpdateType.UPDATE, FilesMgr.FileType.CORE, "a.jar", ctx);

//		c.runTask(new CU1(), null, 0, 0);
	}
}
