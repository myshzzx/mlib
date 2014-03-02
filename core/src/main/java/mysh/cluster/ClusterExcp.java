package mysh.cluster;

/**
 * @author Mysh
 * @since 14-2-2 上午10:07
 */
public abstract class ClusterExcp extends Exception {


	public static class NotMaster extends ClusterExcp {
	}

	public static class NoWorkers extends ClusterExcp {
	}

	public static class TaskTimeout extends ClusterExcp {
	}

	public static class SubTaskTimeout extends RuntimeException {
	}

	public static class Unready extends ClusterExcp {
		public Unready() {
		}

		public Unready(Exception e) {
			super(e);
		}
	}

	protected ClusterExcp() {
	}

	protected ClusterExcp(Throwable cause) {
		super(cause);
	}
}
