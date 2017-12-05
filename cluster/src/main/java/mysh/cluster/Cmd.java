package mysh.cluster;

import mysh.collect.ConcurrentBiMap;
import mysh.net.Nets;
import mysh.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mysh
 * @since 14-2-3 下午2:42
 */
class Cmd implements Serializable, Cloneable {
    private static final Logger log = LoggerFactory.getLogger(Cmd.class);
    private static final long serialVersionUID = 1560510216235141874L;

    enum Action {
        WHO_IS_THE_MASTER_BY_WORKER((byte) 1),
        WHO_IS_THE_MASTER_BY_CLIENT((byte) 2),
        I_AM_THE_MASTER((byte) 3),
        I_AM_A_WORKER((byte) 4),
        CHECK_MASTER((byte) 5),
        REINIT((byte) 6),
        RELAY_ACK((byte) 8),
        SHUTTING_DOWN((byte) 9);

        byte code;

        Action(byte code) {
            this.code = code;
        }

        static Map<Byte, Action> actionMap = new HashMap<>();

        static {
            Arrays.stream(Action.values()).forEach(a -> {
                actionMap.put(a.code, a);
            });
        }

        static Action from(byte code) {
            return actionMap.get(code);
        }
    }

    transient long receiveTime;

    transient Action action;
    /** use this in transfer to reduce serialized data size */
    private final byte transAction;

    transient String id;
    /** use this in transfer to reduce serialized data size */
    private final long transId;

    final long startTime;

    transient String ipAddr;
    /** if positive, indicating this msg comes from the specified ip, or the msg comes from udp sender */
    private int transIpAddr;

    byte deliverFlag;

    final int masterPort;
    final int workerPort;

    Cmd(Action action, String id, long startTime, int masterPort, int workerPort) {
        this.action = action;
        this.transAction = action.code;
        this.id = id;
        this.transId = id2Long(id);
        this.startTime = startTime;
        this.masterPort = masterPort;
        this.workerPort = workerPort;
    }

    void initAfterReceive(DatagramPacket udpPack) {
        //noinspection ConstantConditions
        if (transIpAddr == 0) {
            // this cmd is original msg
            transIpAddr = Nets.ipv4Address2Int(udpPack.getAddress());
            ipAddr = udpPack.getAddress().getHostAddress();
        } else {
            ipAddr = Nets.int2Ipv4Address(transIpAddr).getHostAddress();
        }

        if (deliverFlag > 0) {
            deliverFlag++;
        }

        this.id = idFromLong(transId);
        this.action = Action.from(this.transAction);
        this.receiveTime = System.currentTimeMillis();
    }

    /** map idInt to idStr, like 4038324275928345-> 23.123.13.56_89347324323 */
    private static ConcurrentBiMap<Long, String> idMap = ConcurrentBiMap.create();

    private long id2Long(String idStr) {
        if (Strings.isBlank(idStr))
            return 0;
        else {
            return idMap.getReverse().computeIfAbsent(idStr, is -> {
                try {
                    String[] ids = is.split("_");
                    long ip = Nets.ipv4Address2Int(Inet4Address.getByName(ids[0]));
                    return ip << 32 | Long.parseLong(ids[1]);
                } catch (Exception e) {
                    throw new RuntimeException("id2Long-error:" + is, e);
                }
            });
        }
    }

    private String idFromLong(long transId) {
        if (transId == 0)
            return "";
        else
            return idMap.computeIfAbsent(transId, id -> {
                long timeFlag = id & 0xffffffffL;
                String addr = Nets.int2Ipv4Address((int) (id >>> 32)).getHostAddress();
                return addr + "_" + timeFlag;
            });
    }

    @Override
    public Cmd clone() {
        try {
            Object c = super.clone();
            return (Cmd) c;
        } catch (Exception e) {
            log.error("clone-cmd-error", e);
            return null;
        }
    }

    void prepareDeliver() {
        if (deliverFlag == 0)
            deliverFlag = 1;
    }

    @Override
    public String toString() {
        return "Cmd{" +
                "action=" + action +
                ", id='" + id + '\'' +
                ", startTime=" + startTime +
                ", ipAddr='" + ipAddr + '\'' +
                ", deliverFlag=" + deliverFlag +
                ", masterPort=" + masterPort +
                ", workerPort=" + workerPort +
                '}';
    }
}
