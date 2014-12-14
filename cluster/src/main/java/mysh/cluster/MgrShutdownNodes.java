package mysh.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mysh
 * @since 2014/12/14 19:28
 */
public class MgrShutdownNodes extends IClusterMgr<String, String, String, String> {
	private static final long serialVersionUID = 6411057795287576274L;
	private static final Logger log = LoggerFactory.getLogger(MgrShutdownNodes.class);

	private List<String> nodes2Shutdown;
	private transient boolean closeMaster;

	public MgrShutdownNodes(List<String> nodes2Shutdown) {
		this.nodes2Shutdown = nodes2Shutdown;
	}

	@Override
	public SubTasksPack<String> fork(String task, String masterNode, List<String> workerNodes) {
		if (nodes2Shutdown == null) {
			workerNodes.remove(masterNode);
			closeMaster = true;
			return pack(new String[workerNodes.size()], workerNodes.toArray(new String[workerNodes.size()]));
		} else {
			nodes2Shutdown = new ArrayList<>(nodes2Shutdown);
			closeMaster = nodes2Shutdown.remove(masterNode);
			return pack(new String[nodes2Shutdown.size()], nodes2Shutdown.toArray(new String[nodes2Shutdown.size()]));
		}
	}

	@Override
	public Class<String> getSubResultType() {
		return String.class;
	}

	@Override
	public String procSubTask(String subTask, int timeout) throws InterruptedException {
		log.info("shutdown worker node.");
		worker.shutdownVM(false);
		return "";
	}

	@Override
	public String join(String masterNode, String[] assignedNodeIds, String[] subResults) {
		if (closeMaster) {
			log.info("shutdown master node.");
			master.shutdownVM(false);
		}
		return null;
	}

	@Override
	public String toString() {
		return "MgrShutdownNodes{" +
						"nodes2Shutdown=" + nodes2Shutdown +
						'}';
	}
}
