package mysh.gpgpu.info;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.device.Device;
import com.amd.aparapi.device.OpenCLDevice;
import mysh.gpgpu.AparapiUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.FloatBuffer;


/**
 * @author Mysh
 * @since 14-1-10 下午11:54
 */
public class GPGPUBenchmark {
	@BeforeClass
	public static void init() {
		//		System.setProperty("com.amd.aparapi.enableExecutionModeReporting", "true");
//		System.setProperty("com.amd.aparapi.enableShowGeneratedOpenCL", "true");
//		System.setProperty("com.amd.aparapi.enableProfiling", "true");
//		System.setProperty("com.amd.aparapi.enableVerboseJNI", "true");
//		System.setProperty("com.amd.aparapi.logLevel", "INFO");

	}

	static class AparapiKernel extends Kernel {
		float[] a, b, sum;

		public AparapiKernel(float[] a, float[] b, float[] sum) {
			this.a = a;
			this.b = b;
			this.sum = sum;
		}

		@Override
		public void run() {
			int i = getGlobalId();
			sum[i] = calc(a[i], b[i]);
		}
	}

	final static int size = 10_000_000;
	final static int repeat = 1;
	final static float[] a = new float[size];
	final static float[] b = new float[size];
	final static float[] sum = new float[size];

	static void initData() {
		for (int i = 0; i < size; i++) {
			a[i] = (float) (Math.random() * 100);
			b[i] = (float) (Math.random() * 100);
		}
	}

	@Test
	public void t1() {

		System.out.println("warm up =================");
		// warm up
		initData();
//		cpu();
		gpuAparapi();
		System.out.println();
		gpuAparapi();

		System.out.println("real test =================");
		// real test
		initData();
		gpuAparapi();
	}

	private static void cpu() {
		long start = System.nanoTime();
		for (int j = 0; j < repeat; j++) {
			for (int i = 0; i < size; i++)
				sum[i] = calc(a[i], b[i]);
		}
		System.out.println("cpu " + (System.nanoTime() - start) / 1000_000 + ", " + sum[0]);
	}

	private static void gpuAparapi() {
		aparapiTest(AparapiUtil.getPropCLDevice(null, Device.TYPE.GPU));
		aparapiTest(AparapiUtil.getPropCLDevice("intel", Device.TYPE.GPU));
		aparapiTest(AparapiUtil.getPropCLDevice(null, Device.TYPE.CPU));
	}

	private static void aparapiTest(OpenCLDevice dev) {
		Kernel kernel = new AparapiKernel(a, b, sum);
		long start = System.nanoTime();
		for (int i = 0; i < repeat; i++) {
			kernel.execute(dev.createRange(size));
		}
		System.out.println("aparapi - " + dev.getOpenCLPlatform().getName() + ": "
						+ (System.nanoTime() - start) / 1000_000
						+ ", " + sum[0]);
		kernel.dispose();
	}

	private static void jogAmpFillBuf(FloatBuffer buf, float[] f) {
		for (float v : f) {
			buf.put(v);
		}
		buf.rewind();
	}

	static float calc(float a, float b) {
		final float limit = 1.8446743E19f;
		float r = a + b;
		int n = 100;

		for (int i = 0; i < n; i++) {
			if (Math.abs(r) > limit) r = Math.abs(r - limit);
			else r *= a - b;
		}
		return r;
	}
}
