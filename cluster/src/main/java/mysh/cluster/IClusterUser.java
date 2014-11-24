package mysh.cluster;


import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Cluster user.
 * WARNING: the implementation should not contain "heavy state" because
 * it will be serialized several times during cluster calculation.
 *
 * @param <T>  Task Type. should be Serializable.
 * @param <ST> SubTask Type. should be Serializable.
 * @param <SR> SubResult Type. should be Serializable.
 * @param <R>  Result Type. should be Serializable.
 * @author Mysh
 * @since 14-1-28 下午11:23
 */
public interface IClusterUser<T, ST, SR, R> extends Serializable {

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
	 * generate sub-tasks.
	 *
	 * @param task            task description.
	 * @param workerNodeCount available worker nodes (>0).
	 * @return sub-task-descriptions. can't be NULL.
	 */
	SubTasksPack<ST> fork(T task, int workerNodeCount);

	/**
	 * get SubResult type. will be invoked only once when creating result array.
	 */
	Class<SR> getSubResultType();

	/**
	 * process sub-task.<br/>
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
	SR procSubTask(ST subTask, int timeout) throws InterruptedException;

	/**
	 * join sub-tasks results.
	 *
	 * @param subResults      sub-tasks results.
	 * @param assignedNodeIds nodes who are assigned the subTasks, and then submit results.
	 *                        nodeId may be null, which means the subTask is ignored.
	 * @return task result.
	 */
	R join(SR[] subResults, String[] assignedNodeIds);

	 default SubTasksPack<ST> pack(ST[] subTasks, String[] referredNodeIds) {
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
	default <OT> OT[][] split(OT[] entire, int splitCount) {
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
	default <OT> List<OT>[] split(List<OT> entire, int splitCount) {
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
}
