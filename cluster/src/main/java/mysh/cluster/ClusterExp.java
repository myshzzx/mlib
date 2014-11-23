package mysh.cluster;

/**
 * @author Mysh
 * @since 14-2-2 上午10:07
 */
public abstract class ClusterExp extends Exception {
	private static final long serialVersionUID = -6255937741558729033L;

	public static class NotMaster extends ClusterExp {
		private static final long serialVersionUID = 5349958936229970716L;
	}

	public static class NoWorkers extends ClusterExp {
		private static final long serialVersionUID = 4462301662045096241L;
	}

	public static class TaskTimeout extends ClusterExp {
		private static final long serialVersionUID = -2345536210553087650L;
	}

	public static class TaskCanceled extends ClusterExp {
		private static final long serialVersionUID = -4952342026375713608L;
	}

	/**
	 * IClusterService is not ready until timeout.
	 */
	public static class Unready extends ClusterExp {
		private static final long serialVersionUID = -400165691256459950L;

		public Unready() {
		}

		public Unready(Throwable cause) {
			super(cause);
		}
	}

	public ClusterExp() {
	}

	public ClusterExp(Throwable cause) {
		super(cause);
	}

}
