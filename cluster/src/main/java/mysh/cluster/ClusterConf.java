package mysh.cluster;

import mysh.util.PropConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.StringJoiner;

/**
 * cluster configuration.
 *
 * @author Mysh
 * @since 2014/12/12 21:47
 */
public class ClusterConf implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(ClusterConf.class);

    static final long serialVersionUID = -8940782299543664345L;
    private static final String filePath = "cluster.properties";

    /**
     * cluster node id. should be unique within a cluster.<br/>
     * if it's null, then the node will generate one.
     */
    String id;
    private static final String _id = "id";

    /**
     * cluster start time.
     */
    Long startTime;
    private static final String _startTime = "startTime";

    /**
     * cmd port. UDP(cmdPort) will be used in broadcast communication,
     * while TCP(cmdPort) in services dispatching,
     * TCP(cmdPort+1) in Master-Node service and TCP(cmdPort+2) in Worker-Node service.
     */
    Integer cmdPort;
    private static final String _cmdPort = "cmdPort";

    /**
     * rpc server pool size. usually used by client, also used by worker node
     * when update files. can be 0.<br/>
     * see {@link mysh.thrift.ThriftServerFactory#setServerPoolSize}
     */
    Integer serverPoolSize;
    private static final String _serverPoolSize = "serverPoolSize";

    /**
     * heart beat interval time in milliseconds.
     */
    Integer heartBeatTime;
    private static final String _heartBeatTime = "heartBeatTime";

    /**
     * Not implement now. rpc service use TLS(bidirectional authentication) or not .
     */
    Boolean useTLS = Boolean.FALSE;
    private static final String _useTLS = "useTLS";

    /**
     * cmd relays. used to send cmds across sub-net.
     */
    String[] relays = new String[0];
    private static final String _relays = "relays";

    /** when receive delivered cmd, broadcast it at the same time */
    Boolean broadcastDeliveredCmd;
    private static final String _broadcastDeliveredCmd = "broadcastDeliveredCmd";

    /**
     * initial state of the worker, which can be updated and sent to master node automatically.
     * can be <b>null</b>.
     */
    WorkerState initState;

    /**
     * get config from properties file.
     */
    public static ClusterConf readConf() {
        ClusterConf c = new ClusterConf();

        try {
            final PropConf propConf = new PropConf(filePath);
            c.id = propConf.getPropString(_id);
            c.startTime = propConf.getPropLong(_startTime, System.currentTimeMillis());
            c.cmdPort = propConf.getPropInt(_cmdPort, 8030);
            c.serverPoolSize = propConf.getPropInt(_serverPoolSize, 20);
            c.heartBeatTime = propConf.getPropInt(_heartBeatTime, 10_000);
            c.useTLS = propConf.getPropBoolean(_useTLS, false);
            c.relays = propConf.getPropString(_relays, "").trim().split("[,; ]+");
            c.broadcastDeliveredCmd = propConf.getPropBoolean(_broadcastDeliveredCmd, false);
        } catch (Throwable e) {
            log.error("read-cluster-conf-from-file-error.", e);
        }

        return c;
    }

    /**
     * save config to file.
     */
    public void save() {
        try {
            final PropConf propConf = new PropConf();
            propConf.putProp(_id, id);
            propConf.putProp(_startTime, startTime);
            propConf.putProp(_cmdPort, cmdPort);
            propConf.putProp(_serverPoolSize, serverPoolSize);
            propConf.putProp(_heartBeatTime, heartBeatTime);
            propConf.putProp(_useTLS, useTLS);

            StringJoiner cj = new StringJoiner(",");
            Arrays.stream(relays).forEach(cj::add);
            propConf.putProp(_relays, cj.toString());

            propConf.putProp(_broadcastDeliveredCmd, broadcastDeliveredCmd);

            propConf.save2File(filePath);
        } catch (IOException e) {
            log.error("save-cluster-conf-error.", e);
        }
    }

    public ClusterConf setCmdPort(int cmdPort) {
        this.cmdPort = cmdPort;
        return this;
    }

    public ClusterConf setServerPoolSize(int serverPoolSize) {
        this.serverPoolSize = serverPoolSize;
        return this;
    }

    public ClusterConf setHeartBeatTime(int heartBeatTime) {
        this.heartBeatTime = heartBeatTime;
        return this;
    }

    public ClusterConf setUseTLS(boolean useTLS) {
        this.useTLS = useTLS;
        return this;
    }

    public ClusterConf setRelays(String[] relays) {
        if (relays != null)
            this.relays = relays;
        return this;
    }

    public ClusterConf setBroadcastDeliveredCmd(Boolean broadcastDeliveredCmd) {
        this.broadcastDeliveredCmd = broadcastDeliveredCmd;
        return this;
    }

    @Override
    public String toString() {
        return "ClusterConf{" +
                "id='" + id + '\'' +
                ", startTime=" + startTime +
                ", cmdPort=" + cmdPort +
                ", serverPoolSize=" + serverPoolSize +
                ", heartBeatTime=" + heartBeatTime +
                ", useTLS=" + useTLS +
                ", relays=" + Arrays.toString(relays) +
                ", broadcastDeliveredCmd=" + broadcastDeliveredCmd +
                ", initState=" + initState +
                '}';
    }
}
