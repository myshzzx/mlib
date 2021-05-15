package mysh.util;

import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 任务组, 用于批量发起一组任务, 发起后等待全部任务完成. 任一任务抛异常会使任务组被关闭, 所有任务取消. 开始取结果时任务组会关闭. 关闭的任务组提交新任务时不会执行. WARNING: 任务不可返回 Throwable 实例
 *
 * @author <pre>凯泓(zhixian.zzx@alibaba-inc.com)</pre>
 * @apiNote ThreadSafe
 * @since 2018/05/03
 */
public class TaskGroup<T> {
	
	private ExecutorService exec;
	private volatile boolean stop = false;
	private boolean allowPartialResult;
	private Queue<Future<Object>> tasks = new LinkedBlockingQueue<>();
	
	private TaskGroup() {
	}
	
	public static <T> TaskGroup<T> of(ExecutorService exec) {
		TaskGroup<T> taskGroup = new TaskGroup<>();
		taskGroup.exec = exec;
		return taskGroup;
	}
	
	public TaskGroup<T> allowPartialResult(boolean allowPartialResult) {
		this.allowPartialResult = allowPartialResult;
		return this;
	}
	
	public void execute(Try.ExpRunnable c) {
		execute(() -> {
			c.run();
			return null;
		});
	}
	
	public void execute(Try.ExpCallable<T, Throwable> c) {
		if (stop || c == null) {
			return;
		}
		
		Future<Object> task = exec.submit(() -> {
			try {
				T r = c.call();
				return r;
			} catch (Throwable t) {
				if (!allowPartialResult) {
					cancelAll();
				}
				return t;
			}
		});
		tasks.add(task);
	}
	
	private void cancelAll() {
		stop = true;
		tasks.forEach(t -> t.cancel(true));
	}
	
	/**
	 * 取结果. 此阶段任一部分抛异常都会使整个过程被取消.
	 *
	 * @param c               Nullable
	 * @param timeoutMilliSec 整个任务组等待超时. 超时也抛异常.非正表示不超时.
	 * @return
	 */
	public Throwable iterResults(Try.ExpConsumer<T, Throwable> c, long timeoutMilliSec) {
		stop = true;
		long timeLimit = timeoutMilliSec > 0 ? System.currentTimeMillis() + timeoutMilliSec : Long.MAX_VALUE;
		for (Future<Object> task : tasks) {
			try {
				Object r = task.get(timeLimit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
				if (r instanceof Throwable) {
					if (!allowPartialResult) {
						cancelAll();
						return (Throwable) r;
					}
				} else {
					if (c != null) {
						c.accept((T) r);
					}
				}
			} catch (Throwable th) {
				cancelAll();
				return th;
			}
		}
		return null;
	}
}
