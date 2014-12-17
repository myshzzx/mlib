package mysh.cluster.starter;

import mysh.cluster.ClusterConf;
import mysh.cluster.ClusterNode;

import java.util.concurrent.CountDownLatch;

/**
 * @author Mysh
 * @since 2014/12/12 21:32
 */
public class ClusterStart {
	public static void main(String[] args) throws Throwable {
		new ClusterNode(ClusterConf.readConf());
		new CountDownLatch(1).await();
	}
}
