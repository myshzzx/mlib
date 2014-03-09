package mysh.cluster;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * @author Mysh
 * @since 14-1-28 下午6:07
 */
interface IMasterService extends IClusterService {

	String SERVICE_NAME = IMasterService.class.getSimpleName();

	static void bindService(Registry registry, IMasterService service, int port)
					throws RemoteException, AlreadyBoundException {
		registry.bind(SERVICE_NAME, UnicastRemoteObject.exportObject(service, port));
	}

	static IMasterService getService(String host, int port) throws Exception {
		Registry registry = LocateRegistry.getRegistry(host, port, ClusterNode.clientSockFact);
		return (IMasterService) registry.lookup(SERVICE_NAME);
	}

	/**
	 * tell the master one sub-task complete, no matter the execution succeed or failed.
	 *
	 * @param result non-Throwable obj represent to successful result, while Throwable means failed.
	 *               And if result is instanceof {@link ClusterExcp.TaskTimeout},
	 *               the subTask need not to be re-executed.
	 * @throws java.rmi.RemoteException
	 */
	void subTaskComplete(int taskId, int subTaskId, Object result,
	                     String workerId, IWorkerService.WorkerState workerState) throws RemoteException;

}
