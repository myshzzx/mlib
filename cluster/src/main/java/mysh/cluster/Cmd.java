package mysh.cluster;

import java.io.Serializable;

/**
 * @author Mysh
 * @since 14-2-3 下午2:42
 */
class Cmd implements Serializable {
	private static final long serialVersionUID = 1560510216235141874L;


	static enum Action {
		WHO_IS_THE_MASTER_BY_WORKER, WHO_IS_THE_MASTER_BY_CLIENT,
		I_AM_THE_MASTER, I_AM_A_WORKER,
		CHECK_MASTER, REINIT
	}

	transient long receiveTime;

	final Action action;

	final String id;
	final long startTime;

	final String ipAddr;
	final short ipMask;
	final int masterPort;

	final int workerPort;

	Cmd(Action action, String id, long startTime, String ipAddr, short ipMask, int masterPort, int workerPort) {
		this.action = action;
		this.id = id;
		this.startTime = startTime;
		this.ipAddr = ipAddr;
		this.ipMask = ipMask;
		this.masterPort = masterPort;
		this.workerPort = workerPort;
	}

	@Override
	public String toString() {
		return "Cmd{" +
						"action=" + action +
						", ipAddr='" + ipAddr + '\'' +
						", startTime=" + startTime +
						", receiveTime=" + receiveTime +
						", id='" + id + '\'' +
						", masterPort=" + masterPort +
						", workerPort=" + workerPort +
						'}';
	}
}
