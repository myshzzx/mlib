package mysh.cluster;

import java.util.List;
import java.util.Objects;

/**
 * update cluster config.
 * only affects current running cluster nodes, and one node needs restart to make the new config work.
 *
 * @author Mysh
 * @since 2014/12/21 23:38
 */
final class MgrUpdateConf extends IClusterMgr<String, String, String, String> {
	private static final long serialVersionUID = 5113614870089299846L;

	private ClusterConf conf;

	public MgrUpdateConf(ClusterConf conf) {
		Objects.requireNonNull(conf);
		this.conf = conf;
	}

	@Override
	public SubTasksPack<String> fork(String task, String masterNode, List<String> workerNodes) {
		return pack(new String[workerNodes.size()], workerNodes.toArray(new String[workerNodes.size()]));
	}

	@Override
	public Class<String> getSubResultType() {
		return String.class;
	}

	@Override
	public String procSubTask(String subTask, int timeout) throws InterruptedException {
		ClusterConf currentConf = ClusterConf.readConf();
		if (conf.cmdPort > 0)
			currentConf.cmdPort = conf.cmdPort;
		if (conf.heartBeatTime > 0)
			currentConf.heartBeatTime = conf.heartBeatTime;
		if (conf.serverPoolSize > 0)
			currentConf.serverPoolSize = conf.serverPoolSize;
		currentConf.save();
		return "";
	}

	@Override
	public String join(String masterNode, String[] assignedNodeIds, String[] subResults) {
		return null;
	}

	@Override
	public String toString() {
		return "MgrUpdateConf{" +
						"conf=" + conf +
						'}';
	}
}
