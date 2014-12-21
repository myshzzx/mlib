package mysh.cluster;

import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author Mysh
 * @since 2014/12/21 16:52
 */
public class Cu4Perm extends IClusterUser<String, String, String, String> {

	@Override
	public SubTasksPack<String> fork(String task, String masterNode, List<String> workerNodes) {
		return pack(new String[1], null);
	}

	@Override
	public Class<String> getSubResultType() {
		return String.class;
	}

	@Override
	public String procSubTask(String subTask, int timeout) throws InterruptedException {
		test();
		return null;
	}

	private void test() {
		try (FileOutputStream out = fileOut(fileGet("a.txt"))) {
			out.write("mysh".getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}

		try (FileOutputStream out = fileOut(fileGet("abc/a.txt"))) {
			out.write("mysh".getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}

		try (FileOutputStream out = fileOut(fileGet("../../a.txt"))) {
			out.write("mysh".getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Thread t = new Thread();
			t.start();
			System.out.println("uncontrolled thread started");
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			threadFactory().newThread(() -> {
				System.out.println("user thread.");
				try {
					new CountDownLatch(1).await();
				} catch (InterruptedException e) {
					System.out.println("user thread interrupted");
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String join(String masterNode, String[] assignedNodeIds, String[] subResults) {
		return null;
	}
}
