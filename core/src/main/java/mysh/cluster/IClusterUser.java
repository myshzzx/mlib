package mysh.cluster;


import java.io.Serializable;
import java.util.List;

/**
 * @author Mysh
 * @since 14-1-28 下午11:23
 */
public interface IClusterUser<R> extends Serializable {

	/**
	 * generate sub-tasks.
	 *
	 * @param task            task description.
	 * @param workerNodeCount available worker nodes (>0).
	 * @return sub-task-descriptions.
	 */
	List<?> fork(Object task, int workerNodeCount);

	/**
	 * process sub-task.
	 *
	 * @param subTask sub-task-description.
	 * @param timeout sub-task-execution timeout(milli-second).
	 *                it's a timeout suggestion, not request.
	 *                but if the implementation not obey it, the following tasks may be effected.
	 * @return sub-task result.
	 */
	Object procSubTask(Object subTask, int timeout);

	/**
	 * join sub-tasks results.
	 *
	 * @param subResult sub-tasks results.
	 */
	R join(Object[] subResult);
}
