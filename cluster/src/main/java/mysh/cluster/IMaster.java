package mysh.cluster;

import java.util.Map;

/**
 * @author Mysh
 * @since 14-1-28 下午6:07
 */
public interface IMaster extends IClusterService {

	/**
	 * tell the master one sub-task complete, no matter the execution succeed or failed.
	 *
	 * @param result non-Throwable obj represent to successful result, while Throwable means failed.
	 *               And if result is instanceof {@link ClusterExp.TaskTimeout},
	 *               the subTask need not to be re-executed.
	 */
	void subTaskComplete(int taskId, int subTaskId, Object result,
	                     String workerId, WorkerState workerState);

	/**
	 * cancel task by taskId.
	 */
	void cancelTask(int taskId);

	/**
	 * get current workers state.
	 */
	<WS extends WorkerState> Map<String, WS> getWorkerStates();

	/**
	 * close master.
	 */
	void closeMaster();
}
