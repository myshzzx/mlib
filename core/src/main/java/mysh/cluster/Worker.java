package mysh.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * SubTasks processor, who offers a task-process-service.
 *
 * @author Mysh
 * @since 14-1-22 上午10:17
 */
class Worker implements IWorkerService {
	private static final Logger log = LoggerFactory.getLogger(Worker.class);

	private volatile String id;
	private final int port;
	private volatile Listener listener;

	private final Map<String, IMasterService> mastersCache = new ConcurrentHashMap<>();
	private volatile long lastMasterAction = System.currentTimeMillis();

	private final BlockingQueue<SubTask> subTasks = new LinkedBlockingDeque<>();
	private final BlockingQueue<SubTask> completedSubTasks = new LinkedBlockingQueue<>();
	private final WorkerStateImpl state = new WorkerStateImpl();

	Worker(String id, int port, Listener listener) {
		Objects.requireNonNull(id, "need master id.");
		Objects.requireNonNull(listener, "need worker listener.");

		this.id = id;
		this.port = port;
		this.listener = listener;

		Thread chkMasterT = new Thread(rChkMaster, "clusterWorker:check-master");
		chkMasterT.setDaemon(true);
		chkMasterT.setPriority(Thread.MIN_PRIORITY);
		chkMasterT.start();

		Thread taskExecT = new Thread(rTaskExec, "clusterWorker:task-exec");
		taskExecT.setDaemon(true);
		taskExecT.setPriority(Thread.NORM_PRIORITY);
		taskExecT.start();

		Thread taskCompleteT = new Thread(rTaskComplete, "clusterWorker:task-complete");
		taskCompleteT.setDaemon(true);
		taskCompleteT.setPriority(Thread.NORM_PRIORITY + 1);
		taskCompleteT.start();
	}

	private final Runnable rChkMaster = () -> {
		int timeout = ClusterNode.NETWORK_TIMEOUT * 2;
		while (true) {
			try {
				Thread.sleep(timeout);
				if (listener != null && lastMasterAction < System.currentTimeMillis() - timeout)
					listener.masterTimeout();
			} catch (InterruptedException e) {
				// end thread if interrupted
				return;
			} catch (Exception e) {
				log.error("notify listener failed.", e);
			}
		}
	};

	private final Runnable rTaskExec = () -> {
		SubTask task = null;
		while (true) {
			try {
				task = subTasks.take();
				task.execute();
				completedSubTasks.add(task);
			} catch (InterruptedException e) {
				// end thread if interrupted
				return;
			} catch (Exception e) {
				log.error("run subTask error. " + task, e);
			}
		}
	};

	private final Runnable rTaskComplete = () -> {
		SubTask task = new SubTask<>(null, 0, 0, null, null, 0, 0);

		while (true) {
			try {
				task = completedSubTasks.take();
				IMasterService masterService = mastersCache.get(task.masterId);
				if (masterService != null) {
					masterService.subTaskComplete(task.taskId, task.subTaskId, task.result,
									Worker.this.id, Worker.this.updateState());
				} else {
					log.error("submit task result error: master isn't registered: " + task);
				}
			} catch (InterruptedException e) {
				// end thread if interrupted
				return;
			} catch (Exception e) {
				if (ClusterNode.isNodeUnavailable(e)) {
					if (task.masterId != null) masterUnavailable(task.masterId);
					log.error("submit task result error: master is unavailable: " + task, e);
				} else
					log.error("submit subTask result error. " + task, e);
			}
		}
	};

	private void masterUnavailable(String masterId) {
		removeMaster(masterId);
		this.listener.masterUnavailable(masterId);
	}

	/**
	 * provide node-control for {@link ClusterNode}.
	 */
	void removeMaster(String masterId) {
		mastersCache.remove(masterId);
	}

	public int getServicePort() {
		return port;
	}

	/**
	 * add new master service.
	 */
	void newMaster(Cmd c) {
		try {
			// this pre-check(but not putIfAbsent) can be used in multi-thread app env
			if (!mastersCache.containsKey(c.id))
				mastersCache.put(c.id, IMasterService.getService(c.ipAddr, c.masterPort));
		} catch (Exception e) {
			log.error("failed to connect to master. " + c, e);
		}
	}

	@Override
	public WorkerState masterHeartBeat() throws RemoteException {
		this.lastMasterAction = System.currentTimeMillis();
		return this.updateState();
	}

	@Override
	public <T, ST, SR, R> WorkerState runSubTask(String masterId, int taskId, int subTaskId,
	                                             IClusterUser<T, ST, SR, R> cUser, ST subTask,
	                                             int timeout, int subTaskTimeout)
					throws RemoteException {
		this.lastMasterAction = System.currentTimeMillis();
		this.subTasks.add(new SubTask<>(masterId, taskId, subTaskId, cUser, subTask,
						timeout <= 0 ? Long.MAX_VALUE : this.lastMasterAction + timeout, subTaskTimeout));
		return this.updateState();
	}

	private WorkerState updateState() {
		this.state.taskQueueSize = this.subTasks.size();
		return this.state;
	}

	public static interface Listener {

		void masterTimeout();

		void masterUnavailable(String masterId);
	}

	private static class SubTask<T, ST, SR, R> {

		private static final ClusterExcp.TaskTimeout taskTimeoutExcp = new ClusterExcp.TaskTimeout();

		private final String masterId;
		private final int taskId;
		private final int subTaskId;
		private final IClusterUser<T, ST, SR, R> cUser;
		private final ST subTask;
		private final long execBefore;
		private final int timeout;

		private Object result;

		public SubTask(String masterId, int taskId, int subTaskId,
		               IClusterUser<T, ST, SR, R> cUser, ST subTask, long execBefore, int timeout) {
			this.masterId = masterId;
			this.taskId = taskId;
			this.subTaskId = subTaskId;
			this.cUser = cUser;
			this.subTask = subTask;
			this.execBefore = execBefore;
			this.timeout = timeout;
		}

		public void execute() {
			try {
				if (System.currentTimeMillis() > this.execBefore) {
					result = taskTimeoutExcp;
					log.error("task exec timeout. " + this);
				} else
					result = cUser.procSubTask(subTask, timeout);
			} catch (Exception e) {
				result = e;
				log.error("task exec error.", e);
			}
		}

		@Override
		public String toString() {
			return "SubTask{" +
							"masterId='" + masterId + '\'' +
							", taskId=" + taskId +
							", subTaskId=" + subTaskId +
							", execBefore=" + execBefore +
							'}';
		}
	}

	private static class WorkerStateImpl implements WorkerState {
		private static final long serialVersionUID = -8275435482240858040L;
		private int taskQueueSize;

		@Override
		public int getTaskQueueSize() {
			return taskQueueSize;
		}
	}

}