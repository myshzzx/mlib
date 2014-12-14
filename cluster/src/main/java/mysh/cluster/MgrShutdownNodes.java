package mysh.cluster;

import java.util.List;

/**
 * @author Mysh
 * @since 2014/12/14 19:28
 */
public class MgrShutdownNodes extends IClusterMgr<String, String, String, String> {

	private static final long serialVersionUID = 6411057795287576274L;

	private transient List<String> nodes2Close;
	private transient boolean closeMaster;

	public MgrShutdownNodes(List<String> nodes2Close) {
		this.nodes2Close = nodes2Close;
	}

	@Override
	public SubTasksPack<String> fork(String task, String masterNode, List<String> workerNodes) {
		if (nodes2Close == null) {
			workerNodes.remove(masterNode);
			closeMaster = true;
			return pack(new String[workerNodes.size()], workerNodes.toArray(new String[workerNodes.size()]));
		} else {
			closeMaster = nodes2Close.remove(masterNode);
			return pack(new String[nodes2Close.size()], nodes2Close.toArray(new String[nodes2Close.size()]));
		}
	}

	@Override
	public Class<String> getSubResultType() {
		return null;
	}

	@Override
	public String procSubTask(String subTask, int timeout) throws InterruptedException {
		worker.shutdownVM(false);
		return "";
	}

	@Override
	public String join(String masterNode, String[] assignedNodeIds, String[] subResults) {
		if (closeMaster)
			master.shutdownVM(false);
		return null;
	}
}
