package mysh.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger log = LoggerFactory.getLogger(WorkerState.class);
	private static final long serialVersionUID = 7594565289428608227L;

	private int taskQueueSize;

	public int getTaskQueueSize() {
		return this.taskQueueSize;
	}

	void setTaskQueueSize(int taskQueueSize) {
		this.taskQueueSize = taskQueueSize;
	}

	/**
	 * heap memory usage (MB).
	 */
	volatile long memHeap;
	/**
	 * non heap memory usage (MB).
	 */
	volatile long memNonHeap;
	volatile long threadCount;
	/**
	 * cpu usage percentage(0 - 100) of current process.
	 * negative value represent for failing to get the number.
	 */
	volatile int procCpu = -1;
	/**
	 * cpu usage percentage(0 - 100) of system.
	 * negative value represent for failing to get the number.
	 */
	volatile int sysCpu = -1;

	private transient MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
	private transient ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	private transient com.sun.management.OperatingSystemMXBean osBean;

	public WorkerState() {
		OperatingSystemMXBean t = ManagementFactory.getOperatingSystemMXBean();
		if (t instanceof com.sun.management.OperatingSystemMXBean) {
			osBean = (com.sun.management.OperatingSystemMXBean) t;
		}
	}

	/**
	 * the update method will be invoked periodically.
	 */
	public void update() {
		memHeap = memoryMXBean.getHeapMemoryUsage().getUsed() / 1000_000;
		memNonHeap = memoryMXBean.getNonHeapMemoryUsage().getUsed() / 1000_000;

		threadCount = threadMXBean.getThreadCount();

		if (osBean != null) {
			procCpu = (int) (osBean.getProcessCpuLoad() * 100);
			sysCpu = (int) (osBean.getSystemCpuLoad() * 100);
		}
	}

	@Override
	public String toString() {
		return "WorkerState{" +
						"taskQueueSize=" + taskQueueSize +
						", memHeap=" + memHeap +
						", memNonHeap=" + memNonHeap +
						", threadCount=" + threadCount +
						", procCpu=" + procCpu +
						", sysCpu=" + sysCpu +
						'}';
	}

}
