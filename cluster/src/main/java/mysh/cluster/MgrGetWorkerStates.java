package mysh.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Mysh
 * @since 2014/10/12 14:23
 */
final class MgrGetWorkerStates<WS extends WorkerState>
				extends IClusterMgr<List<String>, Object, WS, Map<String, WS>> {

	private static final long serialVersionUID = -5307305883762804671L;
	private static final Logger log = LoggerFactory.getLogger(MgrGetWorkerStates.class);

	private Map<String, WS> workerStates;
	private Class<WS> wsClass;

	@SuppressWarnings("unchecked")
	public MgrGetWorkerStates(Class<WS> wsClass) {

		this.wsClass = wsClass == null ? (Class<WS>) WorkerState.class : wsClass;
	}

	@Override
	public SubTasksPack<Object> fork(List<String> task, String masterNode, List<String> workerNodes) {

		try {
			workerStates = master.getWorkerStates();
		} catch (Exception e) {
			log.error("master.getWorkerStates error.", e);
		}
		return pack(new Object[0], null);
	}

	@Override
	public Class<WS> getSubResultType() {
		return wsClass;
	}

	@Override
	public WS procSubTask(Object subTask, int timeout) throws InterruptedException {
		return null;
	}

	@Override
	public Map<String, WS> join(String masterNode, String[] assignedNodeIds, WS[] subResults) {
		return workerStates;
	}

	@Override
	public String toString() {
		return "MgrGetWorkerStates{" +
						"wsClass=" + wsClass +
						'}';
	}
}
