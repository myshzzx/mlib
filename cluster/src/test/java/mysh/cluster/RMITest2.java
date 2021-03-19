package mysh.cluster;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Mysh
 * @since 2014/12/10 17:31
 */
public class RMITest2 {
	public static interface RI extends Remote {
		String greet(String name) throws RemoteException;
	}

	private static class RIImpl implements RI {

		@Override
		public String greet(String name) throws RemoteException {
			return "hello " + name;
		}
	}

	private int port = 8030;

	@Test
	public void server() throws RemoteException, AlreadyBoundException, InterruptedException {

		Registry registry = LocateRegistry.createRegistry(port);

		BlockingQueue<Socket> socks = new LinkedBlockingQueue<>();

		RIImpl ro = new RIImpl();
		Remote expRO = UnicastRemoteObject.exportObject(ro, port + 1, null, p -> new ServerSocket(p) {
			@Override
			public Socket accept() throws IOException {
				final Socket accept = super.accept();
				socks.offer(accept);
				return accept;
			}
		});
		registry.bind("ro", expRO);

		new Thread() {
			@Override
			public void run() {
				Socket sock;
				while (true) {
					try {
						Thread.sleep(500);
						sock = socks.take();
						if (sock.isClosed())
							System.out.println("sock closed");
						else
							socks.offer(sock);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}.start();

		Thread.sleep(100000000);
	}

	@Test
	public void client() throws RemoteException, NotBoundException {
		Registry registry = LocateRegistry.getRegistry("localhost", port);
		final RI ri = (RI) registry.lookup("ro");
		System.out.println(ri.greet("mysh"));
	}
}
