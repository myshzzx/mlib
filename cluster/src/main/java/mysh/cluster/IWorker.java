package mysh.cluster;

import mysh.cluster.update.IUpdateListener;

/**
 * without any master-heart-beat/invoke-call in NETWORK_TIMEOUT*2,
 * the worker node will be considered as out of master-worker network,
 * and should be re-init.
 *
 * @author Mysh
 * @since 14-1-28 下午6:07
 */
public interface IWorker extends IUpdateListener {

	/**
	 * master-heart-beat at a fixed rate of {@link ClusterNode#NETWORK_TIMEOUT}.
	 *
	 * @param masterId              master node id
	 * @param masterFilesThumbStamp master files thumbStamp
	 */
	WorkerState masterHeartBeat(String masterId, String masterFilesThumbStamp);

	/**
	 * Invoked by master, subTask execution.
	 */
	<T, ST, SR, R> WorkerState runSubTask(String masterId, int taskId, int subTaskId,
	                                      IClusterUser<T, ST, SR, R> cUser, ST subTask,
	                                      int timeout, int subTaskTimeout);

	/**
	 * Invoked by master, cancel task/subTask by id.
	 *
	 * @param subTaskId ignored if negative.
	 */
	void cancelTask(int taskId, int subTaskId);

}
