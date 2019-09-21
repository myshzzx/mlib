package mysh.gpgpu.info;

import com.aparapi.Kernel;
import com.aparapi.Range;
import com.aparapi.device.Device;
import com.aparapi.device.OpenCLDevice;
import mysh.gpgpu.AparapiUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

/**
 * @author Mysh
 * @since 2014/9/1 21:42
 */
@Ignore
public class AparapiTest {


	@Test
	public void disposeOn2DTest() {
		class Kernel1 extends Kernel {
			public int[][] a = new int[10][10];

			@Override
			public void run() {
				int i = getGlobalId();
				a[i][5] = i;
			}
		}

//		System.setProperty("com.amd.aparapi.enableShowGeneratedOpenCL", "true");
		Kernel1 k = new Kernel1();
		k.execute(10);
		System.out.println(Arrays.deepToString(k.a));
		k.dispose();
	}

	private static int K = 128, S = 1000;

	static float[] Kc1a = new float[K * S], Kc1b = new float[K * S], Kc1c = new float[K * S];

	private static class Kc1 extends Kernel {
		float[] a, b, c;

		@Override
		public void run() {
			int i = getGlobalId();
			float t = a[i] - b[i];
			c[i] = t * t;
		}
	}

	static float[][] Kc2a = new float[S][K], Kc2b = new float[S][K], Kc2c = new float[S][K];

	private static class Kc2 extends Kernel {
		float[][] a, b, c;

		@Override
		public void run() {
			int i = getGlobalId(0), j = getGlobalId(1);
			float t = a[i][j] - b[i][j];
			c[i][j] = t * t;
		}
	}

	@Test
	public void perform1D2DTest() {
		System.out.println("warming up");
		compare1DAnd2D(3000);
		System.out.println("real test");
		compare1DAnd2D(10000);
	}

	private void compare1DAnd2D(int n) {

		OpenCLDevice clDev = AparapiUtil.getPropCLDevice(null, Device.TYPE.GPU);
		Range k1r = clDev.createRange(K * S);
		Range k2r = clDev.createRange2D(S, K);

		long k1t = 0, k2t = 0, startTime;
		Random r = new Random();

		Kc1 k1 = new Kc1();
		prepareData();
		while (n-- > 0) {

			startTime = System.nanoTime();
			runKc1(k1, k1r);
			k1t += System.nanoTime() - startTime;

			if (true) continue;
			startTime = System.nanoTime();
			Kc2 k2 = runKc2(k2r);
			k2t += System.nanoTime() - startTime;

			int tt = r.nextInt(K * S);
			if (k1.c[tt] != k2.c[tt / K][tt % K]) throw new RuntimeException();
		}

		System.out.println(k1t);
		System.out.println(k2t);
	}

	private void runKc1(Kc1 k, Range r) {
		copy2Kc1();
		k.a = Kc1a;
		k.b = Kc1b;
		k.c = Kc1c;
		k.execute(r);
	}

	private Kc2 runKc2(Range r) {
		Kc2 k = new Kc2();
		k.a = Kc2a;
		k.b = Kc2b;
		k.c = Kc2c;
		k.execute(r);
		k.dispose();
		return k;
	}

	private void prepareData() {
		Random r = new Random();

		for (int i = 0; i < Kc2a.length; i++) {
			for (int j = 0; j < Kc2a[i].length; j++) {
				Kc2a[i][j] = r.nextFloat();
			}
		}

		for (int i = 0; i < Kc2b.length; i++) {
			for (int j = 0; j < Kc2b[i].length; j++) {
				Kc2b[i][j] = r.nextFloat();
			}
		}
	}

	private void copy2Kc1() {
		for (int i = 0; i < Kc2a.length; i++) {
			System.arraycopy(Kc2a[i], 0, Kc1a, i * K, K);
		}

		for (int i = 0; i < Kc2b.length; i++) {
			System.arraycopy(Kc2b[i], 0, Kc1b, i * K, K);
		}
	}
}
