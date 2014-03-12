package mysh.thrift;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author 张智贤
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:mysh/thrift/client.xml")
public class ClientTest1 {
	@Autowired
	@Qualifier("client1")
	private TService1.Iface client;

	@Test
	public void test1() throws InterruptedException {

		for (int i = 0; i < 1; i++)
			new Thread("Client " + i) {
				@Override
				public void run() {
					try {
						System.out.println(this.getName() + ": " + client.getStr("mysh", 234));
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}.start();

		Thread.sleep(60000);
	}

	@Test
	public void test2() throws InterruptedException {
		while (true) {
			this.test1();
		}
	}
}
