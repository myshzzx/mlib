package mysh.gpgpu.info;

import com.aparapi.Kernel;
import com.aparapi.Range;
import com.aparapi.device.Device;
import mysh.gpgpu.AparapiUtil;
import org.junit.Test;

import java.util.Random;

/**
 * @author Mysh
 * @since 2014/9/21 21:32
 */
public class EuclideanDistBenchmark {

	@Test
	public void t1() {
		int len = 1000_000;
		float[] a = new float[len], b = new float[len], c = new float[len];
		Random r = new Random();
		for (int i = 0; i < len; i++) {
			a[i] = r.nextFloat();
			b[i] = r.nextFloat();
		}

		K1 k = new K1(a, b, c);
		Range range = AparapiUtil.getPropCLDevice(null, Device.TYPE.GPU).createRange(len);

		t1t(a, b, c, k, range, len);
		System.out.println("- warm up -");
		t1t(a, b, c, k, range, len);
	}

	public void t1t(float[] a, float[] b, float[] c, K1 k, Range range, int len) {


		long start = System.nanoTime();
		for (int i = 0; i < len; i++) {
			c[i] = a[i] - b[i];
			c[i] *= c[i];
		}
		System.out.println((System.nanoTime() - start) / 1000_000);

		start = System.nanoTime();
		k.execute(range);
		System.out.println((System.nanoTime() - start) / 1000_000);
	}


	private static class K1 extends Kernel {
		float[] a, b, c;

		public K1(float[] a, float[] b, float[] c) {
			this.a = a;
			this.b = b;
			this.c = c;
		}

		@Override
		public void run() {
			int i = getGlobalId();
			c[i] = a[i] - b[i];
			c[i] *= c[i];
		}
	}
}
