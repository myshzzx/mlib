package mysh.cluster;

import org.junit.Ignore;

/**
 * @author Mysh
 * @since 14-2-25 下午11:26
 */
@Ignore
public class ClusterTest {
	private static final int cmdPort = 8030;

	public static void main(String[] args) throws Exception {
		new ClusterNode(cmdPort, null);
	}

}
