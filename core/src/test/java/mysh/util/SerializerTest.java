package mysh.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.nustaq.serialization.simpleapi.DefaultCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author mysh
 * @since 2015/7/15.
 */
@Disabled
public class SerializerTest extends Assertions {
	private static final Logger log = LoggerFactory.getLogger(SerializerTest.class);

	public static class T implements Serializable {
		int a;
		Map m;
		Set s;
		List l;

		public T(int a) {
			this.a = a;
			m = ImmutableMap.of(2, new File("f"));
			s = ImmutableSet.of("a", 239, 293874);
			l = ImmutableList.of(ThreadLocalRandom.current().nextLong());
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			T t = (T) o;

			if (a != t.a) return false;
			if (m != null ? !m.equals(t.m) : t.m != null) return false;
			if (s != null ? !s.equals(t.s) : t.s != null) return false;
			return l != null ? l.equals(t.l) : t.l == null;
		}

		@Override
		public int hashCode() {
			int result = a;
			result = 31 * result + (m != null ? m.hashCode() : 0);
			result = 31 * result + (s != null ? s.hashCode() : 0);
			result = 31 * result + (l != null ? l.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "T{" +
							"a=" + a +
							", m=" + m +
							", s=" + s +
							", l=" + l +
							'}';
		}
	}

	@Test
	public void buildInObj() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Serializable[] os = new Serializable[]{"mysh zzx", 1, 'c', new T(234)};

		for (Serializable o : os) {
			Serializer.BUILD_IN.serialize(o, out);
		}

		byte[] buf = out.toByteArray();
		InputStream in = new ByteArrayInputStream(buf);

		for (Object o : os) {
			Object obj = Serializer.BUILD_IN.deserialize(in);
			assertEquals(o, obj);
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
			assertEquals(o, obj);
		}

		foi = new FSTObjectInput(in);
		for (Object o : os) {
			Object obj = foi.readObject();
			assertEquals(o, obj);
		}
	}

	@Test
	@Disabled
	public void fstMemTest() throws Exception {
		OutputStream out = new ByteArrayOutputStream();

		int[] bigObj = new int[25_000_000];
//		Serializer.FST.serialize(bigObj, out);

		byte[] buf = Serializer.FST.serialize(bigObj);
		Serializer.FST.deserialize(new ByteArrayInputStream(buf));
		Serializable obj = Serializer.FST.deserialize(buf);
//		buf = Serializer.FST.serialize(bigObj);

		buf = null;
		bigObj = null;
		out = null;
		obj = null;

		Thread.sleep(100000000);
	}

	@Test
	@Disabled
	public void testFst() throws Exception {

		Random r = new Random();
		String s1 = String.valueOf(r.nextLong());
		long[] s2 = new long[5_000_000];
		for (int i = 0; i < s2.length; i++) {
			s2[i] = r.nextLong();
		}

		int n = 30;
		Serializer s = Serializer.FST;
		byte[] b; ByteArrayOutputStream buf;
		Serializable ds1; long[] ds2;
		while (n-- > 0) {
			if (r.nextBoolean()) {
				b = s.serialize(s1);
				ds1 = s.deserialize(b);
				assertEquals(s1, ds1);
			}

			if (r.nextBoolean()) {
				buf = new ByteArrayOutputStream();
				s.serialize(s2, buf);
				ds2 = s.deserialize(new ByteArrayInputStream(buf.toByteArray()));
				assertArrayEquals(s2, ds2);
			}

			if (r.nextBoolean()) {
				buf = new ByteArrayOutputStream();
				s.serialize(s1, buf);
				ds1 = s.deserialize(new ByteArrayInputStream(buf.toByteArray()));
				assertEquals(s1, ds1);
			}

			if (r.nextBoolean()) {
				b = s.serialize(s2);
				ds2 = s.deserialize(b);
				assertArrayEquals(s2, ds2);
			}
		}

		Thread.sleep(10000000);
	}

}
