package mysh.cluster;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.List;

/**
 * @author Mysh
 * @since 2014/11/24 15:21
 */
@ThreadSafe
final class MgrCancelTask extends IClusterMgr<Integer, Object, Object, Object> {
	private static final long serialVersionUID = 4261257711443667489L;

	@Override
	public SubTasksPack<Object> fork(Integer taskId, String masterNode, List<String> workerNodes) {
		master.cancelTask(taskId, null);
		return pack(Collections.emptyList(), null);
	}

	@Override
	public Object procSubTask(Object subTask, int timeout) throws InterruptedException {
		return null;
	}

	@Override
	public Object join(String masterNode, List<String> assignedNodeIds, List<Object> subResults) {
		return null;
	}
}
