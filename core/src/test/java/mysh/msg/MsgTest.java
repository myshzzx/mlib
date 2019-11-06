package mysh.msg;

import mysh.util.Times;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;

/**
 * @since 2019-11-06
 */
@Ignore
public class MsgTest {
	
	@Test
	public void produce() throws IOException {
		MsgProducer msgProducer = new MsgProducer();
		while (true) {
			msgProducer.produce("abc", System.currentTimeMillis());
			Times.sleepNoExp(1000);
		}
	}
	
	@Test
	public void consume() throws SocketException, InterruptedException {
		MsgConsumer msgConsumer = new MsgConsumer();
		msgConsumer.subscribe("abc", System.out::println);
		new CountDownLatch(1).await();
	}
}