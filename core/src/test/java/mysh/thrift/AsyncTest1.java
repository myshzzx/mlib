package mysh.thrift;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingSocket;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * @author Mysh
 * @since 2014/11/21 15:08
 */
@Ignore
public class AsyncTest1 {
	private static final Logger log = LoggerFactory.getLogger(AsyncTest1.class);

	@Test
	public void test2() throws Exception {
		TNonblockingServerSocket serverTransport = new TNonblockingServerSocket(
						new InetSocketAddress("l", 19000), 0);
		TServer server = new THsHaServer(
						new THsHaServer.Args(serverTransport)
										.processor(new TService1.AsyncProcessor<TService1.AsyncIface>(new Service2Impl()))
										.protocolFactory(new TCompactProtocol.Factory()));
		new Thread() {
			@Override
			public void run() {
				server.serve();
			}
		}.start();

		Thread.sleep(1000);
		TNonblockingSocket transport = new TNonblockingSocket("l", 19000, 5000);
		TService1.AsyncIface client = new TService1.AsyncClient(new TCompactProtocol.Factory(),
						new TAsyncClientManager(), transport);

		byte[] b = {1, 2, 3};
		AsyncMethodCallback<TService1.AsyncClient.getStr_call> h = new AsyncMethodCallback<TService1.AsyncClient.getStr_call>() {

			@Override
			public void onComplete(TService1.AsyncClient.getStr_call response) {
				try {
					System.out.println(response.getResult());
				} catch (TException e) {
					log.error("async get rsp fail.", e);
				}
			}

			@Override
			public void onError(Exception e) {
				log.error("async call fail.", e);
			}
		};

		for (int i = 0; i < 2; i++) {
			client.getStr("mysh", ByteBuffer.wrap(b), h);
		}

		Thread.sleep(10000000);
	}
}
