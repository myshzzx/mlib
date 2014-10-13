package mysh.cluster;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

/**
 * @author Mysh
 * @since 14-1-28 下午6:07
 */
public interface IMaster extends IClusterService {

	String SERVICE_NAME = IMaster.class.getSimpleName();

	static void bind(Registry registry, IMaster master, int port) throws RemoteException, AlreadyBoundException {
		registry.bind(SERVICE_NAME, UnicastRemoteObject.exportObject(master, port));
	}

	static void unbind(Registry registry, IMaster master) throws RemoteException, NotBoundException {
		registry.unbind(SERVICE_NAME);
		UnicastRemoteObject.unexportObject(master, true);
	}

	static IMaster getService(String host, int port) throws Exception {
		return ClusterNode.getRMIService(host, port, SERVICE_NAME);
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
	                     String workerId, WorkerState workerState) throws RemoteException;

	/**
	 * get current workers state.
	 */
	<WS extends WorkerState> Map<String, WS> getWorkerStates() throws RemoteException;

	/**
	 * close master.
	 */
	void closeMaster() throws RemoteException;
}
