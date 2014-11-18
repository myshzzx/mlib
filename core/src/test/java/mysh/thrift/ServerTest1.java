package mysh.thrift;

import org.apache.thrift.server.TServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author: 张智贤
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:mysh/thrift/server.xml")
public class ServerTest1 {

	@Autowired
	private TServer server;

	@Test
	public void test1() throws Exception {
		server.serve();
	}

}
