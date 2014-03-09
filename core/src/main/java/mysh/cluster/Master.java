package mysh.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Task map-reduce, who
 * connects to each worker, assigns subTasks to workers and summarize results.
 *
 * @author Mysh
 * @since 14-1-22 上午10:17
 */
class Master implements IMasterService {
	private static final Logger log = LoggerFactory.getLogger(Master.class);

	private ClusterExcp.NotMaster notMasterExcp;
	private ClusterExcp.NoWorkers noWorkersExcp;
	private ClusterExcp.TaskTimeout taskTimeoutExcp;

	private String id;
	private final int port;
	private volatile Listener listener;
	private volatile boolean isMaster = false;

	/**
	 * run task flag used by heart-beat broadcast.
	 */
	private volatile boolean runTaskFlagForBC = false;
	private final Map<String, WorkerNode> workersCache = new ConcurrentHashMap<>(ClusterNode.NODES_SCALE);
	private final PriorityBlockingQueue<WorkerNode> workersDispatchQueue = new PriorityBlockingQueue<>();

	private final AtomicInteger taskIdGen = new AtomicInteger(0);
	private final Map<Integer, TaskInfo> taskInfoTable = new ConcurrentHashMap<>();
	private final BlockingQueue<SubTask> subTasks = new LinkedBlockingQueue<>();

	Master(String id, int port, Listener listener) {
		Objects.requireNonNull(id, "need master id.");
		Objects.requireNonNull(listener, "need master listener.");

		this.id = id;
		this.port = port;
		this.listener = listener;

		Thread workersHBT = new Thread(rWorkersHeartBeat, "clusterMaster:workers-heart-beat");
		workersHBT.setDaemon(true);
		workersHBT.setPriority(Thread.NORM_PRIORITY);
		workersHBT.start();

		Thread stDistT = new Thread(rSubTaskDispatcher, "clusterMaster:subTask-dispatcher");
		stDistT.setDaemon(true);
		stDistT.setPriority(Thread.NORM_PRIORITY + 1);
		stDistT.start();
	}

	private final Runnable rSubTaskDispatcher = () -> {

		while (true) {
			SubTask subTask = null;
			WorkerNode node = null;

			try {
				subTask = subTasks.take();
				// ignore the subTask if its main-task removed.
				if (!taskInfoTable.containsKey(subTask.ti.taskId)) continue;

				node = workersDispatchQueue.take();

				int taskTimeout = 0;
				try {
					taskTimeout = subTask.taskTimeout();
				} catch (ClusterExcp.TaskTimeout e) {
					subTask.ti.completeLatch.countDown();
					continue;
				}

				@SuppressWarnings("unchecked")
				IWorkerService.WorkerState state = node.workerService.runSubTask(
								Master.this.id, subTask.ti.taskId, subTask.subTaskId,
								subTask.ti.cUser, subTask.getSubTask(),
								taskTimeout, subTask.ti.subTaskTimeout);

				subTask.workerAssigned(node.id);
				updateWorkerState(node.id, state);

			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
				log.error("subTask dispatch error.", e);

				if (subTask != null) subTasks.add(subTask);

				if (node != null) {
					updateWorkerState(node.id, node.workerState);

					if (ClusterNode.isNodeUnavailable(e)) {
						workerUnavailable(node);
						log.error("worker is unavailable: " + node.id);
					}
				}
			}
		}
	};

