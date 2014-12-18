package mysh.cluster;

import mysh.cluster.ClusterClient.SRTarget;
import mysh.cluster.ClusterClient.SRType;
import mysh.cluster.update.FilesMgr.FileType;
import mysh.cluster.update.FilesMgr.UpdateType;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Mysh
 * @since 2014/10/12 14:22
 */
public class ClusterMgrTest1 {

	private static final int cmdPort = 8030;

	@Test
	public void getWorkersState1() throws Throwable {
		ClusterClient c = new ClusterClient(cmdPort);
		final Map<String, WorkerState> workerStates = c.mgrGetWorkerStates(WorkerState.class);
		System.out.println(workerStates);
	}

	@Test
	public void cancelTask1() throws Throwable {
		ClusterClient c = new ClusterClient(cmdPort);
		c.mgrCancelTask(1);
	}

	@Test
	public void restart1() throws Throwable {
		ClusterClient c = new ClusterClient(cmdPort);
		c.mgrShutdownRestart(SRType.Restart, SRTarget.EntireCluster, null);
	}

	@Test
	public void shutdown1() throws Throwable {
		ClusterClient c = new ClusterClient(cmdPort);
		c.mgrShutdownRestart(SRType.Shutdown, SRTarget.EntireCluster, null);
	}

	@Test
	public void shutdown2() throws Throwable {
		ClusterClient c = new ClusterClient(cmdPort);
		c.mgrShutdownRestart(SRType.Shutdown, SRTarget.Specified,
						Arrays.asList("cn_/192.168.80.130_1418572712444", "cn_/169.254.154.173_1418572714483"));
	}

	@Test
	public void update1() throws Throwable {
		ClusterClient c = new ClusterClient(8030);

		List<ClusterClient.UpdateFile> ufs = new ArrayList<>();
		ufs.add(new ClusterClient.UpdateFile(UpdateType.UPDATE, "a.jar", new File("l:/a.jar")));

		c.mgrUpdateFile(FileType.CORE, ufs);

//		c.runTask(new CU1(), null, 0, 0);
	}

	@Test
	public void update2() throws Throwable {
		ClusterClient c = new ClusterClient(8030);

		List<ClusterClient.UpdateFile> ufs = new ArrayList<>();
		ufs.add(new ClusterClient.UpdateFile(UpdateType.DELETE, "a.jar", new File("l:/a.jar")));

		c.mgrUpdateFile(FileType.USER, ufs);

	}

}
