package mysh.cluster;

import mysh.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static mysh.cluster.ClusterNode.isNodeUnavailable;

/**
 * Task map-reduce, who
 * connects to each worker, assigns subTasks to workers and summarize results.
 *
 * @author Mysh
 * @since 14-1-22 上午10:17
 */
class Master implements IMaster {
	private static final Logger log = LoggerFactory.getLogger(Master.class);

	private ClusterExp.NotMaster notMasterExp;
	private ClusterExp.NoWorkers noWorkersExp;
	private ClusterExp.TaskTimeout taskTimeoutExp;
	private ClusterExp.TaskCanceled taskCanceledExp;

	private String id;
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
	/**
	 * [nodeId,taskId]
	 */
	private final BlockingQueue<Object[]> cancelTasks = new LinkedBlockingQueue<>();

	private final List<Thread> mThreads = new ArrayList<>();

	Master(String id, Listener listener) {
		Objects.requireNonNull(id, "need master id.");
		Objects.requireNonNull(listener, "need master listener.");

		this.id = id;
		this.listener = listener;

		Thread t;

		t = new WorkersHeartBeat(true, Thread.NORM_PRIORITY);
		t.start();
		mThreads.add(t);

		for (int i = 0; i < 2; i++) {
			t = new SubTaskDispatcher(true, Thread.NORM_PRIORITY + 1);
			t.start();
			mThreads.add(t);
		}

		for (int i = 0; i < 2; i++) {
			t = new SubTaskCanceller(true, Thread.NORM_PRIORITY + 1);
			t.start();
			mThreads.add(t);
		}
	}

	private class WorkersHeartBeat extends Thread {
		public WorkersHeartBeat(boolean isDaemon, int priority) {
			super("cMaster:heart-beat");
			setDaemon(isDaemon);
			setPriority(priority);
		}

