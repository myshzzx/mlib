package mysh.cluster;

import org.junit.jupiter.api.Disabled;

import java.util.Map;
import java.util.Scanner;

/**
 * @author Mysh
 * @since 14-1-27 下午11:05
 */
@Disabled
public class ControllerTest {

	public static void main(String[] args) throws Throwable {
		ClusterClient c = new ClusterClient(8030);
		Scanner sc = new Scanner(System.in);
		while (true) {
			try {
				final String line = sc.nextLine().trim();
				final String[] s = line.split("\\s");
				switch (s[0]) {
					case "e":
						return;
					case "ws":
						for (Map.Entry<String, WorkerState> e : c.mgrGetWorkerStates().entrySet()) {
							System.out.println(e.getKey() + "-> " + e.getValue());
						}
						break;
					case "c":
						c.mgrCancelTask(Integer.parseInt(s[1]));
						System.out.println("done");
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
