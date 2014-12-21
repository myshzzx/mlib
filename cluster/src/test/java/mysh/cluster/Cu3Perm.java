package mysh.cluster;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Mysh
 * @since 2014/12/18 11:12
 */
public class Cu3Perm extends IClusterUser<String, String, String, String> {
	@Override
	public SubTasksPack<String> fork(String task, String masterNode, List<String> workerNodes) {
		return pack(new String[1], null);
	}

	private void test() {
		try {
			Files.readAllBytes(Paths.get("l:", "a.txt"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Files.write(Paths.get("main", "user", "a.txt"), "user".getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Files.readAllBytes(Paths.get("main", "user", "a.txt"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Files.readAllBytes(Paths.get("main", "user", "a.jar"));
			System.out.println("read a.jar successfully");
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			CountDownLatch l = new CountDownLatch(1);
			final Thread t = new Thread() {
				@Override
				public void run() {
					System.out.println("thread:" + this.getName());
					System.out.println("thread group:" + this.getThreadGroup());
					try {
						l.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
			t.start();
			t.setPriority(1);
			l.countDown();
			t.join();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			ForkJoinPool fjp = ForkJoinPool.commonPool();
			fjp.submit(() -> System.out.println("ForkJoinPool common success"));
			fjp.submit(() -> System.out.println("ForkJoinPool common success"));
			fjp.awaitQuiescence(1, TimeUnit.MINUTES);

			fjp = new ForkJoinPool(3);
			fjp.submit(() -> System.out.println("ForkJoinPool custom success"));
			fjp.awaitQuiescence(1, TimeUnit.MINUTES);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			ExecutorService exec = Executors.newFixedThreadPool(2);
			exec.submit(() -> System.out.println("thread pool success"));
			exec.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			final Process p = Runtime.getRuntime().exec("cmd");
			p.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}

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

	@Override
	public String join(String masterNode, String[] assignedNodeIds, String[] subResults) {
		return null;
	}


}
