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
	
	public interface ExpRunnable {
		void run() throws Exception;
	}
	
	public interface ExpCallable<T> {
		T call() throws Exception;
	}
	
	public interface ExpFunction<P, T> {
		T invoke(P p) throws Exception;
	}
	
	public interface ExpBiFunction<A, B, R> {
		R apply(A a, B b) throws Exception;
	}
	
	public interface ExpConsumer<T> {
		void accept(T t) throws Exception;
	}
	
	public interface ExpBiConsumer<A, B> {
		void accept(A a, B b) throws Exception;
	}
	
	public static <T> Consumer<T> ofIgnoreExpConsumer(ExpConsumer<T> c) {
		return t -> {
			try {
				c.accept(t);
			} catch (Throwable tx) {
				log.error("try-run-error", tx);
				throw new RuntimeException(tx.getMessage(), tx);
			}
		};
	}
	
	public static <P, T> Function<P, T> ofIgnoreExpFunc(ExpFunction<P, T> c) {
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
