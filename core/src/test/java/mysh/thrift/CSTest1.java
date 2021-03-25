package mysh.thrift;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.layered.TFramedTransport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Mysh
 * @since 2014/11/11 1:39
 */
@Disabled
public class CSTest1 {
	@Test
	public void server() {
		try {
			TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(19090);
			TService1.Processor processor = new TService1.Processor(new Service1Impl());

			THsHaServer server = new THsHaServer(
							new THsHaServer.Args(serverTransport)
											.processor(processor)
											.protocolFactory(new TCompactProtocol.Factory())
			);
			System.out.println("Starting server on port 19090 ...");
			server.serve();
		} catch (TTransportException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void client() throws Exception {
		TTransport transport;
		try {
			transport = new TFramedTransport(new TSocket("localhost", 19090));
			TProtocol protocol = new TCompactProtocol(transport);

			TService1.Client client = new TService1.Client(protocol);
			transport.open();

			while (true) {
				try {
					System.out.println(client.getStr("mysh", null));
					Thread.sleep(1000);
				} catch (Exception e) {

				}
			}

//			transport.close();
		} catch (TException e) {
			e.printStackTrace();
		}
	}
}
