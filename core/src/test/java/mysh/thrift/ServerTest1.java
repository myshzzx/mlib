package mysh.thrift;

import org.apache.thrift.server.TServer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:mysh/thrift/server.xml")
@Disabled
public class ServerTest1 {

	@Autowired
	@Qualifier("server1")
	private TServer s1;

	@Test
	public void test1() throws Exception {
		s1.serve();
	}

}
