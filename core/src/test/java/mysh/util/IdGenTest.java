package mysh.util;

import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static mysh.util.IdGen.timeBasedId;

/**
 * IdGenTest
 *
 * @author mysh
 * @since 2016/1/20
 */
public class IdGenTest {

	@Test
	@Ignore
	public void timeBasedIdConflictTest() throws InterruptedException {
		final ConcurrentHashMap<Long, Object> m = new ConcurrentHashMap<>();
		ExecutorService e = Executors.newFixedThreadPool(7);
		int n = 7;

		final AtomicLong total = new AtomicLong();
		final AtomicLong conflict = new AtomicLong();
		while (n-- > 0)
			e.execute(new Runnable() {
				@Override
				public void run() {
					while (true) {
						long id = timeBasedId();
						Object old = m.put(id, "");
						if (old != null) {
							System.out.println("conflict " + id);
							conflict.incrementAndGet();
						}
						total.incrementAndGet();
					}
				}
			});
		Thread.sleep(5000);
		e.shutdownNow();

		Thread.sleep(1000);
		System.out.println(String.format("result: %d/%d, %f", conflict.get(), total.get(),
						conflict.get() * 1.0 / total.get()));
	}
}
