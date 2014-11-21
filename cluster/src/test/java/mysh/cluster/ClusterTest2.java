package mysh.cluster;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger log = LoggerFactory.getLogger(ClusterTest2.class);

	private static final int cmdPort = 8030;

	public static void main(String[] args) throws Exception {
		new ClusterNode(cmdPort, null, 0);
		Thread.sleep(100000000);
	}

	@Test
	public void startCluster() throws Exception {
		ClusterNode c = new ClusterNode(cmdPort, null, 0);
//		Thread.sleep(60000);
//		c.shutdownNode();
		Thread.sleep(100000000);
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
			assertEquals(new Integer(a.length), c.runTask(sumUser, a, 30000, 0));
			System.out.println("cost time: " + (System.nanoTime() - start) / 1000_000);
		}
	}

	private static IClusterUser<float[][], float[][], Integer, Integer> sumUser =
					new IClusterUser<float[][], float[][], Integer, Integer>() {
						private static final long serialVersionUID = -6500480014655019875L;

						@Override
						public SubTasksPack<float[][]> fork(float[][] task, int workerNodeCount) {
							log.info("begin to fork sumUser task.==");

							float[][][] r = split(task, workerNodeCount);

							log.info("fork sumUser task end.==");
							return pack(r, null);
						}

						@Override
						public Class<Integer> getSubResultType() {
							return Integer.class;
						}

						@Override
						public Integer procSubTask(float[][] subTask, int timeout) throws InterruptedException {
							log.info("begin to process sumUser subTask.--");
							Thread.sleep(2000);
							log.info("process sumUser subTask end.--");
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
