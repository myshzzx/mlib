package mysh.cluster;

import mysh.collect.Colls;

import java.util.List;
import java.util.Objects;

/**
 * update cluster config.
 * only affects current running cluster nodes, and one node needs restart to make the new config work.
 *
 * @author Mysh
 * @since 2014/12/21 23:38
 */
final class MgrUpdateConf extends IClusterMgr<String, String, String, String> {
    private static final long serialVersionUID = 5113614870089299846L;

    private ClusterConf conf;

    MgrUpdateConf(ClusterConf conf) {
        Objects.requireNonNull(conf);
        this.conf = conf;
    }

    @Override
    public SubTasksPack<String> fork(String task, String masterNode, List<String> workerNodes) {
        return pack(Colls.fillNull(workerNodes.size()), workerNodes);
    }

    @Override
    public String procSubTask(String subTask, int timeout) throws InterruptedException {
        ClusterConf currentConf = ClusterConf.readConf();
        if (conf.cmdPort != null && conf.cmdPort > 0)
            currentConf.cmdPort = conf.cmdPort;
        if (conf.heartBeatTime != null && conf.heartBeatTime > 0)
            currentConf.heartBeatTime = conf.heartBeatTime;
        if (conf.serverPoolSize != null && conf.serverPoolSize > 0)
            currentConf.serverPoolSize = conf.serverPoolSize;
        if (conf.useTLS != null)
            currentConf.useTLS = conf.useTLS;
        if (conf.relays != null)
            currentConf.relays = conf.relays;
        if (conf.broadcastDeliveredCmd != null)
            currentConf.broadcastDeliveredCmd = conf.broadcastDeliveredCmd;
        currentConf.save();
        return "";
    }

    @Override
    public String join(String masterNode, List<String> assignedNodeIds, List<String> subResults) {
        return null;
    }

    @Override
    public String toString() {
        return "MgrUpdateConf{" +
                "conf=" + conf +
                '}';
    }
}
