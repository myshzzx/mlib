package mysh.util;

import java.util.Arrays;
import java.util.stream.Stream;

public class Threads {
	/**
	 * get all threads running in current JVM.
	 */
	public static Thread[] allThreads() {
		ThreadGroup g = Thread.currentThread().getThreadGroup();
		while (g.getParent() != null) g = g.getParent();
		Thread[] threads = new Thread[g.activeCount()];
		g.enumerate(threads, true);
		return threads;
	}

	/**
	 * get all threads running in current JVM.
	 */
	public static Stream<Thread> allThreadStream() {
		return Arrays.stream(allThreads());
	}
}
