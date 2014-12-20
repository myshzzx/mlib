package mysh.cluster;


import mysh.annotation.GuardedBy;
import mysh.annotation.Nullable;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Cluster user.<p>
 * WARNING: <br/>
 * 1. the implementation should not contain "heavy state" because
 * it will be serialized several times during cluster calculation.<br/>
 * 2. user created thread should be registered using {@link #regUserThread(Thread)},
 * and react for interruption, or you have to terminate them by yourself.
 *
 * @param <T>  Task Type. should be Serializable.
 * @param <ST> SubTask Type. should be Serializable.
 * @param <SR> SubResult Type. should be Serializable.
 * @param <R>  Result Type. should be Serializable.
 * @author Mysh
 * @since 14-1-28 下午11:23
 */
public abstract class IClusterUser<T, ST, SR, R> implements Serializable {

	private static final long serialVersionUID = -4362703651327770255L;

	/**
	 * subTask encapsulation.
	 */
	public static interface SubTasksPack<ST> extends Serializable {
		public ST[] getSubTasks();

		/**
		 * @return refers subTasks to the specified workerNodes by nodeIds, can be null.<br/>
		 * if the nodeId is null, a proper workerNode will be assigned.
		 * if the workerNode is unavailable, the subTask will be ignored.
		 */
		public String[] getReferredNodeIds();
	}

	/**
	 * generate sub-tasks. can't be NULL.
	 *
	 * @param task        task description.
	 * @param masterNode  master node id.
	 * @param workerNodes available worker nodes (>0).
	 * @return sub-task-descriptions.
	 */
	public abstract SubTasksPack<ST> fork(T task, String masterNode, List<String> workerNodes);

	/**
	 * get SubResult type. will be invoked only once when creating result array.
	 */
	public abstract Class<SR> getSubResultType();

	/**
	 * process sub-task. <br/>
	 * WARNING: should react for thread interruption, so the subTask can be terminated graciously.<br/>
	 * it's recommended to return a non-null object even if nothing to return rather than return null.
	 * for example, a blank string.
	 *
	 * @param subTask sub-task-description.
	 * @param timeout sub-task-execution timeout(milli-second).
	 *                it's a suggestion from the client who submits the task, not compulsory.
	 *                but if the implementation doesn't obey it, the cpu resource may be wasted,
	 *                and the following tasks may be affected.
	 * @return sub-task result.
	 */
	public abstract SR procSubTask(ST subTask, int timeout) throws InterruptedException;

	/**
	 * join sub-tasks results.
	 *
	 * @param masterNode      master node id.
	 * @param assignedNodeIds nodes who are assigned the subTasks, and then submit results.
	 *                        nodeId may be null, which means the subTask is ignored.
	 * @param subResults      sub-tasks results.
	 * @return task result.
	 */
	public abstract R join(String masterNode, String[] assignedNodeIds, SR[] subResults);

	protected SubTasksPack<ST> pack(ST[] subTasks, @Nullable String[] referredNodeIds) {
		return new SubTasksPack<ST>() {
			private static final long serialVersionUID = 5545201296636690353L;

			@Override
			public ST[] getSubTasks() {
				return subTasks;
			}

			@Override
			public String[] getReferredNodeIds() {
				return referredNodeIds;
			}
		};
	}

	/**
	 * split entire array into parts.
	 *
	 * @param entire     array.
	 * @param splitCount parts count.
	 * @return parts array.
	 */
	protected <OT> OT[][] split(OT[] entire, int splitCount) {
		Objects.requireNonNull(entire, "entire obj should not be null");
		if (entire.length < splitCount || entire.length < 1 || splitCount < 1)
			throw new IllegalArgumentException(
							"can't split " + entire.length + "-ele-array into " + splitCount + " parts.");

		@SuppressWarnings("unchecked")
		OT[][] s = (OT[][]) Array.newInstance(entire.getClass(), splitCount);

		int start, end, n = -1,
						step = entire.length % splitCount == 0 ?
										entire.length / splitCount :
										entire.length / splitCount + 1;
		while (++n < splitCount) {
			start = step * n;
			end = start + step > entire.length ? entire.length : start + step;
			OT[] subR = Arrays.copyOfRange(entire, start, end);
			s[n] = subR;
		}

		return s;
	}

	/**
	 * split entire array into parts.
	 *
	 * @param entire     list.
	 * @param splitCount parts count.
	 * @return parts array.
	 */
	protected <OT> List<OT>[] split(List<OT> entire, int splitCount) {
		Objects.requireNonNull(entire, "entire obj should not be null");
		if (entire.size() < splitCount || entire.size() < 1 || splitCount < 1)
			throw new IllegalArgumentException(
							"can't split " + entire.size() + "-ele-array into " + splitCount + " parts.");

		@SuppressWarnings("unchecked")
		List<OT>[] s = (List<OT>[]) Array.newInstance(entire.getClass(), splitCount);

		int start, end, n = -1,
						step = entire.size() % splitCount == 0 ?
										entire.size() / splitCount :
										entire.size() / splitCount + 1;
		while (++n < splitCount) {
			start = step * n;
			end = start + step > entire.size() ? entire.size() : start + step;
			List<OT> subR = new ArrayList<>(end - start);
			for (int i = start; i < end; i++) {
				subR.add(entire.get(i));
			}
			s[n] = subR;
		}

		return s;
	}

	/**
	 * user created threads.
	 */
	@GuardedBy("this")
	transient List<Thread> userThreads;

	/**
	 * register user created threads so that they can be interrupted when task is being canceled.<br/>
	 * WARNING: the thread should react when interrupted so that they can be terminated graciously.
	 */
	protected synchronized void regUserThread(Thread t) {
		if (userThreads == null) userThreads = new ArrayList<>();
		userThreads.add(t);
	}


}
