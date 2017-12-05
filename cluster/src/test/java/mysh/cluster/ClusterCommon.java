package mysh.cluster;

import mysh.cluster.starter.ClusterStart;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * ClusterCommon
 *
 * @author 凯泓(zhixian.zzx@alibaba-inc.com)
 * @since 2017/11/27
 */
public class ClusterCommon {
    private static final Logger log = LoggerFactory.getLogger(ClusterCommon.class);

    @Test
    public void t() throws Throwable {
        ClusterStart.main(null);
    }

    @Test
    public void udp() throws IOException {
        DatagramSocket udp = new DatagramSocket(22222);
        DatagramPacket p = new DatagramPacket(new byte[1000], 1000);
        while (true) {
            udp.receive(p);
            log.info("receive,ip={},data={}", p.getAddress().getHostAddress(), new String(p.getData()));
        }
    }

}
