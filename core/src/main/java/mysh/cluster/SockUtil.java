package mysh.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.function.Consumer;

/**
 * @author Mysh
 * @since 14-2-3 下午2:37
 */
class SockUtil {
	private static final Logger log = LoggerFactory.getLogger(SockUtil.class);

	static Cmd receiveCmd(DatagramSocket cmdSock, final DatagramPacket reusePack) throws Exception {
		synchronized (reusePack) {
			cmdSock.receive(reusePack);
			try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(
							reusePack.getData(), reusePack.getOffset(), reusePack.getLength()))) {
				return (Cmd) in.readObject();
			}
		}
	}

	static Cmd receiveCmd(DatagramSocket cmdSock, int bufSize) throws Exception {
		byte[] buf = new byte[bufSize];
		DatagramPacket p = new DatagramPacket(buf, 0, buf.length);
		return receiveCmd(cmdSock, p);
	}

	static void sendCmd(DatagramSocket cmdSock, Cmd c, InetAddress addr, int port) throws IOException {
		try (ByteArrayOutputStream baOut = new ByteArrayOutputStream();
		     ObjectOutputStream out = new ObjectOutputStream(baOut)) {
			out.writeObject(c);
			DatagramPacket p = new DatagramPacket(baOut.toByteArray(), 0, baOut.size());
			p.setAddress(addr);
			p.setPort(port);
			cmdSock.send(p);
		}
	}

	/**
	 * iterate enable and non-loopback network interfaces.
	 */
	static void iterateNetworkIF(Consumer<NetworkInterface> c) throws SocketException {
		Enumeration<NetworkInterface> nifEnum = NetworkInterface.getNetworkInterfaces();
		while (nifEnum.hasMoreElements()) {
			NetworkInterface nif = nifEnum.nextElement();
			if (nif.isUp() && !nif.isLoopback()) {
				c.accept(nif);
			}
		}
	}

	/**
	 * is given ip and ifa in the same broadcast domain.
	 * WARNING: need ipv4.
	 *
	 * @param ifa    interface address.
	 * @param ip     given ip.
	 * @param ipMask used on ip, but not ifa.
	 * @return <code>true</code> if ifa and ip are ipv4 addresses and in the same domain,
	 * <code>false</code> otherwise.
	 */
	static boolean isInTheSameBroadcastDomain(InterfaceAddress ifa, String ip, short ipMask) {
		int nifDomain = 0, ipDomain = 0;

		byte[] nifAddr = ifa.getAddress().getAddress();
		if (nifAddr.length != 4) return false;

		nifDomain |= (nifAddr[0] + 256) % 256 << 24;
		nifDomain |= (nifAddr[1] + 256) % 256 << 16;
		nifDomain |= (nifAddr[2] + 256) % 256 << 8;
		nifDomain |= (nifAddr[3] + 256) % 256;
		nifDomain &= -1 << (32 - ifa.getNetworkPrefixLength());

		int[] ipAddr = Arrays.stream(ip.split("\\.")).mapToInt(Integer::parseInt).toArray();
		if (ipAddr.length != 4) return false;

		ipDomain |= ipAddr[0] << 24;
		ipDomain |= ipAddr[1] << 16;
		ipDomain |= ipAddr[2] << 8;
		ipDomain |= ipAddr[3];
		ipDomain &= -1 << (32 - ipMask);

		return nifDomain == ipDomain;
	}
}
