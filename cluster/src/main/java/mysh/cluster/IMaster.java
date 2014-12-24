package mysh.cluster;

/**
 * @author Mysh
 * @since 14-1-28 下午6:07
 */
public interface IMaster extends IClusterService, IUpdateDispatcher {

	/**
	 * tell the master one sub-task complete, no matter the execution succeed or failed.<br/>
	 * WARNING: check {@link mysh.cluster.rpc.thrift.RpcUtil#wrapSyncClient} when update signature.
	 *
	 * @param result non-Throwable obj represent to successful result, while Throwable means failed.
	 *               And if result is instanceof {@link ClusterExp.TaskTimeout},
	 *               the subTask need not to be re-executed.
	 */
	void subTaskComplete(String ns, int taskId, int subTaskId, Object result,
	                     String workerId, WorkerState workerState);

}
