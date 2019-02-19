package mysh.benchmark;

import mysh.util.Serializer;
import org.junit.Assert;
import org.junit.Test;
import org.nustaq.serialization.simpleapi.DefaultCoder;

import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * @author Mysh
 * @since 2014/11/24 11:14
 */
public class FstTest {
	private static class T implements Serializable {
		transient int a;
		int b;
	}

	@Test
	public void fstTest1() {
		T t = new T();
		t.a = 10;
		t.b = 20;
		final byte[] buf = Serializer.FST.serialize(t);
		final T tt = Serializer.FST.deserialize(buf, null);
		Assert.assertEquals(0, tt.a);
		Assert.assertEquals(20, tt.b);
	}

	public void fstTest3() throws InterruptedException {
		DefaultCoder c = new DefaultCoder();
		Random r = new Random();
		CountDownLatch l = new CountDownLatch(1);
		for (int i = 0; i < 100; i++) {
			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						l.await();
						long a = r.nextLong();
						byte[] buf = c.toByteArray(a);
						long b = (long) c.toObject(buf);
						Assert.assertEquals(a, b);
					} catch (InterruptedException e) {
					}
				}
			};
			t.start();
		}

		l.countDown();
		Thread.sleep(10000);
	}

	@Test
	public void fstTest4() throws InterruptedException {
		Random r = new Random();
		CountDownLatch l = new CountDownLatch(1);
		for (int i = 0; i < 100; i++) {
			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						l.await();
						long a = r.nextLong();
						byte[] buf = Serializer.FST.serialize(a);
						long b = Serializer.FST.deserialize(buf);
						Assert.assertEquals(a, b);
					} catch (InterruptedException e) {
					}
				}
			};
			t.start();
		}

		l.countDown();
		Thread.sleep(10000);
	}
}