		@Override
		public void run() {

			long lastHBTime = 0, tTime;
			// broadcast factor
			int bcFact = 0;
			while (!this.isInterrupted()) {
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

						for (Map.Entry<String, WorkerNode> workerEntry : workersCache.entrySet()) {
							try {
								WorkerState state = workerEntry.getValue().workerService.masterHeartBeat();
								updateWorkerState(workerEntry.getKey(), state);
							} catch (Exception e) {
								if (isNodeUnavailable(e))
									workerUnavailable(workerEntry.getValue(), e);
								else
									log.error("heart beat error, worker id: " + workerEntry.getValue(), e);
							}
						}
					}
				} catch (InterruptedException e) {
					return;
				} catch (Exception e) {
					log.error("master-to-worker-heart-beat error.", e);
				}
			}
		}
	}

	private class SubTaskDispatcher extends Thread {
		public SubTaskDispatcher(boolean isDaemon, int priority) {
			super("cMaster:subTask-dispatcher");
			setDaemon(isDaemon);
			setPriority(priority);
		}

		@Override
		@SuppressWarnings("unchecked")
		public void run() {
			while (!this.isInterrupted()) {
				SubTask subTask = null;
				WorkerNode node = null;

				try {
					subTask = subTasks.take();
					TaskInfo ti = subTask.ti;
					// ignore the subTask if its main-task is removed or timeout
					if (ti.completeLatch.getCount() == 0
									|| (ti.timeout > 0 && System.currentTimeMillis() - ti.startTime > ti.timeout))
						continue;

					String referredNodeId = subTask.getReferredWorkerNode();
					if (referredNodeId != null) {
						node = workersCache.get(referredNodeId);
						if (node != null) {
							workersDispatchQueue.remove(node);
						} else { // ignore subTask because of unavailable node
							ti.subTaskComplete(subTask.subTaskIdx, null, null);
							continue;
						}
					} else {
						node = workersDispatchQueue.take();
					}

					int taskTimeout;
					try {
						taskTimeout = subTask.taskTimeout();
					} catch (ClusterExp.TaskTimeout e) {
						ti.completeLatch.countDown();
						workersDispatchQueue.offer(node);
						continue;
					}

					@SuppressWarnings("unchecked")
					WorkerState state = node.workerService.runSubTask(
									Master.this.id, ti.taskId, subTask.subTaskIdx,
									ti.cUser, subTask.getSubTask(),
									taskTimeout, ti.subTaskTimeout);

					subTask.workerAssigned(node.id);
					updateWorkerState(node.id, state);

				} catch (InterruptedException e) {
					return;
				} catch (Exception e) {
					log.error("subTask dispatch error.", e);

					try {
						if (node != null) {
							if (isNodeUnavailable(e))
								workerUnavailable(node, e);
							else
								updateWorkerState(node.id, node.workerState);
						}
					} catch (Exception ex) {
						log.error("handle sub-task dispatch error failed.", ex);
					}

					if (subTask != null) subTasks.add(subTask);
				}
			}
		}
	}

	private class SubTaskCanceller extends Thread {
		public SubTaskCanceller(boolean isDaemon, int priority) {
			super("cMaster:subTask-canceller");
			setDaemon(isDaemon);
			setPriority(priority);
		}

		@Override
		public void run() {
			while (!this.isInterrupted()) {
				try {
					Object[] ct = cancelTasks.take();
					String nodeId = (String) ct[0];
					int taskId = (int) ct[1];
					WorkerNode node = workersCache.get(nodeId);
					if (node != null) {
						try {
							node.workerService.cancelTask(taskId,-1);
						} catch (Exception e) {
							if (isNodeUnavailable(e))
								workerUnavailable(node, e);
						}
					}
				} catch (InterruptedException e) {
					return;
				} catch (Exception e) {
					log.error("cancel sub task error.", e);
				}
			}
		}
	}

	@Override
	public void closeMaster() {
		log.debug("closing master.");

		for (Thread thread : mThreads)
			try {
				log.debug("closing " + thread.getName());
				thread.interrupt();
				thread.join();
			} catch (Exception e) {
				log.error(thread.getName() + " stop error.", e);
			}

		log.debug("master closed.");
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
				WorkerNode node = new WorkerNode(c.id, listener.getWorkerService(c.ipAddr, c.workerPort));
				this.workersCache.put(node.id, node);
				this.workersDispatchQueue.offer(node);
			}
		} catch (Exception e) {
			log.error("failed to connect to worker. " + c, e);
		}
	}

	private void workerUnavailable(WorkerNode workerNode, Exception e) {
		log.info("worker is unavailable: " + workerNode.id, e);
		removeWorker(workerNode.id);
		if (this.listener != null)
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
	                            String workerNodeId, WorkerState workerState) {
		TaskInfo ti = this.taskInfoTable.get(taskId);
		if (ti != null) {
			ti.subTaskComplete(subTaskId, result, workerNodeId);
			if ((result instanceof Throwable)
							&& !(result instanceof ClusterExp.TaskTimeout) && !(result instanceof InterruptedException))
				subTasks.add(new SubTask(ti, subTaskId));
		}

		this.updateWorkerState(workerNodeId, workerState);
	}

	@Override
	public <T, ST, SR, R> R runTask(IClusterUser<T, ST, SR, R> cUser, T task, int timeout, int subTaskTimeout)
					throws ClusterExp.NotMaster, ClusterExp.NoWorkers, ClusterExp.TaskTimeout, InterruptedException, ClusterExp.TaskCanceled {
		if (!isMaster)
			throw notMasterExp == null ?
							(notMasterExp = new ClusterExp.NotMaster()) : notMasterExp;

		int workerCount = workersCache.size();
		if (workerCount < 1)
			throw noWorkersExp == null ?
							(noWorkersExp = new ClusterExp.NoWorkers()) : noWorkersExp;

		runTaskFlagForBC = true;

		if (cUser instanceof IClusterMgr) {
			((IClusterMgr) cUser).master = this;
		}

		IClusterUser.SubTasksPack<ST> sTasks = cUser.fork(task, workerCount);
		Objects.requireNonNull(sTasks, "IClusterUser.fork return NULL value.");

		Integer taskId = taskIdGen.incrementAndGet();
		if (taskId > Integer.MAX_VALUE / 2) taskIdGen.compareAndSet(taskId, 0);

		TaskInfo<T, ST, SR, R> ti = new TaskInfo<>(taskId, cUser, sTasks.getSubTasks(),
						System.currentTimeMillis(), timeout, subTaskTimeout, sTasks.getReferredNodeIds());
		this.taskInfoTable.put(taskId, ti);

		int subTaskCount = sTasks.getSubTasks().length;
		for (int i = 0; i < subTaskCount; i++)
			this.subTasks.add(new SubTask(ti, i));

		if (timeout <= 0)
			ti.completeLatch.await();
		else if (!ti.completeLatch.await(timeout, TimeUnit.MILLISECONDS)) { // timeout
			cancelTask(ti.taskId);
			throw taskTimeoutExp == null ?
							(taskTimeoutExp = new ClusterExp.TaskTimeout()) : taskTimeoutExp;
		}

		this.taskInfoTable.remove(taskId);

		if (ti.unfinishedTask.get() > 0) // check if task is canceled
			throw taskCanceledExp == null ?
							(taskCanceledExp = new ClusterExp.TaskCanceled()) : taskCanceledExp;

		return cUser.join(ti.results, ti.assignedNodeIds);
	}

	@Override
	public void cancelTask(int taskId) {
		log.info("preparing to cancel task, taskId=" + taskId);
		TaskInfo ti = this.taskInfoTable.remove(taskId);
		if (ti != null) {
			ti.completeLatch.countDown();
			Set<String> assignedNodes = new HashSet<>();
			for (int i = 0; i < ti.assignedNodeIds.length; i++) {
				// the sub-tasks those are assigned but not returned
				if (ti.assignedNodeIds[i] != null &&
								(ti.results[i] == null || ti.results[i] instanceof Exception))
					assignedNodes.add(ti.assignedNodeIds[i]);
			}
			for (String nodeId : assignedNodes) {
				this.cancelTasks.offer(new Object[]{nodeId, taskId});
			}
		}
	}

	public static interface Listener {
		IWorker getWorkerService(String host, int port) throws Exception;

		void broadcastIAmTheMaster();

		void workerUnavailable(String workerId);
	}

	@Override
	@SuppressWarnings("unchecked")
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
		private static ClusterExp.TaskTimeout timeoutExp;

		private final TaskInfo ti;
		private final int subTaskIdx;

		private SubTask(TaskInfo ti, int subTaskIdx) {
			this.ti = ti;
			this.subTaskIdx = subTaskIdx;
			// sub-task executed with exception and not timeout will be reassigned
			this.ti.assignedNodeIds[subTaskIdx] = null;
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
		private int taskTimeout() throws ClusterExp.TaskTimeout {
			int to = ti.timeout == 0 ?
							0 :
							ti.timeout - (int) (System.currentTimeMillis() - ti.startTime);

			if (to < 0)
				throw timeoutExp == null ?
								(timeoutExp = new ClusterExp.TaskTimeout()) :
								timeoutExp;
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

		@Override
		public boolean equals(Object obj) {
			return obj instanceof WorkerNode && this.id.equals(((WorkerNode) obj).id);
		}

		@Override
		public int hashCode() {
			return this.id.hashCode();
		}
	}


}
