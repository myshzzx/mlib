package mysh.msg;

import mysh.util.Times;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;

/**
 * @since 2019-11-06
 */
@Disabled
public class MsgTest {
	
	@Test
	public void produce() throws IOException {
		MsgProducer msgProducer = new MsgProducer(UdpUtil.generateUdpSender(3333, 1000));
		while (true) {
			msgProducer.produce(new Msg<>("abc", System.currentTimeMillis()));
			Times.sleepNoExp(1000);
		}
	}
	
	@Test
	public void consume() throws SocketException, InterruptedException {
		MsgConsumer msgConsumer = new MsgConsumer(UdpUtil.generateUdpReceiver(3333, 1000), 2, null);
		msgConsumer.subscribe("abc", System.out::println);
		new CountDownLatch(1).await();
	}
}