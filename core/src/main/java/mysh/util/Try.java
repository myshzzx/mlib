package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Try
 *
 * @since 2018/01/16
 */
public class Try {
    private static final Logger log = LoggerFactory.getLogger(Try.class);
    
    public interface ExpRunnable {
        void run() throws Throwable;
    }

    public interface ExpCallable<T> {
        T call() throws Throwable;
    }

    public interface ExpConsumer<T> {
        void accept(T t) throws Throwable;
    }

    public static <T> Consumer<T> ofIgnoreExp(ExpConsumer<T> c) {
        return t -> {
            try {
                c.accept(t);
            } catch (Throwable tx) {
                log.error("try-run-error", tx);
            }
        };
    }
}
