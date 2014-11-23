package mysh.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * SubTasks processor, who offers a task-process-service.
 *
 * @author Mysh
 * @since 14-1-22 上午10:17
 */
class Worker implements IWorker {
	private static final Logger log = LoggerFactory.getLogger(Worker.class);
	/**
	 * executors number limit.
	 */
	private static final int ST_EXEC_LIMIT = Runtime.getRuntime().availableProcessors() * 5;

	private volatile String id;
	private volatile Listener listener;

	private final Map<String, IMaster> mastersCache = new ConcurrentHashMap<>();
	private volatile long lastMasterAction = System.currentTimeMillis();

	private final BlockingDeque<SubTask> subTasks = new LinkedBlockingDeque<>();
	private final BlockingQueue<SubTask> completedSubTasks = new LinkedBlockingQueue<>();
	private final WorkerState state;

	Worker(String id, Listener listener, WorkerState initState) {
		Objects.requireNonNull(id, "need master id.");
		Objects.requireNonNull(listener, "need worker listener.");

		this.id = id;
		this.listener = listener;
		this.state = initState == null ? new WorkerState() : initState;
		this.state.update();

		tChkMaster.setDaemon(true);
		tChkMaster.setPriority(Thread.MIN_PRIORITY);
		tChkMaster.start();

		tSTExecScheduler.setDaemon(true);
		tSTExecScheduler.setPriority(Thread.NORM_PRIORITY + 1);
		tSTExecScheduler.start();

		tTaskComplete.setDaemon(true);
		tTaskComplete.setPriority(Thread.NORM_PRIORITY + 1);
		tTaskComplete.start();

		notifySTExecScheduler();
	}

	private Thread tChkMaster = new Thread("cWorker:check-master") {
		@Override
		public void run() {
			int timeout = ClusterNode.NETWORK_TIMEOUT * 2;

			while (!this.isInterrupted()) {
				try {
					Thread.sleep(timeout);
					if (listener != null && lastMasterAction < System.currentTimeMillis() - timeout)
						listener.masterTimeout();
				} catch (InterruptedException e) {
					// end thread if interrupted
					return;
				} catch (Exception e) {
					log.error("worker checker failed.", e);
				}
			}
		}
	};

	private class SubTaskExecutor extends Thread {
		private volatile boolean keepRunning = true;
		private volatile SubTask currSubTask;

		@Override
		public void run() {
			try {
				while (keepRunning && !isInterrupted()) {
					try {
						currSubTask = null;
						currSubTask = subTasks.takeFirst();
						if (!keepRunning) {
							subTasks.offerFirst(currSubTask);
							return;
						}
						currSubTask.execute();
						completedSubTasks.add(currSubTask);
					} catch (InterruptedException e) {
						// end thread if interrupted
						return;
					} catch (Exception e) {
						log.error("run subTask error. " + currSubTask, e);
					}
				}
			} finally {
				stExecutors.remove(this);
				notifySTExecScheduler();
			}
		}
	}

	/**
	 * notify tSTExecScheduler to check executors
	 */
	private void notifySTExecScheduler() {
		synchronized (tSTExecScheduler){
			tSTExecScheduler.notify();
		}
	}

	/**
	 * subTask executors.
	 */
	private final Map<SubTaskExecutor, SubTaskExecutor> stExecutors = new ConcurrentHashMap<>();

	/**
	 * optimize cpu usage by adjusting number of subTask executors.
	 */
	private final Thread tSTExecScheduler = new Thread("cWorker:task-scheduler") {

		@Override
		public void run() {
			Thread currThread = Thread.currentThread();
			while (!currThread.isInterrupted()) {
				try {
					synchronized (this){
						this.wait(ClusterNode.NETWORK_TIMEOUT);
					}
					Worker.this.state.update();

					if (stExecutors.size() == 0) {
						addSubTaskExecutor();
					} else if (state.sysCpu >= 0) {
						if (state.sysCpu < 80 && subTasks.size() > 0 && stExecutors.size() < ST_EXEC_LIMIT)
							addSubTaskExecutor();
						else if (state.sysCpu > 95 && stExecutors.size() > 1) {
							Iterator<SubTaskExecutor> it = stExecutors.keySet().iterator();
							if (it.hasNext())
								stExecutors.remove(it.next()).keepRunning = false;
						}
					} else {
						log.warn("failed to get system cpu usage.");
					}
				} catch (InterruptedException e) {
					// end thread and kill all task-executors if interrupted
					for (Map.Entry<SubTaskExecutor, SubTaskExecutor> steEntry : stExecutors.entrySet()) {
						steEntry.getKey().interrupt();
					}
					stExecutors.clear();
					return;
				} catch (Exception e) {
					log.error("task scheduler error.", e);
				}
			}
		}

		/**
		 * add one subTask executor.
		 */
		private void addSubTaskExecutor() {
			SubTaskExecutor t = new SubTaskExecutor();
			t.setName("cWorker:executor");
			t.setDaemon(true);
			t.setPriority(Thread.NORM_PRIORITY - 1);
			t.start();
			stExecutors.put(t, t);
		}
	};

