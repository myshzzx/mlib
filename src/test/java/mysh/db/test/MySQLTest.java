package mysh.db.test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MySQLTest {
	static {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			System.exit(-1);
		}
	}

	static public Connection getConn() throws SQLException {
		return DriverManager
				.getConnection("jdbc:mysql://localhost/test?user=root&password=myshzzx");
	}

	static class TestTask implements Callable<String> {
		private final double task;
		private final CyclicBarrier barrier;

		public TestTask(double task, CyclicBarrier barrier) {
			this.task = task;
			this.barrier = barrier;
		}

		@Override
		public String call() throws Exception {
			long start ;
			try (Connection connection = getConn()) {
				Statement stmt = connection.createStatement();

				this.barrier.await();
				start = System.nanoTime();
				stmt.executeQuery("select count(*) from test.test1 where value>"
								+ this.task + ";");
			}
			return start / 1000000 + ": " + (System.nanoTime() - start) / 1000000;

		}

	}

	public static void main(String[] args) throws Exception {
		// prepareData();
		int n = 0;
		while (++n < 25) {
			if (24 % n == 0)
				test(n);
		}
	}

	private static void test(int concurrentNum) throws InterruptedException, ExecutionException {
		CyclicBarrier barrier = new CyclicBarrier(concurrentNum);
		ExecutorService exe = Executors.newFixedThreadPool(concurrentNum);
		CompletionService<String> exec = new ExecutorCompletionService<>(exe);

		exec.submit(new TestTask(0.2, barrier));
		exec.submit(new TestTask(0.4, barrier));
		exec.submit(new TestTask(0.6, barrier));
		exec.submit(new TestTask(0.8, barrier));
		exec.submit(new TestTask(0.1, barrier));
		exec.submit(new TestTask(0.3, barrier));
		exec.submit(new TestTask(0.5, barrier));
		exec.submit(new TestTask(0.7, barrier));
		exec.submit(new TestTask(0.23, barrier));
		exec.submit(new TestTask(0.42, barrier));
		exec.submit(new TestTask(0.654, barrier));
		exec.submit(new TestTask(0.864, barrier));
		exec.submit(new TestTask(0.13, barrier));
		exec.submit(new TestTask(0.32, barrier));
		exec.submit(new TestTask(0.55, barrier));
		exec.submit(new TestTask(0.72, barrier));
		exec.submit(new TestTask(0.2312, barrier));
		exec.submit(new TestTask(0.42312, barrier));
		exec.submit(new TestTask(0.65442, barrier));
		exec.submit(new TestTask(0.86442, barrier));
		exec.submit(new TestTask(0.1311, barrier));
		exec.submit(new TestTask(0.3242, barrier));
		exec.submit(new TestTask(0.5511, barrier));
		exec.submit(new TestTask(0.7232, barrier));
		exe.shutdown();

		long start = System.nanoTime();

		System.out.println("concurrent : " + concurrentNum);
		for (int i = 0; i < 24; i++)
			System.out.println(exec.take().get());
		System.out.println("total : " + (System.nanoTime() - start) / 1000000);
		System.out.println();
	}

	public static void prepareData() throws Exception {
		Connection connection = getConn();
		try {
			Statement stmt = connection.createStatement();

			Random rand = new Random(1893264893472098734L);
			int i = 0;
			while (++i < 600) {
				int n = 10000;
				while (n-- > 0) {
					stmt.executeUpdate("insert into test.test1(value, text) value("
							+ rand.nextDouble() + ", '" + rand.nextInt()
							+ "');");
				}
				System.out.println(i);
			}
		} finally {
			connection.close();
		}
	}
}

