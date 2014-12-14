package mysh.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Mysh
 * @since 2014/12/13 17:07
 */
final class MgrRestartCluster extends IClusterMgr<String, String, String, String> {
	private static final long serialVersionUID = 6988843023468603974L;
	private static final Logger log = LoggerFactory.getLogger(MgrRestartCluster.class);

	@Override
	public SubTasksPack<String> fork(String task, String masterNode, List<String> workerNodes) {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				master.restartNode();
			}
		}, 30_000);
		workerNodes.remove(masterNode);
		return pack(new String[workerNodes.size()], workerNodes.toArray(new String[workerNodes.size()]));
	}

	@Override
	public Class<String> getSubResultType() {
		return String.class;
	}

	@Override
	public String procSubTask(String subTask, int timeout) throws InterruptedException {
		log.info("worker node is going to restart.");
		worker.restartNode();
		return "";
	}

	@Override
	public String join(String masterNode, String[] assignedNodeIds, String[] subResults) {
		log.info("master node is going to restart.");
		master.restartNode();
		return null;
	}

}
