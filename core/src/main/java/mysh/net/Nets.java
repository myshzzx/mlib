package mysh.net;

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;

/**
 * Nets
 *
 * @since 2017/11/28
 */
public class Nets {
	
	public static int ipv4Address2Int(InetAddress a) {
		byte[] addr = a.getAddress();
		if (addr.length != 4)
			throw new IllegalArgumentException("not-ipv4-addr:" + a);
		int ip = addr[3] & 0xFF;
		ip |= ((addr[2] << 8) & 0xFF00);
		ip |= ((addr[1] << 16) & 0xFF0000);
		ip |= ((addr[0] << 24) & 0xFF000000);
		return ip;
	}
	
	public static Inet4Address int2Ipv4Address(int ip) {
		byte[] addr = new byte[4];
		addr[0] = (byte) ((ip >>> 24) & 0xFF);
		addr[1] = (byte) ((ip >>> 16) & 0xFF);
		addr[2] = (byte) ((ip >>> 8) & 0xFF);
		addr[3] = (byte) (ip & 0xFF);
		try {
			return (Inet4Address) Inet4Address.getByAddress(addr);
		} catch (UnknownHostException e) {
			// impossible
			throw new RuntimeException("int2Ipv4Address-error:" + ip, e);
		}
	}
	
	/**
	 * iterate enabled and non-loopback network interfaces.
	 */
	public static void iterateNetworkIF(Consumer<NetworkInterface> c) throws SocketException {
		Enumeration<NetworkInterface> nifEnum = NetworkInterface.getNetworkInterfaces();
		while (nifEnum.hasMoreElements()) {
			NetworkInterface nif = nifEnum.nextElement();
			if (nif.isUp() && !nif.isLoopback()) {
				c.accept(nif);
			}
		}
	}
	
	public static List<InetAddress> getBroadcastAddress() throws SocketException {
		List<InetAddress> ba = new ArrayList<>();
		Nets.iterateNetworkIF(i -> i.getInterfaceAddresses().forEach(
				ia -> {
					if (ia.getBroadcast() != null)
						ba.add(ia.getBroadcast());
				}
		));
		return ba;
	}
}