/*
concurrent : 1
16659307: 2362
16661686: 2305
16664009: 2279
16666310: 2252
16668577: 2343
16670937: 2315
16673267: 2292
16675575: 2256
16677847: 2334
16680196: 2304
16682513: 2274
16684803: 2215
16687032: 2332
16689378: 2320
16691713: 2284
16694015: 2244
16696272: 2329
16698614: 2308
16700935: 2268
16703216: 2216
16705444: 2330
16707786: 2310
16710108: 2281
16712410: 2255
total : 55703

concurrent : 2
16714688: 2712
16714688: 2712
16717413: 2682
16717413: 2682
16720112: 2579
16720112: 2579
16722704: 2663
16722704: 2714
16725428: 3143
16725428: 3146
16728586: 2575
16728586: 2608
16731204: 2628
16731204: 2628
16733848: 2550
16733848: 2550
16736418: 2741
16736418: 2741
16739177: 2679
16739178: 2682
16741869: 2683
16741869: 2683
16744566: 2607
16744566: 2607
total : 32506

concurrent : 3
16747254: 3282
16747254: 3415
16747254: 3461
16750722: 3133
16750722: 3415
16750722: 3506
16754244: 3347
16754244: 3368
16754257: 3413
16757677: 3068
16757677: 3312
16757737: 3416
16761160: 3346
16761160: 3351
16761160: 3466
16764633: 3274
16764633: 3361
16764633: 3426
16768097: 3240
16768066: 3330
16768066: 3389
16771463: 3263
16771464: 3311
16771464: 3480
total : 27770

concurrent : 4
16774958: 4286
16774969: 4314
16774958: 4398
16774958: 4438
16779402: 4324
16779402: 4386
16779402: 4396
16779403: 4399
16783808: 4232
16783808: 4373
16783808: 4376
16783808: 4403
16788218: 4306
16788218: 4402
16788218: 4415
16788218: 4418
16792642: 4221
16792642: 4369
16792642: 4373
16792642: 4421
16797070: 4339
16797070: 4345
16797070: 4444
16797070: 4464
total : 26589

concurrent : 6
16801582: 5431
16801553: 5862
16801553: 5943
16801553: 6586
16801553: 6678
16801598: 6634
16808239: 4937
16808239: 6168
16808239: 6450
16808239: 6562
16808239: 6580
16808239: 6638
16814895: 5379
16814884: 5831
16814933: 6030
16814884: 6527
16814895: 6654
16814895: 6701
16821617: 5864
16821702: 6007
16821671: 6375
16821717: 6361
16821617: 6477
16821765: 6601
total : 26828

concurrent : 8
16828399: 5825
16828398: 6946
16828399: 7307
16828399: 7484
16828473: 7740
16828398: 8853
16828398: 8854
16828398: 8984
16837390: 5160
16837480: 7889
16837418: 8130
16837436: 8265
16837390: 8630
16837494: 8740
16837494: 8747
16837494: 8773
16846273: 5045
16846467: 6948
16846273: 7478
16846374: 7859
16846467: 7975
16846273: 8804
16846483: 8658
16846483: 8752
total : 26868

concurrent : 12
16855641: 6959
16855272: 8956
16855275: 9310
16855345: 12156
16855275: 12227
16855272: 12772
16855458: 12853
16855275: 13050
16855431: 12910
16855431: 13007
16855336: 13110
16855467: 13259
16868733: 7150
16868733: 8201
16868762: 9033
16868824: 10620
16869061: 10861
16868733: 11738
16868892: 11626
16869062: 11708
16868824: 12670
16869159: 12708
16868733: 13326
16869152: 12951
total : 26866

concurrent : 24
16882826: 19099
16882653: 19548
16882789: 19530
16882377: 20027
16882790: 19686
16882526: 20145
16882208: 20663
16882527: 20696
16882526: 20819
16882209: 23626
16882481: 24567
16882208: 25113
16882528: 25499
16882591: 25535
16882209: 25988
16882492: 25707
16882481: 25811
16882263: 26097
16882826: 25698
16882731: 25849
16882377: 26300
16882359: 26342
16882263: 26488
16882826: 26035
total : 26755

 */
