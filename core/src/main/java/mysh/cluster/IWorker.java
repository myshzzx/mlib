package mysh.cluster;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * without any master-heart-beat/invoke-call in NETWORK_TIMEOUT*2,
 * the worker node will be considered as out of master-worker network,
 * and should be re-init.
 *
 * @author Mysh
 * @since 14-1-28 下午6:07
 */
public interface IWorker extends Remote {

	/**
	 * master-heart-beat at a fixed rate of {@link ClusterNode#NETWORK_TIMEOUT}.
	 */
	WorkerState masterHeartBeat() throws RemoteException;

	/**
	 * Invoked by master, subTask execution.
	 */
	<T, ST, SR, R> WorkerState runSubTask(String masterId, int taskId, int subTaskId,
	                                      IClusterUser<T, ST, SR, R> cUser, ST subTask,
	                                      int timeout, int subTaskTimeout)
					throws RemoteException;

	/**
	 * close worker .
	 */
	void closeWorker() throws RemoteException;
}
