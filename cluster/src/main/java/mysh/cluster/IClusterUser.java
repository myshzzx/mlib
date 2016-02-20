package mysh.cluster;

import mysh.util.Exps;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.io.*;
import java.lang.reflect.Array;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cluster user.<p>
 * WARNING: <br/>
 * 1. the implementation should not contain "heavy state" because
 * it will be serialized several times during cluster calculation.<br/>
 * 2. user should use {@link #threadFactory()} to created thread even if not limited.
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

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(0);
	private transient volatile boolean isClosed = false;

	/**
	 * close current cluster user, and release registered resources.
	 * limited resources are not accessible after this invoking.
	 */
	synchronized void closeAndRelease() {
		isClosed = true;
		ns = null;
		filesPerm = null;

		if (userThreads != null) {
			for (Thread t : userThreads) {
				if (t.isAlive()) t.interrupt();
			}
			if (userThreads.size() > 0)
				scheduler.schedule(() -> {
					for (Thread t : userThreads) {
						// force stop
						// noinspection deprecation
						t.stop();
					}
				}, 1, TimeUnit.MINUTES);
		}
	}

	/**
	 * user created threads.
	 */
	@GuardedBy("this")
	private transient Queue<Thread> userThreads;
	@GuardedBy("this")
	private transient AtomicInteger userThreadCount;

	/**
	 * create a thread factory. <br/>
	 * it's recommended to use this factory to create thread or to create thread-pool.<br/>
	 * make sure all created threads are terminated before process finished
	 * (on return of {@link #procSubTask} or {@link mysh.cluster.IMaster#runTask}),
	 * or at least react for interruption so that they can be terminated graciously,
	 * because all threads will be interrupted then, and forced stopped in 1 minute after interruption.
	 */
	protected synchronized ThreadFactory threadFactory() {
		nsCheck();
		if (userThreads == null) userThreads = new ConcurrentLinkedQueue<>();
		if (userThreadCount == null) userThreadCount = new AtomicInteger(1);
		return r -> {
			Thread t = AccessController.doPrivileged(
							(PrivilegedAction<Thread>) () -> {
								Thread tt = new Thread(r, "user-" + ns + "-" + userThreadCount.getAndIncrement());
								tt.setPriority(Thread.NORM_PRIORITY - 2);
								tt.setDaemon(true);
								return tt;
							});
			userThreads.offer(t);
			if (!isClosed) {
				return t;
			} else
				throw new RuntimeException("cluster user has been closed.");
		};
	}

	/**
	 * current clusterUser instance.<br/>
	 * it's used for resource access control.
	 */
	volatile String ns;
	private transient String workDir;
	private transient Permission filesPerm;
	private transient Permission folderPerm;

	private void nsCheck() {
		if (isClosed)
			throw new RuntimeException("cluster user has been closed.");
		if (ns == null)
			throw new AccessControlException("access denied (namespace not given)");
	}

	/**
	 * prepare permission and get current working folder.
	 */
	private String chkAndGetWorkDir() {
		nsCheck();

		if (workDir == null) {
			workDir = FilesMgr.workDir + "/" + ns + "/";

			AccessController.doPrivileged((PrivilegedAction) () -> {
				File dir = new File(workDir);
				if (!dir.exists()) dir.mkdirs();
				return null;
			});
		}

		if (filesPerm == null) {
			filesPerm = new FilePermission(FilesMgr.workDir + "/" + ns + "/-",
							"read,write,delete,readlink");
			folderPerm = new FilePermission(FilesMgr.workDir + "/" + ns, "read,readlink");
		}
		return workDir;
	}

	/**
	 * get file that user can access.
	 *
	 * @param path file path related to work dir.
	 */
	protected final File fileGet(String path) {
		String wd = chkAndGetWorkDir();
		return new File(wd + path);
	}

	/**
	 * get file input stream.
	 */
	protected final FileInputStream fileIn(File f) {
		try {
			return AccessController.doPrivileged(
							(PrivilegedExceptionAction<FileInputStream>) () ->
											new FileInputStream(f), null, filesPerm, folderPerm);
		} catch (PrivilegedActionException e) {
			throw Exps.unchecked(e);
		}
	}

	/**
	 * get file input stream.
	 */
	protected final FileOutputStream fileOut(File f) {
		try {
			return AccessController.doPrivileged(
							(PrivilegedExceptionAction<FileOutputStream>) () -> {
								if (!f.getParentFile().exists())
									f.getParentFile().mkdirs();
								return new FileOutputStream(f);
							}, null, filesPerm, folderPerm);
		} catch (PrivilegedActionException e) {
			throw Exps.unchecked(e);
		}
	}

	/**
	 * delete file or folder.
	 */
	protected final void fileDelete(File f) {
		try {
			AccessController.doPrivileged(
							(PrivilegedExceptionAction<Object>) () -> {
								f.delete();
								return null;
							}, null, filesPerm, folderPerm);
		} catch (PrivilegedActionException e) {
			throw Exps.unchecked(e);
		}
	}

	/**
	 * list child.
	 */
	protected final File[] fileList(File parent, FileFilter filter) {
		try {
			return AccessController.doPrivileged(
							(PrivilegedExceptionAction<File[]>) () -> parent.listFiles(filter),
							null, filesPerm, folderPerm);
		} catch (PrivilegedActionException e) {
			throw Exps.unchecked(e);
		}
	}


}
