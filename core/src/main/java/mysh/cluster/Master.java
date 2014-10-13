package mysh.cluster;

import mysh.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.rmi.RemoteException;
import java.util.HashMap;
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
class Master implements IMaster {
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

		rCloseMaster = () -> {
			try {
				log.info("closing master.rWorkersHeartBeat");
				workersHBT.interrupt();
				workersHBT.join();
			} catch (Exception e) {
				log.error("master.rWorkersHeartBeat stop error.", e);
			}

			try {
				log.info("closing master.rSubTaskDispatcher");
				stDistT.interrupt();
				stDistT.join();
			} catch (Exception e) {
				log.error("master.rSubTaskDispatcher stop error.", e);
			}
		};
	}

	private final Runnable rCloseMaster;

	@Override
	public void closeMaster() {
		rCloseMaster.run();
	}

	private final Runnable rSubTaskDispatcher = () -> {

		Thread currentThread = Thread.currentThread();
		while (!currentThread.isInterrupted()) {
			SubTask subTask = null;
			WorkerNode node = null;

			try {
				subTask = subTasks.take();
				// ignore the subTask if its main-task removed.
				if (!taskInfoTable.containsKey(subTask.ti.taskId)) continue;

				String referredNodeId = subTask.getReferredWorkerNode();
				if (referredNodeId != null) {
					node = workersCache.get(referredNodeId);
					if (node != null) {
						workersDispatchQueue.remove(node);
					} else { // ignore subTask because of unavailable node
						subTask.ti.subTaskComplete(subTask.subTaskIdx, null, null);
						continue;
					}
				} else {
					node = workersDispatchQueue.take();
				}

				int taskTimeout = 0;
				try {
					taskTimeout = subTask.taskTimeout();
				} catch (ClusterExcp.TaskTimeout e) {
					subTask.ti.completeLatch.countDown();
					continue;
				}

				@SuppressWarnings("unchecked")
				WorkerState state = node.workerService.runSubTask(
								Master.this.id, subTask.ti.taskId, subTask.subTaskIdx,
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
		Thread currentThread = Thread.currentThread();
		while (!currentThread.isInterrupted()) {
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
							WorkerState state = worker.getValue().workerService.masterHeartBeat();
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
				WorkerNode node = new WorkerNode(c.id, IWorker.getService(c.ipAddr, c.workerPort));
				this.workersCache.put(node.id, node);
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
				for (int i = 0; i < ti.assignedNodeIds.length; i++) {
					if (workerId.equals(ti.assignedNodeIds[i]))
						this.subTasks.add(new SubTask(ti, i));
				}
			}
		}
	}

	/**
	 * maintain {@link #workersDispatchQueue}
	 */
	private void updateWorkerState(String id, WorkerState state) {
		WorkerNode node = this.workersCache.get(id);
		if (node != null) {
			node.workerState = state;
			synchronized (this.workersDispatchQueue) {
				this.workersDispatchQueue.remove(node);
				this.workersDispatchQueue.offer(node);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void subTaskComplete(int taskId, int subTaskId, Object result,
	                            String workerNodeId, WorkerState workerState) throws RemoteException {
		TaskInfo ti = this.taskInfoTable.get(taskId);
		if (ti != null) {
			ti.subTaskComplete(subTaskId, result, workerNodeId);
			if ((result instanceof Throwable) && !(result instanceof ClusterExcp.TaskTimeout))
				subTasks.add(new SubTask(ti, subTaskId));
		}

		this.updateWorkerState(workerNodeId, workerState);
	}

	@Override
	public <T, ST, SR, R> R runTask(IClusterUser<T, ST, SR, R> cUser, T task, int timeout, int subTaskTimeout)
					throws RemoteException,
					ClusterExcp.NotMaster, ClusterExcp.NoWorkers, ClusterExcp.TaskTimeout,
					InterruptedException {
		if (!isMaster)
			throw notMasterExcp == null ?
							(notMasterExcp = new ClusterExcp.NotMaster()) : notMasterExcp;

		int workerCount = workersCache.size();
		if (workerCount < 1)
			throw noWorkersExcp == null ?
							(noWorkersExcp = new ClusterExcp.NoWorkers()) : noWorkersExcp;

		runTaskFlagForBC = true;

		if (cUser instanceof IClusterMgr) {
			((IClusterMgr) cUser).master = this;
		}

		IClusterUser.SubTasksPack<ST> sTasks = cUser.fork(task, workerCount);
		Objects.requireNonNull(sTasks, "IClusterUser.fork return NULL value.");

		Integer taskId = taskIdGen.incrementAndGet();
		TaskInfo<T, ST, SR, R> ti = new TaskInfo<>(taskId, cUser, sTasks.getSubTasks(),
						System.currentTimeMillis(), timeout, subTaskTimeout, sTasks.getReferredNodeIds());
		this.taskInfoTable.put(taskId, ti);

		int subTaskCount = sTasks.getSubTasks().length;
		for (int i = 0; i < subTaskCount; i++)
			this.subTasks.add(new SubTask(ti, i));

		if (timeout <= 0)
			ti.completeLatch.await();
		else if (!ti.completeLatch.await(timeout, TimeUnit.MILLISECONDS) || ti.unfinishedTask.get() > 0)
			throw taskTimeoutExcp == null ?
							(taskTimeoutExcp = new ClusterExcp.TaskTimeout()) : taskTimeoutExcp;

		this.taskInfoTable.remove(taskId);
		return cUser.join(ti.results, ti.assignedNodeIds);
	}

	public static interface Listener {

		void broadcastIAmTheMaster();

		void workerUnavailable(String workerId);
	}

	@Override
	public <WS extends WorkerState> Map<String, WS> getWorkerStates() {
		Map<String, WorkerState> ws = new HashMap<>();
		workersCache.forEach((id, node) -> ws.put(id, node.workerState));
		return (Map<String, WS>) ws;
	}

	private static class TaskInfo<T, ST, SR, R> {
		private final int taskId;
		private final IClusterUser<T, ST, SR, R> cUser;
		private final ST[] subTasks;
		private final long startTime;
		private final int timeout;
		private final int subTaskTimeout;
		private final String[] referredNodeIds;
		private final String[] assignedNodeIds;
		private final SR[] results;

		private final AtomicInteger unfinishedTask;
		private final CountDownLatch completeLatch;

		/**
		 * @param referredNodeIds
		 */
		@SuppressWarnings("unchecked")
		public TaskInfo(int taskId, IClusterUser<T, ST, SR, R> cUser, ST[] subTasks,
		                long startTime, int timeout, int subTaskTimeout,
		                String[] referredNodeIds
		) {
			this.taskId = taskId;
			this.cUser = cUser;
			this.subTasks = subTasks;
			this.startTime = startTime;
			this.timeout = timeout;
			this.subTaskTimeout = subTaskTimeout;

			int taskCount = subTasks.length;
			Asserts.require(referredNodeIds == null || referredNodeIds.length == taskCount,
							"referredNodeIds must have a length of subTasks");
			this.referredNodeIds = referredNodeIds == null ? new String[taskCount] : referredNodeIds;
			assignedNodeIds = new String[taskCount];
			results = (SR[]) Array.newInstance(cUser.getSubResultType(), taskCount);
			unfinishedTask = new AtomicInteger(taskCount);
			completeLatch = new CountDownLatch(taskCount > 0 ? 1 : 0);
		}

		private void subTaskComplete(int index, SR result, String workerNodeId) {
			if (index > -1 && index < results.length) {
				results[index] = result;
				assignedNodeIds[index] = workerNodeId;
				if (!(result instanceof Throwable) && unfinishedTask.decrementAndGet() == 0)
					completeLatch.countDown();
			}
		}
	}

	private static class SubTask {
		private static ClusterExcp.TaskTimeout timeoutExcp;

		private final TaskInfo ti;
		private final int subTaskIdx;

		private SubTask(TaskInfo ti, int subTaskIdx) {
			this.ti = ti;
			this.subTaskIdx = subTaskIdx;
		}

		private Object getSubTask() {
			return ti.subTasks[subTaskIdx];
		}

		private void workerAssigned(String workId) {
			ti.assignedNodeIds[subTaskIdx] = workId;
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

		private String getReferredWorkerNode() {
			return ti.referredNodeIds[subTaskIdx];
		}

	}

	private static class WorkerNode implements Comparable<WorkerNode> {
		private final String id;
		private final IWorker workerService;
		private volatile WorkerState workerState;

		public WorkerNode(String id, IWorker service) {
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
