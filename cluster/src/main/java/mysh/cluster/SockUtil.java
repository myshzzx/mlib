package mysh.cluster;

import mysh.cluster.rpc.thrift.RpcUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * @author Mysh
 * @since 14-2-3 下午2:37
 */
class SockUtil {
    static Cmd receiveCmd(DatagramSocket cmdSock, final DatagramPacket reusePack) throws Throwable {
        synchronized (reusePack) {
            cmdSock.receive(reusePack);
            Cmd cmd = RpcUtil.s.deserialize(reusePack.getData(), reusePack.getOffset(), reusePack.getLength(), null);
            cmd.initAfterReceive(reusePack);
            return cmd;
        }
    }

    static Cmd receiveCmd(DatagramSocket cmdSock, int bufSize) throws Throwable {
        byte[] buf = new byte[bufSize];
        DatagramPacket p = new DatagramPacket(buf, 0, buf.length);
        return receiveCmd(cmdSock, p);
    }

    static void sendCmd(DatagramSocket cmdSock, Cmd c, InetAddress addr, int port) throws IOException {
        final byte[] buf = RpcUtil.s.serialize(c);
        DatagramPacket p = new DatagramPacket(buf, 0, buf.length);
        p.setAddress(addr);
        p.setPort(port);
        cmdSock.send(p);
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
    public static boolean isInTheSameBroadcastDomain(NetFace ifa, String ip, short ipMask) {
        int nifDomain = 0, ipDomain = 0;
    
        byte[] nifAddr = ifa.getAddress().getAddress();
        if (nifAddr.length != 4)
            return false;
    
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
