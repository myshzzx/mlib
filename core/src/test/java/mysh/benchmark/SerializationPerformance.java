package mysh.benchmark;

import com.alibaba.fastjson.JSON;
import mysh.util.Serializer;
import mysh.util.Tick;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.nustaq.serialization.simpleapi.DefaultCoder;

import java.io.IOException;
import java.io.Serializable;
import java.util.Base64;
import java.util.Random;

/**
 * conclusions:
 * 1) json fit for small common POJO and readable result, but need default constructor
 * 2) java serialization fit for large complex object
 * 3) fast-serialization is a good candidate for java-build-in.
 * 4) kryo has NO customized class loader support.
 *
 * @author Mysh
 * @since 2014/11/21 18:55
 */
public class SerializationPerformance {
	private static interface TI {
		void s(Serializable o, int times) throws Exception;

		void ds(Serializable o, int times) throws Exception;
	}

	private static TC t = new TC();
	private static float[][] f = new float[10][100];

	static {
		Random r = new Random();
		for (float[] ff : f) {
			for (int i = 0; i < ff.length; i++) {
				ff[i] = r.nextFloat();
			}
		}
	}

	private static byte[] tb;

	@Test
	@Ignore
	public void benchmarkSuite() throws Exception {
		test(new FST(), t);
//		test(new Java(), t); // 87% slower than fst
//		test(new KRYO(), t);
//		test(new Json(), t); // 42% slower than fst

		test(new FST(), f);
//		test(new Java(), f); // about 40% slower than fst
//		test(new KRYO(), f);
//		test(new Json(), f); // costs too much time, which unacceptable
	}

//	@Test
//	public void kryoTest() throws Exception {
//		KRYO k = new KRYO();
//		k.s(null, 1);
//	}

	@Test
	public void fstTest() throws IOException, ClassNotFoundException {
		TC tt = new TC();
		tt.name = "test";

		// common test
		byte[] bb = Serializer.fst.serialize(tt);
		TC ftt = Serializer.fst.deserialize(bb);
		Assert.assertEquals(tt.name, ftt.name);

		// lambda test
		String msg = "ok";
		Serializable r = (Runnable & Serializable) () -> System.out.println(msg);
		bb = Serializer.fst.serialize(r);
		Runnable ft = Serializer.fst.deserialize(bb);
		ft.run();
		System.out.println(Base64.getEncoder().encodeToString(bb));
	}

	public void test(TI ti, Serializable o) throws Exception {
		int times = 100_000;
		ti.s(o, times);
		ti.ds(o, times);
		System.out.println("byte size = " + tb.length + " : " + ti.getClass().getSimpleName());

		System.gc();
		Thread.sleep(3000);

		times = 1_000_000;
		Tick tick = Tick.tick(ti.getClass().getSimpleName());
		ti.s(o, times);
		System.out.println(tick.nip());

		System.gc();
		Thread.sleep(3000);

		tick.reset();
		ti.ds(o, times);
		System.out.println(tick.nip());

		System.out.println(tick.nipsTotal());
	}

	private static class Java implements TI {

		public void s(Serializable o, int times) throws Exception {
			while (times-- > 0)
				tb = Serializer.buildIn.serialize(o);
		}

		public void ds(Serializable o, int times) throws Exception {
			while (times-- > 0)
				o = Serializer.buildIn.deserialize(tb);
		}

	}

	private static class Json implements TI {

		@Override
		public void s(Serializable o, int times) {
			String jsonStr = "";
			while (times-- > 0) {
				jsonStr = JSON.toJSONString(o);
			}
			tb = jsonStr.getBytes();
		}

		@Override
		public void ds(Serializable o, int times) {
			final Class<? extends Serializable> clazz = o.getClass();
			String jsonStr = new String(tb);
			while (times-- > 0) {
				o = JSON.parseObject(jsonStr, clazz);
			}
		}
	}

	private static class FST implements TI {
		DefaultCoder coder = new DefaultCoder();

		@Override
		public void s(Serializable o, int times) throws Exception {

			while (times-- > 0)
				tb = coder.toByteArray(o);
		}

		@Override
		public void ds(Serializable o, int times) throws Exception {
			while (times-- > 0)
				coder.toObject(tb);
		}
	}

	/*
	private static class KRYO implements TI {
		//		@NotThreadSafe
		Kryo kryo = new Kryo();
		private final byte[] buffer = new byte[100_000];
		//		@NotThreadSafe
		private final Output out = new Output(buffer, -1);

		@Override
		public void s(Serializable o, int times) throws Exception {

			while (times-- > 0) {
				out.setBuffer(buffer, -1);
				kryo.writeClassAndObject(out, o);
				tb = out.toBytes();
			}
		}

		@Override
		public void ds(Serializable o, int times) throws Exception {
			Input in = new Input();
			while (times-- > 0) {
				in.setBuffer(tb, 0, tb.length);
				kryo.readClassAndObject(in);
			}
		}
	}
*/

	private static class TC implements Serializable {
		private static final long serialVersionUID = -3187284791599967033L;
		private String name = "mysh zzx";
		private int age = 26;
		private boolean gender = true;
		private int height = 175;
		private String addr = "";
		private String tel = "189xxxxxxxx";

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public boolean isGender() {
			return gender;
		}

		public void setGender(boolean gender) {
			this.gender = gender;
		}

		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		public String getAddr() {
			return addr;
		}

		public void setAddr(String addr) {
			this.addr = addr;
		}

		public String getTel() {
			return tel;
		}

		public void setTel(String tel) {
			this.tel = tel;
		}
	}
}
