package com.taobao.rdc.nextone.task.base.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Try
 *
 * @author 凯泓(zhixian.zzx @ alibaba - inc.com)
 * @since 2018/01/16
 */
@Slf4j
public class Try {
    public interface ExpRunnable<E extends Exception> {
        void run() throws E;
    }

    public interface ExpCallable<T> {
        T call() throws Throwable;
    }

    public interface ExpFunction<P, T> {
        T invoke(P p) throws Throwable;
    }

    public interface ExpConsumer<T> {
        void accept(T t) throws Throwable;
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
}
