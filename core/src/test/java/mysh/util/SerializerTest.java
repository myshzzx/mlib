package mysh.util;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.nustaq.serialization.simpleapi.DefaultCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Random;

/**
 * @author mysh
 * @since 2015/7/15.
 */
public class SerializerTest {
	private static final Logger log = LoggerFactory.getLogger(SerializerTest.class);

	public static class T implements Serializable {
		int a;

		public T(int a) {
			this.a = a;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			T t = (T) o;
			return a == t.a;
		}
	}

	@Test
	public void buildInObj() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Serializable[] os = new Serializable[]{"mysh zzx", 1, 'c', new T(234)};

		for (Serializable o : os) {
			Serializer.buildIn.serialize(o, out);
		}

		byte[] buf = out.toByteArray();
		InputStream in = new ByteArrayInputStream(buf);

		for (Object o : os) {
			Object obj = Serializer.buildIn.deserialize(in);
			Assert.assertEquals(o, obj);
		}
	}

	@Test
	public void fstObj() throws IOException, ClassNotFoundException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Serializable[] os = new Serializable[]{"mysh zzx", 1, 'c'};

		try (FSTObjectOutput fos = new FSTObjectOutput(out)) {
			for (Serializable o : os) {
				fos.writeObject(o);
			}
		}

		byte[] buf = out.toByteArray();
		InputStream in = new ByteArrayInputStream(buf);

		final DefaultCoder c = new DefaultCoder();
		FSTObjectInput foi = new FSTObjectInput(in);
		for (Object o : os) {
			Object obj = foi.readObject();
			Assert.assertEquals(o, obj);
		}

		foi = new FSTObjectInput(in);
		for (Object o : os) {
			Object obj = foi.readObject();
			Assert.assertEquals(o, obj);
		}
	}

	@Test
	@Ignore
	public void fstMemTest() throws Exception {
		OutputStream out = new ByteArrayOutputStream();

		int[] bigObj = new int[25_000_000];
//		Serializer.fst.serialize(bigObj, out);

		byte[] buf = Serializer.fst.serialize(bigObj);
		Serializer.fst.deserialize(new ByteArrayInputStream(buf));
		Serializable obj = Serializer.fst.deserialize(buf);
//		buf = Serializer.fst.serialize(bigObj);

		buf = null;
		bigObj = null;
		out = null;
		obj = null;

		Thread.sleep(100000000);
	}

	@Test
	@Ignore
	public void testFst() throws Exception {

		Random r = new Random();
		String s1 = String.valueOf(r.nextLong());
		long[] s2 = new long[5_000_000];
		for (int i = 0; i < s2.length; i++) {
			s2[i] = r.nextLong();
		}

		int n = 30;
		Serializer s = Serializer.fst;
		byte[] b; ByteArrayOutputStream buf;
		Serializable ds1; long[] ds2;
		while (n-- > 0) {
			if (r.nextBoolean()) {
				b = s.serialize(s1);
				ds1 = s.deserialize(b);
				Assert.assertEquals(s1, ds1);
			}

			if (r.nextBoolean()) {
				buf = new ByteArrayOutputStream();
				s.serialize(s2, buf);
				ds2 = s.deserialize(new ByteArrayInputStream(buf.toByteArray()));
				Assert.assertArrayEquals(s2, ds2);
			}

			if (r.nextBoolean()) {
				buf = new ByteArrayOutputStream();
				s.serialize(s1, buf);
				ds1 = s.deserialize(new ByteArrayInputStream(buf.toByteArray()));
				Assert.assertEquals(s1, ds1);
			}

			if (r.nextBoolean()) {
				b = s.serialize(s2);
				ds2 = s.deserialize(b);
				Assert.assertArrayEquals(s2, ds2);
			}
		}

		Thread.sleep(10000000);
	}
}
