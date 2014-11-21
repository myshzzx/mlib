package mysh.benchmark;

import com.alibaba.fastjson.JSON;
import mysh.util.SerializeUtil;
import org.junit.Test;

import java.io.Serializable;

/**
 * @author Mysh
 * @since 2014/11/21 18:55
 */
public class SerializationTest {
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

	private static interface TI {
		void s(int times) throws Exception;

		void us(int times) throws Exception;
	}

	private static TC t = new TC();

	private static byte[] tb;
	private static String json;


	@Test
	public void ts() throws Exception {
		test(new Java());
		test(new Json());
	}


	public void test(TI t) throws Exception {
		int times = 100000;
		t.s(times);
		t.us(times);

		Thread.sleep(3000);
		System.out.println("begin");

		times = 100_0000;
		long s = System.nanoTime();
		t.s(times);
		System.out.println((System.nanoTime() - s) / 1000_000);

		s = System.nanoTime();
		t.us(times);
		System.out.println((System.nanoTime() - s) / 1000_000);
	}

	private static class Java implements TI {
		public void s(int times) throws Exception {
			while (times-- > 0)
				tb = SerializeUtil.serialize(t);
		}

		public void us(int times) throws Exception {
			while (times-- > 0)
				t = SerializeUtil.unSerialize(tb);
		}
	}

	private static class Json implements TI {

		@Override
		public void s(int times) {
			while (times-- > 0)
				tb = JSON.toJSONString(t).getBytes();
		}

		@Override
		public void us(int times) {
			while (times-- > 0)
				JSON.parseObject(new String(tb), TC.class);
		}
	}

}
