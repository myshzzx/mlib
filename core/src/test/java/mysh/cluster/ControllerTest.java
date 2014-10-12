package mysh.cluster;

import org.junit.Ignore;

import java.util.Scanner;

/**
 * @author Mysh
 * @since 14-1-27 下午11:05
 */
@Ignore
public class ControllerTest {

	public static void main(String[] args) throws Exception {
		new ClusterNode(8030, null);
		Scanner s = new Scanner(System.in);
		s.next();
		System.exit(0);
	}

}