	private final Runnable rWorkersHeartBeat = () -> {
		long lastHBTime = 0, tTime;
		// broadcast factor
		int bcFact = 0;
		while (true) {
			try {
				Thread.sleep(ClusterNode.NETWORK_TIMEOUT / 2);

				tTime = System.currentTimeMillis();
				if (isMaster && tTime - lastHBTime > ClusterNode.NETWORK_TIMEOUT) {
					lastHBTime = tTime;

					// broadcast I_AM_THE_MASTER
					if (listener != null && (bcFact = (bcFact + 1) % 10) == 0 && runTaskFlagForBC) {
						runTaskFlagForBC = false;
						listener.broadcastIAmTheMaster();
					}

					for (Map.Entry<String, WorkerNode> worker : workersCache.entrySet()) {
						try {
							IWorkerService.WorkerState state = worker.getValue().workerService.masterHeartBeat();
							updateWorkerState(worker.getKey(), state);
						} catch (Exception e) {
							if (ClusterNode.isNodeUnavailable(e)) {
								workerUnavailable(worker.getValue());
								log.error("worker is unavailable: " + worker.getValue().id, e);
							} else
								log.error("heart beat error, worker id: " + worker.getValue(), e);
						}
					}
				}
			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
				log.error("master-to-worker-heart-beat error.", e);
			}
		}
	};


	public int getServicePort() {
		return port;
	}

	public void setMaster(boolean isMaster) {
		this.isMaster = isMaster;
		log.info("set to be master: " + isMaster);
	}

	/**
	 * add new worker service.
	 */
	void newNode(Cmd c) {
		try {
			if (!workersCache.containsKey(c.id)) {
				WorkerNode node = new WorkerNode(c.id, IWorkerService.getService(c.ipAddr, c.workerPort));
				this.workersCache.put(c.id, node);
				this.workersDispatchQueue.offer(node);
			}
		} catch (Exception e) {
			log.error("failed to connect to worker. " + c, e);
		}
	}

	private void workerUnavailable(WorkerNode workerNode) {
		removeWorker(workerNode.id);
		this.listener.workerUnavailable(workerNode.id);
	}

	/**
	 * provide node-control for {@link ClusterNode}.
	 */
	void removeWorker(String workerId) {
		WorkerNode node = this.workersCache.remove(workerId);
		if (node != null) {
			this.workersDispatchQueue.remove(node);

			// reschedule unfinished subTask
			for (Map.Entry<Integer, TaskInfo> taskE : taskInfoTable.entrySet()) {
				TaskInfo ti = taskE.getValue();
				for (int i = 0; i < ti.workersId.length; i++) {
					if (workerId.equals(ti.workersId[i]))
						this.subTasks.add(new SubTask(ti, i));
				}
			}
		}
	}

