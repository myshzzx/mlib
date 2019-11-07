package mysh.msg;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.function.Consumer;

/**
 * @since 2019-11-07
 */
public class MsgConsumerProducer implements Closeable {
	
	private MsgConsumer consumer;
	private MsgProducer producer;
	
	public MsgConsumerProducer(
			MsgConsumer.MsgReceiver msgReceiver, int consumerThreadPoolSize,
			RejectedExecutionHandler consumerRejectedHandler,
			MsgProducer.MsgSender msgSender
	) {
		consumer = new MsgConsumer(msgReceiver, consumerThreadPoolSize, consumerRejectedHandler);
		producer = new MsgProducer(msgSender);
	}
	
	public void subscribe(String topic, Consumer<Msg<?>> c) {
		consumer.subscribe(topic, c);
	}
	
	public void produce(Msg<?> msg) throws IOException {
		producer.produce(msg);
	}
	
	@Override
	public void close() {
		try {
			consumer.close();
		} catch (Exception e) { }
		try {
			producer.close();
		} catch (Exception e) {}
	}
}
