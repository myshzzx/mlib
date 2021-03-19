package mysh.cluster;

import mysh.net.Nets;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import java.net.SocketException;
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
        Nets.iterateNetworkIF(nif -> {
            nif.getInterfaceAddresses().forEach(addr -> {
                Arrays.stream(ips).forEach(ip -> {
                    boolean r = SockUtil.isInTheSameBroadcastDomain(new NetFace(addr), ip, addr.getNetworkPrefixLength());
                    System.out.println((r ? "same" : "diff") + "\t\t" + addr + " " + ip);
                });
                System.out.println();
            });
        });
    }

    @Test
    public void iteratTest() throws SocketException {
        Nets.iterateNetworkIF(nif -> {
            nif.getInterfaceAddresses().stream()
                    .filter(addr -> addr.getBroadcast() != null && addr.getAddress().getAddress().length == 4)
                    .forEach(addr -> {
                        System.out.println(addr + "---bc--->" + addr.getBroadcast());
                    });
        });
    }
}