	/**
	 * maintain {@link #workersDispatchQueue}
	 */
	private void updateWorkerState(String id, IWorkerService.WorkerState state) {
		WorkerNode node = this.workersCache.get(id);
		if (node != null) {
			node.workerState = state;
			synchronized (this) {
				this.workersDispatchQueue.remove(node);
				this.workersDispatchQueue.offer(node);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void subTaskComplete(int taskId, int subTaskId, Object result,
	                            String workerId, IWorkerService.WorkerState workerState) throws RemoteException {
		TaskInfo ti = this.taskInfoTable.get(taskId);
		if (ti != null) {
			ti.subTaskComplete(subTaskId, result);
			if ((result instanceof Throwable) && !(result instanceof ClusterExcp.TaskTimeout))
				subTasks.add(new SubTask(ti, subTaskId));
		}

		this.updateWorkerState(workerId, workerState);
	}

	@Override
	public <T, ST, SR, R> R runTask(IClusterUser<T, ST, SR, R> cUser, T task, int timeout, int subTaskTimeout)
					throws RemoteException,
					ClusterExcp.NotMaster, ClusterExcp.NoWorkers, ClusterExcp.TaskTimeout,
					InterruptedException {
//todo 这方法要改: 客户端等待 任务执行时, 任务执行时间超过设置的 RMI 等待超时, RMI会认为服务无响应而抛异常
		if (!isMaster)
			throw notMasterExcp == null ?
							(notMasterExcp = new ClusterExcp.NotMaster()) : notMasterExcp;

		int workerCount = workersCache.size();
		if (workerCount < 1)
			throw noWorkersExcp == null ?
							(noWorkersExcp = new ClusterExcp.NoWorkers()) : noWorkersExcp;

		runTaskFlagForBC = true;
		ST[] sTasks = cUser.fork(task, workerCount);
		Objects.requireNonNull(sTasks, "IClusterUser.fork return NULL value.");

		Integer taskId = taskIdGen.incrementAndGet();
		TaskInfo<T, ST, SR, R> ti = new TaskInfo<>(taskId, cUser, sTasks,
						System.currentTimeMillis(), timeout, subTaskTimeout);
		this.taskInfoTable.put(taskId, ti);

		int subTaskCount = sTasks.length;
		for (int i = 0; i < subTaskCount; i++)
			this.subTasks.add(new SubTask(ti, i));

		if (timeout <= 0)
			ti.completeLatch.await();
		else if (!ti.completeLatch.await(timeout, TimeUnit.MILLISECONDS) || ti.unfinishedTask.get() > 0)
			throw taskTimeoutExcp == null ?
							(taskTimeoutExcp = new ClusterExcp.TaskTimeout()) : taskTimeoutExcp;

		this.taskInfoTable.remove(taskId);
		return cUser.join(ti.results);
	}

	public static interface Listener {

		void broadcastIAmTheMaster();

		void workerUnavailable(String workerId);
	}

	private static class TaskInfo<T, ST, SR, R> {
		private final int taskId;
		private final IClusterUser<T, ST, SR, R> cUser;
		private final ST[] subTasks;
		private final long startTime;
		private final int timeout;
		private final int subTaskTimeout;
		private final String[] workersId;
		private final SR[] results;

		private final AtomicInteger unfinishedTask;
		private final CountDownLatch completeLatch = new CountDownLatch(1);

		@SuppressWarnings("unchecked")
		public TaskInfo(int taskId, IClusterUser<T, ST, SR, R> cUser, ST[] subTasks,
		                long startTime, int timeout, int subTaskTimeout) {
			this.taskId = taskId;
			this.cUser = cUser;
			this.subTasks = subTasks;
			this.startTime = startTime;
			this.timeout = timeout;
			this.subTaskTimeout = subTaskTimeout;

			int taskCount = subTasks.length;
			workersId = new String[taskCount];
			results = (SR[]) Array.newInstance(cUser.getSubResultType(), taskCount);
			unfinishedTask = new AtomicInteger(taskCount);
		}

		private void subTaskComplete(int index, SR result) {
			if (index > -1 && index < results.length) {
				results[index] = result;
				if (!(result instanceof Throwable) && unfinishedTask.decrementAndGet() == 0)
					completeLatch.countDown();
			}
		}
	}

	private static class SubTask {
		private static ClusterExcp.TaskTimeout timeoutExcp;

		private final TaskInfo ti;
		private final int subTaskId;

		private SubTask(TaskInfo ti, int subTaskId) {
			this.ti = ti;
			this.subTaskId = subTaskId;
		}

		private Object getSubTask() {
			return ti.subTasks[subTaskId];
		}

		private void workerAssigned(String workId) {
			ti.workersId[subTaskId] = workId;
		}

		/**
		 * calculate left time of the task from now on.
		 */
		private int taskTimeout() throws ClusterExcp.TaskTimeout {
			int to = ti.timeout == 0 ?
							0 :
							ti.timeout - (int) (System.currentTimeMillis() - ti.startTime);

			if (to < 0)
				throw timeoutExcp == null ?
								(timeoutExcp = new ClusterExcp.TaskTimeout()) :
								timeoutExcp;
			return to;
		}

	}

	private static class WorkerNode implements Comparable<WorkerNode> {
		private final String id;
		private final IWorkerService workerService;
		private volatile IWorkerService.WorkerState workerState;

		public WorkerNode(String id, IWorkerService service) {
			this.id = id;
			this.workerService = service;
		}

		@Override
		public int compareTo(@SuppressWarnings("NullableProblems") WorkerNode o) {
			if (workerState == null && o.workerState == null) return 0;
			else if (workerState == null) return -1;
			else if (o.workerState == null) return 1;
			else
				return workerState.getTaskQueueSize() - o.workerState.getTaskQueueSize();
		}

		@Override
		public String toString() {
			return id;
		}
	}


}
