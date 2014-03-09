package mysh.cluster;

/**
 * @author Mysh
 * @since 14-2-2 上午10:07
 */
public abstract class ClusterExcp extends Exception {
	private static final long serialVersionUID = -6255937741558729033L;

	public static class NotMaster extends ClusterExcp {
		private static final long serialVersionUID = 5523592130387134313L;
	}

	public static class NoWorkers extends ClusterExcp {
		private static final long serialVersionUID = 4460163700164276223L;
	}

	public static class TaskTimeout extends ClusterExcp {
		private static final long serialVersionUID = 4709028058064796875L;
	}

	public static class SubTaskTimeout extends RuntimeException {
		private static final long serialVersionUID = 259692736881865254L;
	}

	public static class Unready extends ClusterExcp {
		private static final long serialVersionUID = 167985644648520885L;

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
