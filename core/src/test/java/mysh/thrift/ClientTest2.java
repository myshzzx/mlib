package mysh.thrift;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.nio.ByteBuffer;
import java.util.Date;

/**
 * @author 张智贤
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:mysh/thrift/client.xml")
public class ClientTest2 {
	private static final Logger log = LoggerFactory.getLogger(ClientTest2.class);

	@Autowired
	@Qualifier("client2")
	private ThriftClientFactory.ClientHolder<TService1.Iface> client;

	@Test
	public void test1() throws Exception {

		byte[] b = new byte[]{1, 2, 3};

		for (int i = 0; i < 3; i++)
			new Thread("Client " + i) {
				@Override
				public void run() {
					try {
						System.out.println(new Date() + " - " + this.getName() + ": "
										+ client.getClient().getStr("mysh", ByteBuffer.wrap(b)));
					} catch (Throwable t) {
						log.error(Thread.currentThread().getName(), t);
					}
				}
			}.start();

		Thread.sleep(150000);


		for (int i = 0; i < 1; i++) {
			new Thread("Client " + i) {
				@Override
				public void run() {
					try {
						System.out.println(new Date() + " - " + this.getName() + ": " +
										client.getClient().getStr("xxxx", ByteBuffer.wrap(b)));
					} catch (Throwable t) {
						log.error(Thread.currentThread().getName(), t);
					}
				}
			}.start();
			Thread.sleep(1000);
		}
	}

	@Test
	public void test2() throws Exception {
		while (true) {
			this.test1();
			Thread.sleep(15000);
		}
	}
}
