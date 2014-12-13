package mysh.cluster;

import org.junit.Ignore;

/**
 * @author Mysh
 * @since 14-2-25 下午11:26
 */
@Ignore
public class ClusterTest {

	public static void main(String[] args) throws Exception {
		new ClusterNode(ClusterConf.readConf());
		Thread.sleep(1000000000);
	}

}
