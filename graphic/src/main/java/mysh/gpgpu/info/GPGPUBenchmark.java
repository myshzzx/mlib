package mysh.gpgpu.info;

import com.amd.aparapi.device.Device;
import com.amd.aparapi.Kernel;
import com.amd.aparapi.device.OpenCLDevice;
import com.jogamp.opencl.*;
import mysh.gpgpu.AparapiUtil;
import mysh.gpgpu.JogAmpUtil;

import java.nio.FloatBuffer;

import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;


/**
 * @author Mysh
 * @since 14-1-10 下午11:54
 */
public class GPGPUBenchmark {
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

	public static void main(String[] args) {

		System.out.println("warm up =================");
		// warm up
		initData();
		cpu();
		gpuAparapi();
		gpuJogAmp();
		System.out.println();
		gpuAparapi();
		gpuJogAmp();

		System.out.println("real test =================");
		// real test
		initData();
		gpuAparapi();
		gpuJogAmp();
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

	static void gpuJogAmp() {
		jogAmpTest(JogAmpUtil.getPropCLPlat(null), CLDevice.Type.GPU);
		jogAmpTest(JogAmpUtil.getPropCLPlat("intel"), CLDevice.Type.GPU);
		jogAmpTest(JogAmpUtil.getPropCLPlat("intel"), CLDevice.Type.CPU);
	}

	private static void jogAmpTest(CLPlatform plat, CLDevice.Type type) {
		CLContext context = CLContext.create(plat, type);
		CLDevice device = context.getMaxFlopsDevice();
		CLCommandQueue queue = device.createCommandQueue();
		CLProgram program = context.createProgram(
						"kernel void Calc(global const float* a, global const float* b, global float* c, " +
										"int numElements) {" +
										"" +
										"    int iGID = get_global_id(0);" +
										"" +
										"    if (iGID >= numElements)  {" +
										"        return;" +
										"    }" +
										"" +
										"    c[iGID] = a[iGID] + b[iGID];" +
										"    int n = 100;" +
										"" +
										"    for (int i = 0; i < n; i++) {" +
										"      if ( (c[iGID]>0?c[iGID]:0-c[iGID]) > 1.8446743E19){" +
										"       c[iGID] -= 1.8446743E19;" +
										"       c[iGID]= c[iGID]>0?c[iGID]:0-c[iGID];" +
										"      }" +
										"      else c[iGID] *= a[iGID] - b[iGID];" +
										"    }" +
										"}").build();

		int workSize = device.getMaxWorkGroupSize();  // Local work size dimensions
		int globalWorkSize = size % workSize == 0 ? size : (size / workSize + 1) * workSize;
		CLBuffer<FloatBuffer> clBufferA = context.createFloatBuffer(globalWorkSize, READ_ONLY);
		CLBuffer<FloatBuffer> clBufferB = context.createFloatBuffer(globalWorkSize, READ_ONLY);
		CLBuffer<FloatBuffer> clBufferC = context.createFloatBuffer(globalWorkSize, WRITE_ONLY);
		jogAmpFillBuf(clBufferA.getBuffer(), a);
		jogAmpFillBuf(clBufferB.getBuffer(), b);

		CLKernel kernel = program.createCLKernel("Calc");
		kernel.putArgs(clBufferA, clBufferB, clBufferC).putArg(size);

		long start = System.nanoTime();
		for (int i = 0; i < repeat; i++) {
			queue.putWriteBuffer(clBufferA, false)
							.putWriteBuffer(clBufferB, false)
							.put1DRangeKernel(kernel, 0, globalWorkSize, workSize)
							.putReadBuffer(clBufferC, true);
		}
		System.out.println("JogAmp - " + device.getName() + ": "
						+ (System.nanoTime() - start) / 1000_000
						+ ", " + clBufferC.getBuffer().get());
		context.release();
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
