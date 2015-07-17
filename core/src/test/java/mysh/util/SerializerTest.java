package mysh.util;

import org.junit.Assert;
import org.junit.Test;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.nustaq.serialization.simpleapi.DefaultCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

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
}