	private Thread tTaskComplete = new Thread("cWorker:task-complete") {
		@Override
		public void run() {
			SubTask task = new SubTask<>(null, 0, 0, null, null, 0, 0);

			while (!this.isInterrupted()) {
				try {
					task = completedSubTasks.take();
					IMaster masterService = mastersCache.get(task.masterId);
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
		}
	};

	@Override
	public void closeWorker() {
		log.debug("closing worker.");

		try {
			log.debug("closing worker.tChkMaster");
			tChkMaster.interrupt();
			tChkMaster.join();
		} catch (Exception e) {
			log.error("worker.tChkMaster close error.", e);
		}
		try {
			log.debug("closing worker.tSTExecScheduler");
			tSTExecScheduler.interrupt();
			tSTExecScheduler.join();
		} catch (Exception e) {
			log.error("worker.tSTExecScheduler close error.", e);
		}
		try {
			log.debug("closing worker.tTaskComplete");
			tTaskComplete.interrupt();
			tTaskComplete.join();
		} catch (Exception e) {
			log.error("worker.tTaskComplete close error.", e);
		}

		log.debug("worker closed.");
	}

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

	/**
	 * add new master service.
	 */
	void newMaster(Cmd c) {
		try {
			// this pre-check(but not putIfAbsent) can be used in multi-thread app env
			if (!mastersCache.containsKey(c.id))
				mastersCache.put(c.id, listener.getMasterService(c.ipAddr, c.masterPort));
		} catch (Exception e) {
			log.error("failed to connect to master. " + c, e);
		}
	}

	@Override
	public WorkerState masterHeartBeat() {
		this.lastMasterAction = System.currentTimeMillis();
		return this.updateState();
	}

	@Override
	public <T, ST, SR, R> WorkerState runSubTask(
					String masterId, int taskId, int subTaskId, IClusterUser<T, ST, SR, R> cUser, ST subTask,
					int timeout, int subTaskTimeout) {
		this.lastMasterAction = System.currentTimeMillis();
		this.subTasks.offer(new SubTask<>(masterId, taskId, subTaskId, cUser, subTask,
						timeout <= 0 ? Long.MAX_VALUE : this.lastMasterAction + timeout, subTaskTimeout));
		return this.updateState();
	}

	@Override
	public void cancelTask(int taskId) {
		log.debug("cancel task request, taskId=" + taskId);
		this.lastMasterAction = System.currentTimeMillis();
		//cancel sub-tasks in waiting queue
		for (SubTask subTask : subTasks) {
			if (subTask.taskId == taskId) subTask.cancel();
		}
		// cancel sub-tasks in running (by interrupt SubTaskExecutor
		for (Map.Entry<SubTaskExecutor, SubTaskExecutor> steEntry : stExecutors.entrySet()) {
			if (steEntry.getKey().currSubTask != null && steEntry.getKey().currSubTask.taskId == taskId) {
				steEntry.getKey().interrupt();
			}
		}
	}

	private WorkerState updateState() {
		this.state.setTaskQueueSize(this.subTasks.size());
		return this.state;
	}

	public static interface Listener {
		IMaster getMasterService(String host, int port) throws Exception;

		void masterTimeout();

		void masterUnavailable(String masterId);
	}

	private static final ClusterExp.TaskTimeout taskTimeoutExp = new ClusterExp.TaskTimeout();

	private class SubTask<T, ST, SR, R> {

		private final String masterId;
		private final int taskId;
		private final int subTaskId;
		private final IClusterUser<T, ST, SR, R> cUser;
		private final ST subTask;
		private volatile long execBefore;
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
					result = taskTimeoutExp;
					if (this.execBefore == 0)
						log.info("subTask canceled. " + this);
					else
						log.info("subTask exec timeout. " + this);
				} else {
					if (cUser instanceof IClusterMgr) {
						((IClusterMgr) cUser).worker = Worker.this;
					}
					result = cUser.procSubTask(subTask, timeout);
				}
			} catch (Exception e) {
				result = e;
				log.error("task exec error.", e);
			}
		}

		public void cancel() {
			this.execBefore = 0;
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

}
