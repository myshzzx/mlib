package mysh.cluster;

import mysh.cluster.FilesMgr.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static mysh.cluster.FilesMgr.FileType.*;

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
	private final ClusterNode clusterNode;
	private final FilesMgr filesMgr;

	private final Map<String, IMaster> mastersCache = new ConcurrentHashMap<>();
	private volatile long lastMasterAction = System.currentTimeMillis();

	private final BlockingDeque<SubTask> subTasks = new LinkedBlockingDeque<>();
	private final BlockingQueue<SubTask> completedSubTasks = new LinkedBlockingQueue<>();
	private final WorkerState state;
	private final int heartBeatTime;

	Worker(String id, ClusterNode clusterNode, WorkerState initState, int heartBeatTime, FilesMgr filesMgr) {
		Objects.requireNonNull(id, "need master id.");
		Objects.requireNonNull(clusterNode, "need cluster node.");

		this.id = id;
		this.clusterNode = clusterNode;
		this.state = initState == null ? new WorkerState() : initState;
		this.state.update();
		this.heartBeatTime = heartBeatTime;
		this.filesMgr = filesMgr;

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
			int timeout = heartBeatTime * 2;

			while (!this.isInterrupted()) {
				try {
					Thread.sleep(timeout);
					if (lastMasterAction + timeout < System.currentTimeMillis())
						clusterNode.masterLost(null);
				} catch (InterruptedException e) {
					// end thread if interrupted
					return;
				} catch (Throwable e) {
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
					} catch (Throwable e) {
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
		synchronized (tSTExecScheduler) {
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
					synchronized (this) {
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
						try {
							steEntry.getKey().join();
						} catch (InterruptedException ex) {
						}
					}
					stExecutors.clear();
					return;
				} catch (Throwable e) {
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
						masterService.subTaskComplete(task.cUser.ns, task.taskId, task.subTaskId, task.result,
										Worker.this.id, Worker.this.updateState());
					} else {
						log.error("submit task result error: master isn't registered: " + task);
					}
				} catch (InterruptedException e) {
					// end thread if interrupted
					return;
				} catch (Throwable e) {
					if (ClusterNode.isNodeUnavailable(e)) {
						if (task.masterId != null) masterServiceUnavailable(task.masterId);
						log.error("submit task result error: master is unavailable: " + task, e);
					} else
						log.error("submit subTask result error. " + task, e);
				}
			}
		}
	};

	void closeWorker() {
		log.debug("closing worker.");

		for (Thread t : new Thread[]{tChkMaster, tSTExecScheduler, tTaskComplete, tFileUpdater}) {
			if (t == null) continue;
			try {
				log.debug("closing " + t.getName());
				t.interrupt();
				t.join();
			} catch (Throwable e) {
				log.error(t.getName() + " close error.", e);
			}
		}

		log.debug("worker closed.");
	}

	private void masterServiceUnavailable(String masterId) {
		lastMasterAction = System.currentTimeMillis();
		removeMasterFromCache(masterId);
		clusterNode.masterLost(masterId);
	}

	/**
	 * remove master from cache table.
	 * provide node-control for {@link ClusterNode}.
	 */
	void removeMasterFromCache(String masterId) {
		mastersCache.remove(masterId);
	}

	/**
	 * add new master service.
	 */
	void newMaster(Cmd c) {
		try {
			// this pre-check(but not putIfAbsent) can be used in multi-thread app env
			if (!mastersCache.containsKey(c.id))
				mastersCache.put(c.id, clusterNode.getMasterService(c.ipAddr, c.masterPort));
		} catch (Throwable e) {
			log.error("failed to connect to master. " + c, e);
		}
	}

	@Override
	public WorkerState masterHeartBeat(String masterId, String masterFilesThumbStamp) {
		this.lastMasterAction = System.currentTimeMillis();
		updateFiles(masterId, masterFilesThumbStamp);
		return this.updateState();
	}

	@Override
	public <T, ST, SR, R> WorkerState runSubTask(
					String ns, String masterId, int taskId, int subTaskId, IClusterUser<T, ST, SR, R> cUser,
					ST subTask, int timeout, int subTaskTimeout) {
		this.lastMasterAction = System.currentTimeMillis();
		this.subTasks.offer(new SubTask<>(masterId, taskId, subTaskId, cUser, subTask,
						timeout <= 0 ? Long.MAX_VALUE : this.lastMasterAction + timeout, subTaskTimeout));
		return this.updateState();
	}

	@Override
	public void cancelTask(int taskId, int subTaskId) {
		log.debug("cancel task request, taskId=" + taskId + ", subTaskId=" + subTaskId);
		this.lastMasterAction = System.currentTimeMillis();
		//cancel sub-tasks in waiting queue
		for (SubTask subTask : subTasks) {
			if (subTask.taskId == taskId
							&& (subTaskId < 0 || subTask.subTaskId == subTaskId))
				subTask.cancel();
		}
		// cancel sub-tasks in running (by interrupt SubTaskExecutor
		for (Map.Entry<SubTaskExecutor, SubTaskExecutor> steEntry : stExecutors.entrySet()) {
			final SubTaskExecutor ste = steEntry.getKey();
			if (ste.currSubTask != null
							&& ste.currSubTask.taskId == taskId
							&& (subTaskId < 0 || ste.currSubTask.subTaskId == subTaskId)) {
				ste.interrupt();
			}
		}
	}

	private WorkerState updateState() {
		this.state.setTaskQueueSize(this.subTasks.size());
		return this.state;
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
			} catch (Throwable e) {
				result = e;
				if (e instanceof InterruptedException)
					log.info("sub-task interrupted: " + this.toString());
				else
					log.error("sub-task exec error:" + this.toString(), e);
			} finally {
				// close cluster user and release resources
				cUser.closeAndRelease();
			}
		}

		public void cancel() {
			log.info("subTask canceled: " + this);
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

	/**
	 * use <b>AtomicBoolean</b> but not <b>volatile boolean</b> here to make sure only one thread started.
	 */
	private final AtomicBoolean updateFilesRunning = new AtomicBoolean(false);

	@Override
	public void updateFiles(String dispatcherId, String thumbStamp) {
		final FilesInfo currFilesInfo = filesMgr.getFilesInfo();
		if (!currFilesInfo.thumbStamp.equals(thumbStamp)) {
			log.info("begin to update files from: " + dispatcherId + ", ts:" + thumbStamp);
			log.info("current filesInfo: " + currFilesInfo);

			if (updateFilesRunning.compareAndSet(false, true)) {
				final IMaster master = mastersCache.get(dispatcherId);
				if (master != null) {
					tFileUpdater = new FileUpdater(master, currFilesInfo);
					tFileUpdater.setDaemon(true);
					tFileUpdater.start();
				}
			}
		}
	}

	private volatile FileUpdater tFileUpdater;

	/**
	 * files check thread.
	 */
	private class FileUpdater extends Thread {

		private IMaster master;
		private FilesInfo currFilesInfo;
		private FilesInfo masterFiles;

		public FileUpdater(IMaster master, FilesInfo currFilesInfo) {
			super("cWorker:update-files-" + System.currentTimeMillis());
			this.master = master;
			this.currFilesInfo = currFilesInfo;
		}

		@Override
		public void run() {
			try {
				// when update core libs, then node needs to restart
				boolean needRestart = false;

				masterFiles = master.getFilesInfo();
				Map<String, String> cFiles = currFilesInfo.filesTsMap;
				Map<String, String> mFiles = masterFiles.filesTsMap;
				for (Map.Entry<String, String> cEntry : cFiles.entrySet()) {
					final String cName = cEntry.getKey();
					String[] path = cName.split("/");
					FileType type = FileType.parse(path[0]);
					String ns = path.length < 3 ? null : path[1];
					String fileName = path.length < 3 ? path[1] : path[2];
					String mts = mFiles.get(cName);
					if (mts == null) {
						filesMgr.removeFile(type, ns, fileName);
						needRestart |= type == CORE;
					} else if (!mts.equals(cEntry.getValue())) {
						filesMgr.putFile(type, ns, fileName, master.getFile(cName));
						needRestart |= type == CORE;
					}
				}
				for (Map.Entry<String, String> mEntry : mFiles.entrySet()) {
					final String mName = mEntry.getKey();
					String[] path = mName.split("/");
					FileType type = FileType.parse(path[0]);
					String ns = path.length < 3 ? null : path[1];
					String fileName = path.length < 3 ? path[1] : path[2];
					if (!cFiles.containsKey(mName)) {
						filesMgr.putFile(type, ns, fileName, master.getFile(mName));
						needRestart |= type == CORE;
					}
				}

				if (needRestart)
					clusterNode.shutdownVM(true);
			} catch (Throwable e) {
				log.error("update files error.", e);
			} finally {
				updateFilesRunning.set(false);
			}
		}
	}

	FilesMgr getFilesMgr() {
		return filesMgr;
	}

	/**
	 * shutdown current node (and restart VM).
	 */
	void shutdownVM(boolean restart) {
		clusterNode.shutdownVM(restart);
	}
}
