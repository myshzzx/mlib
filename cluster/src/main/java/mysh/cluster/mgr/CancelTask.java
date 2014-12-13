package mysh.cluster.mgr;

import mysh.annotation.ThreadSafe;
import mysh.cluster.IClusterMgr;

import java.util.Set;

/**
 * @author Mysh
 * @since 2014/11/24 15:21
 */
@ThreadSafe
public final class CancelTask extends IClusterMgr<Integer, Object, Object, Object> {
	private static final long serialVersionUID = 4261257711443667489L;

	@Override
	public SubTasksPack<Object> fork(Integer taskId, Set<String> workerNodes) {
		master.cancelTask(taskId, null);
		return pack(new Object[0], null);
	}

	@Override
	public Class<Object> getSubResultType() {
		return Object.class;
	}

	@Override
	public Object procSubTask(Object subTask, int timeout) throws InterruptedException {
		return null;
	}

	@Override
	public Object join(Object[] subResults, String[] assignedNodeIds) {
		return null;
	}
}
