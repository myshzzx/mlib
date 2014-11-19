package mysh.cluster;

import org.junit.Ignore;
import org.junit.Test;

import java.net.SocketException;
import java.rmi.RemoteException;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Mysh
 * @since 14-2-27 下午9:43
 */
@Ignore
public class ClusterTest2 {

	private static final int cmdPort = 8030;

	@Test
	public void singleTest() throws Exception {
		new ClusterNode(cmdPort, null);
		this.calcTest();
	}

	@Test
	public void calcTest() throws SocketException, RemoteException, ClusterExp.TaskTimeout, InterruptedException, ClusterExp.Unready {
		ClusterClient c = new ClusterClient(cmdPort);

		Random r = new Random();
		float[][] a = new float[r.nextInt(400) + 128][128];
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				a[i][j] = r.nextFloat();
			}
		}

		while (true) {
			Thread.sleep(3000);
			long start = System.nanoTime();
			assertEquals(new Integer(a.length), c.runTask(sumUser, a, 10000, 0));
			System.out.println("cost time: " + (System.nanoTime() - start) / 1000_000);
		}
	}

	private static IClusterUser<float[][], float[][], Integer, Integer> sumUser =
					new IClusterUser<float[][], float[][], Integer, Integer>() {
						private static final long serialVersionUID = -6500480014655019875L;

						@Override
						public SubTasksPack<float[][]> fork(float[][] task, int workerNodeCount) {
							System.out.println("==begin to fork sumUser task.==");

							float[][][] r = split(task, workerNodeCount);

							System.out.println("==fork sumUser task end.==");
							return pack(r, null);
						}

						@Override
						public Class<Integer> getSubResultType() {
							return Integer.class;
						}

						@Override
						public Integer procSubTask(float[][] subTask, int timeout) throws InterruptedException {
							System.out.println("--begin to process sumUser subTask.--");
							Thread.sleep(20000);
							System.out.println("--process sumUser subTask end.--");
							return subTask.length;
						}

						@Override
						public Integer join(Integer[] subResult, String[] nodes) {
							int sum = 0;
							for (int sr : subResult) sum += sr;
							return sum;
						}
					};
}
