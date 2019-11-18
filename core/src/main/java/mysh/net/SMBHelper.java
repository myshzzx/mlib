package mysh.net;

import jcifs.Address;
import jcifs.NameServiceClient;
import jcifs.context.SingletonContext;
import jcifs.netbios.NameServiceClientImpl;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @since 2019-11-18
 */
public abstract class SMBHelper {
	public static InetAddress getHostAddressByName(String host) throws UnknownHostException {
		NameServiceClient nsc = new NameServiceClientImpl(SingletonContext.getInstance());
		Address addr = nsc.getByName(host);
		return addr != null ? addr.toInetAddress() : null;
	}
}
