package mysh.cluster;

import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * without any master-heart-beat/invoke-call in NETWORK_TIMEOUT*2,
 * the worker node will be considered as out of master-worker network,
 * and should be re-init.
 *
 * @author Mysh
 * @since 14-1-28 下午6:07
 */
interface IWorkerService extends Remote {

	public static interface WorkerState extends Serializable {
		int getTaskQueueSize();
	}

	String SERVICE_NAME = IWorkerService.class.getSimpleName();

	static void bindService(Registry registry, IWorkerService service, int port)
					throws RemoteException, AlreadyBoundException {
		registry.bind(SERVICE_NAME, UnicastRemoteObject.exportObject(service, port));
	}

	static IWorkerService getService(String host, int port) throws Exception {
		Registry registry = LocateRegistry.getRegistry(host, port, ClusterNode.clientSockFact);
		return (IWorkerService) registry.lookup(SERVICE_NAME);
	}

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
}
