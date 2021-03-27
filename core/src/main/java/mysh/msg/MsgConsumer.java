package mysh.msg;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import mysh.collect.Colls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketException;
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
public class MsgConsumer implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(MsgConsumer.class);
	
	public interface MsgReceiver extends Closeable {
		Msg<?> fetch() throws IOException;
		
		void close();
	}
	
	private static RejectedExecutionHandler DEFAULT_REJECTED_EXECUTION_HANDLER = (r, e) -> {
		if (!e.isShutdown()) {
			e.getQueue().poll();
			log.error("MsgConsumer-discard-oldestMsg-byDefault");
			e.execute(r);
		}
	};
	
	private ThreadPoolExecutor exec;
	private Map<String, Set<Consumer<Msg<?>>>> consumerMap = new ConcurrentHashMap<>();
	
	public MsgConsumer(MsgReceiver msgReceiver, int threadPoolSize, RejectedExecutionHandler msgRejectedHandler) {
		if (threadPoolSize < 1)
			throw new RuntimeException("threadPoolSize should be positive");
		Objects.requireNonNull(msgReceiver, "msgReceiver can't be null");
		
		AtomicInteger ci = new AtomicInteger();
		String threadName = Thread.currentThread().getName();
		exec = new ThreadPoolExecutor(threadPoolSize + 1, threadPoolSize + 1, 1, TimeUnit.MINUTES,
				new LinkedBlockingQueue<>(50),
				r -> new Thread(r, threadName + "-MsgConsumer-" + ci.incrementAndGet()),
				msgRejectedHandler == null ? DEFAULT_REJECTED_EXECUTION_HANDLER : msgRejectedHandler);
		exec.allowCoreThreadTimeOut(true);
		exec.submit(() -> {
			try {
				Thread t = Thread.currentThread();
				Cache<Object, Object> msgIdCache = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.SECONDS).build();
				while (!t.isInterrupted())
					try {
						Msg<?> msg = msgReceiver.fetch();
						if (msgIdCache.asMap().putIfAbsent(msg.getId(), true) != null)
							continue;
						
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
						if (!(e instanceof SocketException && Objects.equals(e.getMessage(), "socket closed")))
							log.error("fail-on-getMsg", e);
					}
			} catch (Throwable t) {
				log.error("consume-msg-error", t);
			}
		});
	}
	
	public void subscribe(String topic, Consumer<Msg<?>> c) {
		Set<Consumer<Msg<?>>> consumers = consumerMap.computeIfAbsent(topic, t -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
		consumers.add(c);
	}
	
	public void close() {
		exec.shutdownNow();
	}
	
}
