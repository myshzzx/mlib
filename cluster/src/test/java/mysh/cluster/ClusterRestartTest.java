package mysh.cluster;

import mysh.util.OSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * @author Mysh
 * @since 2014/10/12 21:32
 */
public class ClusterRestartTest {
	private static final Logger log = LoggerFactory.getLogger(ClusterRestartTest.class);

	public static void main(String[] args) {
		log.info("start.");
		try {
			ClusterNode node = new ClusterNode(8030, null,0);
			Thread.sleep(10000);
			node.shutdownNode();
			OSUtil.restart();
		} catch (Throwable e) {
			JOptionPane.showMessageDialog(null, e.toString());
		}
	}
}
