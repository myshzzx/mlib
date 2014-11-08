package mysh.cluster;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Mysh
 * @since 14-2-25 下午5:56
 */
@Ignore
public class SockUtilTest {
	@Test
	public void testIsInTheSameBroadcastDomain() throws Exception {
		String[] ips = {
						"192.168.1.124",
						"172.16.12.46",
						"172.16.10.46",
						"172.16.1.46",
						"192.168.58.1",
						"192.168.58.115",
						"192.168.8.115"
		};
		SockUtil.iterateNetworkIF(nif -> {
			nif.getInterfaceAddresses().forEach(addr -> {
				Arrays.stream(ips).forEach(ip -> {
					boolean r = SockUtil.isInTheSameBroadcastDomain(addr, ip, addr.getNetworkPrefixLength());
					System.out.println((r ? "same" : "diff") + "\t\t" + addr + " " + ip);
				});
				System.out.println();
			});
		});
	}
}
