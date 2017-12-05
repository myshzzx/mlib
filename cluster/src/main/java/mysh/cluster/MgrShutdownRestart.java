package mysh.cluster;

import mysh.cluster.ClusterClient.SRTarget;
import mysh.cluster.ClusterClient.SRType;
import mysh.collect.Colls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Mysh
 * @since 2014/12/14 19:28
 */
final class MgrShutdownRestart extends IClusterMgr<String, String, String, String> {
    private static final long serialVersionUID = 6411057795287576274L;
    private static final Logger log = LoggerFactory.getLogger(MgrShutdownRestart.class);

    private final SRType srType;
    private final SRTarget target;
    private List<String> specNodes;
    private transient boolean closeMaster;

    /**
     * @param srType    shutdown/restart type
     * @param target    target type
     * @param specNodes specified nodes. will be ignored if target is
     *                  {@link SRTarget#EntireCluster} or {@link SRTarget#MasterOnly}.
     */
    MgrShutdownRestart(SRType srType, SRTarget target,
                       List<String> specNodes) {
        this.srType = srType;
        this.target = target;
        this.specNodes = specNodes;
    }

    @Override
    public SubTasksPack<String> fork(String task, String masterNode, List<String> workerNodes) {
        switch (target) {
            case EntireCluster:
                closeMaster = true;
                specNodes = null;
                workerNodes.remove(masterNode);
                return pack(Colls.fillNull(workerNodes.size()), workerNodes);
            case MasterOnly:
                closeMaster = true;
                return pack(Collections.emptyList(), Collections.emptyList());
            case MasterAndSpecified:
                closeMaster = true;
                if (specNodes == null)
                    return pack(Collections.emptyList(), Collections.emptyList());
                else {
                    ArrayList<String> sNodes = new ArrayList<>(specNodes);
                    specNodes = null;
                    sNodes.remove(masterNode);
                    return pack(Colls.fillNull(sNodes.size()), sNodes);
                }
            case Specified:
                if (specNodes == null)
                    return pack(Collections.emptyList(), Collections.emptyList());
                else {
                    ArrayList<String> sNodes = new ArrayList<>(specNodes);
                    specNodes = null;
                    closeMaster = sNodes.remove(masterNode);
                    return pack(Colls.fillNull(sNodes.size()), sNodes);
                }
            default:
                throw new RuntimeException("unknown target: " + target);
        }
    }

    @Override
    public String procSubTask(String subTask, int timeout) throws InterruptedException {
        log.info(srType + " worker-node.");
        worker.shutdownVM(srType == SRType.Restart);
        return "";
    }

    @Override
    public String join(String masterNode, List<String> assignedNodeIds, List<String> subResults) {
        if (closeMaster) {
            log.info(srType + " master-node.");
            master.shutdownVM(srType == SRType.Restart);
        }
        return null;
    }

    @Override
    public String toString() {
        return "MgrShutdownRestart{" +
                "srType=" + srType +
                ", target=" + target +
                ", specNodes=" + specNodes +
                '}';
    }
}
