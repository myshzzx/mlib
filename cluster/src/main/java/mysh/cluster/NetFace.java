package mysh.cluster;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.UnknownHostException;

/**
 * RelayAddress
 *
 * @author 凯泓(zhixian.zzx@alibaba-inc.com)
 * @since 2017/11/27
 */
public class NetFace {

    private Inet4Address address;
    private short networkPrefixLength;
    private Inet4Address broadcast;
    private boolean isRelay;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetFace netFace = (NetFace) o;

        if (networkPrefixLength != netFace.networkPrefixLength) return false;
        if (!address.equals(netFace.address)) return false;
        return broadcast.equals(netFace.broadcast);
    }

    @Override
    public int hashCode() {
        int result = address.hashCode();
        result = 31 * result + (int) networkPrefixLength;
        result = 31 * result + broadcast.hashCode();
        return result;
    }

    NetFace(String relayAddr) throws UnknownHostException {
        address = (Inet4Address) InetAddress.getByName(relayAddr);
        broadcast = address;
        isRelay = true;
    }

    NetFace(InterfaceAddress addr) {
        address = (Inet4Address) addr.getAddress();
        networkPrefixLength = addr.getNetworkPrefixLength();
        broadcast = (Inet4Address) addr.getBroadcast();
    }

    Inet4Address getAddress() {
        return address;
    }

    short getNetworkPrefixLength() {
        return networkPrefixLength;
    }

    Inet4Address getBroadcast() {
        return broadcast;
    }

    public boolean isRelay() {
        return isRelay;
    }
}
