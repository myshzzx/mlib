package mysh.cluster;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * @author Mysh
 * @since 14-2-3 下午3:57
 */
@Disabled
public class UDPTest {

	@Test
	public void udpTest() throws SocketException {
		DatagramSocket sock = new DatagramSocket(0);

		System.out.println(sock.getBroadcast());
		System.out.println(sock.getReceiveBufferSize());
		System.out.println(sock.getReuseAddress());
		System.out.println(sock.getSendBufferSize());

		System.out.println();
		sock.setReceiveBufferSize(5 * 1024 * 1024);
		sock.setSendBufferSize(1024 * 1024);
		System.out.println(sock.getReceiveBufferSize());
		System.out.println(sock.getSendBufferSize());
		System.out.println(sock.getSoTimeout());
	}

	private static final int PACK_COUNT = 2000;
	private static final int PORT = 8060;
	private static final byte[] buf = new byte[500];


	@Test
	public void udpSend() throws Exception {

		DatagramSocket sock = new DatagramSocket(PORT);
		sock.setSendBufferSize(10 * 1024 * 1024);

		int i = 0;
		DatagramPacket p = new DatagramPacket(buf, 0, buf.length);
		p.setAddress(InetAddress.getByName("192.168.1.255"));
		p.setPort(PORT);
		while (i++ < PACK_COUNT) {
			sock.send(p);
			Thread.sleep(10);
		}
	}

	@Test
	public void udpReceive() throws Exception {

		DatagramSocket sock = new DatagramSocket(PORT);
		sock.setReceiveBufferSize(20 * 1024 * 1024);
		DatagramPacket p = new DatagramPacket(buf, 0, buf.length);
		int i = 0;
		while (true) {
			sock.receive(p);
			Thread.sleep(25);
			System.out.println(++i);
		}
	}

	public static void main(String[] args) throws Exception {
		new UDPTest().udpReceive();
	}
}
