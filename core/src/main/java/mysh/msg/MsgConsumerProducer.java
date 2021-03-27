package mysh.msg;

import mysh.collect.Pair;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Collection;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.function.Consumer;

/**
 * @since 2019-11-07
 */
public interface MsgConsumerProducer extends Closeable {
	
	void subscribe(String topic, Consumer<Msg<?>> c);
	
	void produce(Msg<?> msg) throws IOException;
	
	void close();
	
	static MsgConsumerProducer createUdp(
			int udpPort,
			int consumerThreadPoolSize,
			RejectedExecutionHandler consumerRejectedHandler,
			@Nullable Collection<SocketAddress> repeaters) throws SocketException {
		
		Pair<MsgConsumer.MsgReceiver, MsgProducer.MsgSender> rsp = UdpUtil.generateUdpReceiverSender(udpPort, repeaters);
		MsgConsumer consumer = new MsgConsumer(rsp.getL(), consumerThreadPoolSize, consumerRejectedHandler);
		MsgProducer producer = new MsgProducer(rsp.getR());
		
		return new MsgConsumerProducer() {
			@Override
			public void subscribe(final String topic, final Consumer<Msg<?>> c) {
				consumer.subscribe(topic, c);
			}
			
			@Override
			public void produce(final Msg<?> msg) throws IOException {
				producer.produce(msg);
			}
			
			@Override
			public void close() {
				consumer.close();
				rsp.getL().close();
				rsp.getR().close();
			}
		};
	}
	
}
