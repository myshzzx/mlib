package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Function;

/**
 * 关于异常的工具类.
 */
public abstract class Exps {
	private static final Logger log = LoggerFactory.getLogger(Exps.class);
	
	/**
	 * 将CheckedException转换为UncheckedException.
	 */
	public static RuntimeException unchecked(Throwable e) {
		if (e instanceof RuntimeException) {
			return (RuntimeException) e;
		} else {
			return new RuntimeException(e);
		}
	}
	
	/**
	 * 将ErrorStack转化为String.
	 */
	public static String getStackTraceAsString(Exception e) {
		StringWriter stringWriter = new StringWriter();
		e.printStackTrace(new PrintWriter(stringWriter));
		return stringWriter.toString();
	}
	
	/**
	 * 判断异常是否由某些底层的异常(含本层)引起. 是则返回异常, 否返回 null.
	 */
	@SafeVarargs
	public static Throwable isCausedBy(Throwable ex, Class<? extends Throwable>... causeExceptionClasses) {
		Throwable cause = ex;
		while (cause != null) {
			for (Class<? extends Throwable> causeClass : causeExceptionClasses) {
				if (causeClass.isInstance(cause)) {
					return cause;
				}
			}
			cause = cause.getCause();
		}
		return null;
	}
	
	/**
	 * 执行若有异常自动重试.
	 *
	 * @param comment    任务备注
	 * @param retryTimes 重试次数
	 * @param needRetry  为null则无条件重试
	 * @param r          任务
	 * @return 执行抛出的异常
	 */
	public static <T> T retryOnExp(
			String comment, int retryTimes, Function<Exception, Boolean> needRetry, Try.ExpCallable<T, Exception> r) throws Exception {
		int times = 0;
		while (true) {
			try {
				return r.call();
			} catch (Exception e) {
				if (times < Math.max(1, retryTimes)
						&& (needRetry == null || needRetry.apply(e))) {
					times++;
					log.info("retry-on-exp {}/{}, {}, exp={}", times, retryTimes, comment, e.toString());
				} else {
					log.error("stop-retry {}/{}, {}, exp={}", times, retryTimes, comment, e.toString());
					throw e;
				}
			}
		}
	}
	
	/**
	 * @see #retryOnExp(String, int, Function, Try.ExpCallable)
	 */
	public static void retryOnExp(
			String comment, int retryTimes, Function<Exception, Boolean> needRetry, Try.ExpRunnable<Exception> r) throws Exception {
		retryOnExp(comment, retryTimes, needRetry, () -> {
			r.run();
			return null;
		});
	}
}
