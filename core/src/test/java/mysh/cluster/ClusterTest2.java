package mysh.cluster;

import org.junit.Test;

import java.net.SocketException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Mysh
 * @since 14-2-27 下午9:43
 */
public class ClusterTest2 {

	private static final int cmdPort = 8030;

	@Test
	public void calcTest() throws SocketException, RemoteException, ClusterExcp.TaskTimeout, InterruptedException, ClusterExcp.Unready {
		ClusterClient c = new ClusterClient(cmdPort);

		Random r = new Random();
		float[][] a = new float[400][128];
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				a[i][j] = r.nextFloat();
			}
		}

		while (true) {
			Thread.sleep(3000);
			long start = System.nanoTime();
			assertEquals(new Integer(a.length), c.runTask(sumUser, a, 0, 0));
			System.out.println("cost time: " + (System.nanoTime() - start) / 1000_000);
		}
	}

	private static IClusterUser<Integer> sumUser = new IClusterUser<Integer>() {
		private static final long serialVersionUID = -6500480014655019875L;

		@Override
		public List<?> fork(Object task, int workerNodeCount) {
			System.out.println("==begin to fork sumUser task.==");
			float[][] a = (float[][]) task;
			int start, end, n = -1, step = a.length / workerNodeCount + 1;

			List r = new ArrayList<>(workerNodeCount);
			while (++n < workerNodeCount) {
				start = step * n;
				end = start + step > a.length ? a.length : start + step;
				float[][] subTask = new float[end - start][];
				System.arraycopy(a, start, subTask, 0, subTask.length);
				r.add(subTask);
			}

			System.out.println("==fork sumUser task end.==");
			return r;
		}

		@Override
		public Object procSubTask(Object subTask, int timeout) {
			System.out.println("--begin to process sumUser subTask.--");
			float[][] a = (float[][]) subTask;
			System.out.println("--process sumUser subTask end.--");
			return a.length;
		}

		@Override
		public Integer join(Object[] subResult) {
			int sum = 0;
			for (Object sr : subResult) sum += (int) sr;
			return sum;
		}
	};
}
