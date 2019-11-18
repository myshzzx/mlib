package mysh.msg;

import lombok.extern.slf4j.Slf4j;
import mysh.collect.Colls;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * losable message consumer
 *
 * @since 2019-11-06
 */
@Slf4j
public class MsgConsumer implements Closeable {
	public interface MsgReceiver extends Closeable {
		Msg<?> fetch() throws IOException;
	}
	
	private static RejectedExecutionHandler DEFAULT_REJECTED_EXECUTION_HANDLER = (r, e) -> {
		if (!e.isShutdown()) {
			e.getQueue().poll();
			log.error("MsgConsumer-discard-oldestMsg-byDefault");
			e.execute(r);
		}
	};
	
	private MsgReceiver msgReceiver;
	private ThreadPoolExecutor exec;
	private Map<String, Set<Consumer<Msg<?>>>> consumerMap = new ConcurrentHashMap<>();
	
	public MsgConsumer(MsgReceiver msgReceiver, int threadPoolSize, RejectedExecutionHandler msgRejectedHandler) {
		if (threadPoolSize < 1)
			throw new RuntimeException("threadPoolSize should be positive");
		this.msgReceiver = Objects.requireNonNull(msgReceiver, "msgReceiver can't be null");
		
		AtomicInteger ci = new AtomicInteger();
		exec = new ThreadPoolExecutor(threadPoolSize + 1, threadPoolSize + 1, 1, TimeUnit.MINUTES,
				new LinkedBlockingQueue<>(50),
				r -> new Thread(r, Thread.currentThread().getName() + "-MsgConsumer-" + ci.incrementAndGet()),
				msgRejectedHandler == null ? DEFAULT_REJECTED_EXECUTION_HANDLER : msgRejectedHandler);
		exec.allowCoreThreadTimeOut(true);
		exec.submit(() -> {
			Thread t = Thread.currentThread();
			while (!t.isInterrupted())
				try {
					Msg<?> msg = msgReceiver.fetch();
					String topic = msg.getTopic();
					Set<Consumer<Msg<?>>> consumers = consumerMap.get(topic);
					if (Colls.isNotEmpty(consumers))
						exec.submit(() -> {
							for (Consumer<Msg<?>> consumer : consumers) {
								try {
									consumer.accept(msg);
								} catch (Exception e) {
									log.error("consume-msg-fail", e);
								}
							}
						});
				} catch (Exception e) {
					log.error("fail-on-getMsg", e);
				}
		});
	}
	
	public void subscribe(String topic, Consumer<Msg<?>> c) {
		Set<Consumer<Msg<?>>> consumers = consumerMap.computeIfAbsent(topic, t -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
		consumers.add(c);
	}
	
	@Override
	public void close() throws IOException {
		exec.shutdownNow();
		msgReceiver.close();
	}
	
}
