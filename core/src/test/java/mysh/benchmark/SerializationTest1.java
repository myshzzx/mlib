package mysh.benchmark;

import com.alibaba.fastjson.JSON;
import mysh.util.SerializeUtil;
import org.junit.Assert;
import org.junit.Test;
import org.nustaq.serialization.simpleapi.DefaultCoder;

import java.io.IOException;
import java.io.Serializable;
import java.util.Base64;
import java.util.Random;

/**
 * conclusions:
 * json fit for small common POJO and readable result, but need default constructor
 * java serialization fit for large complex object
 * fast-serialization is a good candidate for java-build-in, if no bugs
 *
 * @author Mysh
 * @since 2014/11/21 18:55
 */
public class SerializationTest1 {
	private static interface TI {
		void s(Serializable o, int times) throws Exception;

		void us(Serializable o, int times) throws Exception;
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
	public void ts() throws Exception {
		test(new Java(), t);
		test(new Json(), t);
		test(new FST(), t);

		test(new Java(), f);
//		test(new Json(), f); // costs too much time, which unacceptable
		test(new FST(), f);
	}

	@Test
	public void fstTest() throws IOException, ClassNotFoundException {
		TC tt = new TC();
		tt.name = "test";

		// common test
		byte[] bb = SerializeUtil.serializeFST(tt);
		TC ftt = SerializeUtil.unSerializeFST(bb);
		Assert.assertEquals(tt.name, ftt.name);

		// lambda test
		String msg = "ok";
		Serializable r = (Runnable & Serializable) () -> System.out.println(msg);
		bb = SerializeUtil.serializeFST(r);
		Runnable ft = SerializeUtil.unSerializeFST(bb);
		ft.run();
	}

	@Test
	public void fstTest2() throws IOException, ClassNotFoundException {
		byte[] decode = Base64.getDecoder().decode("giEBDmludm9rZVN2TWV0aG9kGApydW5TdWJUYXNrGIAB+yYH" +
						"/B9jbl8vMTkyLjE2OC4xMjIuMV8xNDE2NzU5MTI2OTU39wL3AAABK2phdmEubGFuZy5yZWZsZWN0Lkludm9jYXRpb25UYXJnZXRFeGNlcHRpb24AAR5teXNoLmNsdXN0ZXIuQ2x1c3Rlck1nclRlc3QxJDE3/QAA//cA9wAA");
		Serializable s = SerializeUtil.unSerialize(decode);
		System.out.println(s);
		DefaultCoder coder = new DefaultCoder();
		Object o = coder.toObject(decode);
		System.out.println(o);
	}

	@Test
	public void fstTest3(){
		Exception e= new Exception();
		DefaultCoder coder = new DefaultCoder();
		byte[] bytes = coder.toByteArray(e);
		Object o = coder.toObject(bytes);
		System.out.println(o);
	}

	public void test(TI ti, Serializable o) throws Exception {
		int times = 20_000;
		ti.s(o, times);
		ti.us(o, times);

		Thread.sleep(3000);
		System.out.println("byte size = " + tb.length + " : " + ti.getClass().getSimpleName());

		times = 1000_000;
		long s = System.nanoTime();
		ti.s(o, times);
		System.out.println((System.nanoTime() - s) / 1000_000);

		s = System.nanoTime();
		ti.us(o, times);
		System.out.println((System.nanoTime() - s) / 1000_000);
	}

	private static class Java implements TI {

		public void s(Serializable o, int times) throws Exception {
			while (times-- > 0)
				tb = SerializeUtil.serialize(o);
		}

		public void us(Serializable o, int times) throws Exception {
			while (times-- > 0)
				o = SerializeUtil.unSerialize(tb);
		}

	}

	private static class Json implements TI {

		@Override
		public void s(Serializable o, int times) {
			while (times-- > 0)
				tb = JSON.toJSONString(o).getBytes();
		}

		@Override
		public void us(Serializable o, int times) {
			while (times-- > 0)
				o = JSON.parseObject(new String(tb), o.getClass());
		}

	}

	private static class FST implements TI {

		@Override
		public void s(Serializable o, int times) throws Exception {
			DefaultCoder coder = new DefaultCoder();
			while (times-- > 0)
				tb = coder.toByteArray(o);
		}

		@Override
		public void us(Serializable o, int times) throws Exception {
			DefaultCoder coder = new DefaultCoder();
			while (times-- > 0)
				coder.toObject(tb);
		}

	}

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
