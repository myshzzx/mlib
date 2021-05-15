package mysh.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Try
 *
 * @since 2018/01/16
 */
public class Try {
	private static final Logger log = LoggerFactory.getLogger(Try.class);
	
	public interface ExpRunnable<E extends Throwable> {
		void run() throws E;
	}
	
	public interface ExpCallable<T, E extends Throwable> {
		T call() throws E;
	}
	
	public interface ExpFunction<P, T, E extends Throwable> {
		T invoke(P p) throws E;
	}
	
	public interface ExpBiFunction<A, B, R, E extends Throwable> {
		R apply(A a, B b) throws E;
	}
	
	public interface ExpConsumer<T, E extends Throwable> {
		void accept(T t) throws E;
	}
	
	public interface ExpBiConsumer<A, B, E extends Throwable> {
		void accept(A a, B b) throws E;
	}
	
	public static <T> Consumer<T> ofIgnoreExpConsumer(ExpConsumer<T, Throwable> c) {
		return t -> {
			try {
				c.accept(t);
			} catch (Throwable tx) {
				log.error("try-run-error", tx);
				throw new RuntimeException(tx.getMessage(), tx);
			}
		};
	}
	
	public static <P, T> Function<P, T> ofIgnoreExpFunc(ExpFunction<P, T, Throwable> c) {
		return t -> {
			try {
				return c.invoke(t);
			} catch (Throwable tx) {
				log.error("try-invoke-error", tx);
				throw new RuntimeException(tx.getMessage(), tx);
			}
		};
	}
	
	public static <T> T runWithCl(ClassLoader cl, Callable<T> c) throws Exception {
		Thread thread = Thread.currentThread();
		ClassLoader currCl = thread.getContextClassLoader();
		try {
			thread.setContextClassLoader(cl);
			return c.call();
		} finally {
			thread.setContextClassLoader(currCl);
		}
	}
	
	public static void runWithCl(ClassLoader cl, Runnable c) throws Exception {
		Thread thread = Thread.currentThread();
		ClassLoader currCl = thread.getContextClassLoader();
		try {
			thread.setContextClassLoader(cl);
			c.run();
		} finally {
			thread.setContextClassLoader(currCl);
		}
	}
}
