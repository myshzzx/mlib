package mysh.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * shutdown master node and restart VM.
 *
 * @author Mysh
 * @since 2014/12/13 17:07
 */
final class MgrRestartMaster extends IClusterMgr<String, String, String, String> {
	private static final Logger log = LoggerFactory.getLogger(MgrRestartMaster.class);
	private static final long serialVersionUID = 6988843023468603974L;

	private static final String[] blankStringArray = new String[0];

	@Override
	public SubTasksPack<String> fork(String task, String masterNode, List<String> workerNodes) {
		log.info("restart master node.");
		master.shutdownVM(true);
		return pack(blankStringArray, blankStringArray);
	}

	@Override
	public Class<String> getSubResultType() {
		return String.class;
	}

	@Override
	public String procSubTask(String subTask, int timeout) throws InterruptedException {
		return "";
	}

	@Override
	public String join(String masterNode, String[] assignedNodeIds, String[] subResults) {
		return null;
	}

}
