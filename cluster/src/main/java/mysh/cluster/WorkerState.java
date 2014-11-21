package mysh.cluster;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

/**
 * current worker state, init and set to ClusterNode when current node start.
 *
 * @author Mysh
 * @since 2014/10/11 20:36
 */
public class WorkerState implements Serializable {
	private static final long serialVersionUID = 7594565289428608227L;

	private int taskQueueSize;

	public int getTaskQueueSize() {
		return this.taskQueueSize;
	}

	void setTaskQueueSize(int taskQueueSize) {
		this.taskQueueSize = taskQueueSize;
	}

	private long memHeap;
	private long memNonHeap;
	private long threadCount;
	private long procCpu = -1;
	private long sysCpu = -1;

	private transient MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
	private transient ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	private transient OperatingSystemMXBean opBean = ManagementFactory.getOperatingSystemMXBean();

	/**
	 * the update method will be invoked periodically.
	 */
	public void update() {
		memHeap = memoryMXBean.getHeapMemoryUsage().getUsed();
		memNonHeap = memoryMXBean.getNonHeapMemoryUsage().getUsed();

		threadCount = threadMXBean.getThreadCount();

		if (opBean instanceof com.sun.management.OperatingSystemMXBean) {
			procCpu = (long) (((com.sun.management.OperatingSystemMXBean) opBean).getProcessCpuLoad() * 10000);
			sysCpu = (long) (((com.sun.management.OperatingSystemMXBean) opBean).getSystemCpuLoad() * 10000);
		}
	}

	@Override
	public String toString() {
		return "WorkerState{" +
						"taskQueueSize=" + taskQueueSize +
						", memHeap=" + memHeap +
						", memNonHeap=" + memNonHeap +
						", threadCount=" + threadCount +
						", procCpu=" + (procCpu / 100.0) +
						", sysCpu=" + (sysCpu / 100.0) +
						'}';
	}
}
