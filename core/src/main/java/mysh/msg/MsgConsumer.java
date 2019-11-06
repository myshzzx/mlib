package mysh.msg;

import lombok.extern.slf4j.Slf4j;
import mysh.collect.Colls;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * losable message consumer
 *
 * @since 2019-11-06
 */
@Slf4j
public class MsgConsumer implements Closeable {
	public interface MsgSupplier extends Closeable {
		Msg<?> fetch() throws IOException;
	}
	
	private static MsgSupplier DEFAULT_UDP_SUPPLIER;
	
	private static RejectedExecutionHandler DEFAULT_REJECTED_EXECUTION_HANDLER = (r, e) -> {
		if (!e.isShutdown()) {
			e.getQueue().poll();
			log.error("MsgConsumer-discard-oldestMsg-byDefault");
			e.execute(r);
		}
	};
	
	private MsgSupplier msgSupplier;
	private ExecutorService exec;
	private Map<String, Set<Consumer<Msg<?>>>> consumerMap = new ConcurrentHashMap<>();
	
	public MsgConsumer() throws SocketException {
		this(getDefaultUdpSupplier(), Runtime.getRuntime().availableProcessors(), DEFAULT_REJECTED_EXECUTION_HANDLER);
	}
	
	public MsgConsumer(MsgSupplier msgSupplier, int threadCount, RejectedExecutionHandler msgRejectedHandler) {
		if (threadCount < 1)
			throw new RuntimeException("threadCount should be positive");
		this.msgSupplier = Objects.requireNonNull(msgSupplier, "msgSupplier can't be null");
		
		AtomicInteger ci = new AtomicInteger();
		exec = new ThreadPoolExecutor(1, threadCount + 1, 1, TimeUnit.MINUTES,
				new LinkedBlockingQueue<>(100),
				r -> new Thread(r, "MsgConsumer-" + ci.incrementAndGet()),
				msgRejectedHandler);
		exec.submit(() -> {
			Thread t = Thread.currentThread();
			while (!t.isInterrupted())
				try {
					Msg<?> msg = msgSupplier.fetch();
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
		msgSupplier.close();
	}
	
	private static MsgSupplier getDefaultUdpSupplier() throws SocketException {
		synchronized (MsgConsumer.class) {
			if (DEFAULT_UDP_SUPPLIER == null) {
				DEFAULT_UDP_SUPPLIER = DefaultUdpUtil.generateUdpConsumer(
						DefaultUdpUtil.DEFAULT_PORT, DefaultUdpUtil.DEFAULT_UDP_PACK_BUF);
			}
			return DEFAULT_UDP_SUPPLIER;
		}
	}
}
