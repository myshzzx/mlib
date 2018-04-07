package mysh.cluster;

import mysh.cluster.FilesMgr.FileType;
import mysh.cluster.FilesMgr.UpdateType;
import mysh.cluster.rpc.IFaceHolder;
import mysh.cluster.rpc.thrift.RpcUtil;
import mysh.collect.Colls;
import mysh.net.Nets;
import mysh.util.Exps;
import mysh.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.DatagramSocket;
import java.net.InterfaceAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Mysh
 * @since 14-2-23 下午2:21
 */
public final class ClusterClient implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(ClusterClient.class);
    private static final int CMD_SOCK_BUF = 1024 * 1024;

    private volatile boolean running = true;
    private final int cmdPort;
    private final DatagramSocket cmdSock;
    /**
     * network interface addresses with broadcast address.
     */
    private final Set<NetFace> bcAdds = new HashSet<>();
    private volatile IFaceHolder<IClusterService> service;

    private final Set<String> masterCandidates = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * it takes expensive cost to create such a client, so reuse it.
     *
     * @param pCmdPort cluster master node cmd port.
     * @throws java.net.SocketException
     */
    public ClusterClient(int pCmdPort) throws SocketException {
        this(pCmdPort, null, null);
    }

    /**
     * it takes expensive cost to create such a client, so reuse it.
     *
     * @param pCmdPort         cluster master node cmd port.
     * @param relays           cmd relays, can be null
     * @param masterCandidates if client can't receive master's or relay's cmd, but you know some nodes may be masters
     */
    public ClusterClient(int pCmdPort, @Nullable List<String> relays, @Nullable List<String> masterCandidates) throws SocketException {
        this.cmdPort = pCmdPort;
        if (masterCandidates != null)
            masterCandidates.stream().filter(Strings::isNotBlank).forEach(mc -> this.masterCandidates.add(mc.trim()));
        this.cmdSock = new DatagramSocket();
        this.cmdSock.setSoTimeout(ClusterNode.NETWORK_TIMEOUT);
        this.cmdSock.setReceiveBufferSize(CMD_SOCK_BUF);
        this.cmdSock.setSendBufferSize(CMD_SOCK_BUF);
        this.cmdSock.setBroadcast(true);

        Nets.iterateNetworkIF(nif -> {
            for (InterfaceAddress addr : nif.getInterfaceAddresses()) {
                if (addr.getBroadcast() != null && addr.getBroadcast().getAddress().length == 4
                        && bcAdds.add(new NetFace(addr)))
                    log.info("add-addr-with-broadcast: " + addr);
            }
        });
        if (Colls.isNotEmpty(relays)) {
            relays.forEach(r -> {
                try {
                    if (Strings.isNotBlank(r))
                        bcAdds.add(new NetFace(r.trim()));
                } catch (Exception e) {
                    log.error("put-relay-error:{}", r, e);
                }
            });
        }

        this.prepareClusterService();
    }

    /**
     * run task in cluster.
     *
     * @param ns             execute within this namespace. it's used for resource access control.
     * @param cUser          task define.
     * @param task           task data.
     * @param timeout        task execution timeout(milli-second).
     *                       <code>0</code> represent for never timeout, even waiting for cluster getting ready.
     *                       <code>negative</code> represent for never timeout for task execution,
     *                       but throwing {@link ClusterExp.Unready} immediately if cluster is not ready.
     * @param subTaskTimeout suggested subTask execution timeout,
     *                       obeying it or not depends on the implementation of cUser.
     * @return execution result.
     * @throws mysh.cluster.ClusterExp        exceptions from cluster, generally about cluster
     *                                        status and task status.
     * @throws java.lang.InterruptedException current thread interrupted.
     * @throws java.lang.Throwable            other exceptions.
     */
    public <T, ST, SR, R> R runTask(final String ns, IClusterUser<T, ST, SR, R> cUser, T task,
                                    final int timeout, final int subTaskTimeout) throws Throwable {

        if (this.service == null && timeout < 0) {
            this.prepareClusterService();
            throw new ClusterExp.Unready();
        }

        long startTime = System.currentTimeMillis();
        int leftTime;

        IFaceHolder<IClusterService> cs = this.service;
        while (this.running) {
            if (timeout == 0)
                leftTime = 0;
            else {
                leftTime = timeout - (int) (System.currentTimeMillis() - startTime);
                if (leftTime <= 0)
                    throw new ClusterExp.Unready();
            }

            if (cs == null)
                cs = this.waitForClusterPreparing(startTime, leftTime);
            if (!this.running)
                break;

            try {
                return cs.getClient().runTask(ns, cUser, task, leftTime, subTaskTimeout);
            } catch (Throwable e) {
                log.debug("client-run-cluster-task-error.", e);
                if (isClusterUnready(e)) {
                    if (this.service != null)
                        try {
                            this.service.close();
                        } catch (Throwable ex) {
                        }
                    cs = this.service = null;
                    this.prepareClusterService();
                    if (timeout < 0) throw new ClusterExp.Unready(e);
                } else {
                    Throwable et;
                    if (e instanceof UndeclaredThrowableException && (et = e.getCause()) != null)
                        e = et;
                    if (e instanceof InvocationTargetException && (et = e.getCause()) != null)
                        e = et;
                    throw e;
                }
            }
        }
        throw new ClusterExp.ClientClosed();
    }

    /**
     * close the client.
     */
    public void close() {
        this.running = false;
        this.cmdSock.close();

        try {
            this.chkMasterCandidatesExec.shutdownNow();
            this.chkMasterCandidatesExec.awaitTermination(ClusterNode.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Throwable t) {}

        if (this.tPrepareClusterService != null) {
            this.tPrepareClusterService.interrupt();
            try {
                this.tPrepareClusterService.join();
            } catch (Throwable e) {
            }
        }

        if (this.service != null)
            try {
                this.service.close();
            } catch (Throwable e) {
            }
    }

    /**
     * preparing cluster service flag.
     * use <b>AtomicBoolean</b> but not <b>volatile boolean</b> here to make sure only one thread started.
     */
    private final AtomicBoolean isPreparingClusterService = new AtomicBoolean(false);

    private void prepareClusterService() {
        if (isPreparingClusterService.compareAndSet(false, true)) {
            tPrepareClusterService = new Thread(this.rPrepareClusterService,
                    "clusterClient:prepare-cluster-service-" + System.currentTimeMillis());
            tPrepareClusterService.setDaemon(true);
            tPrepareClusterService.start();

            if (masterCandidates.size() > 0)
                chkMasterCandidates();
        }
    }

    private final ExecutorService chkMasterCandidatesExec = new ThreadPoolExecutor(
            0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(), r -> {
        Thread t = new Thread(r, "chkMasterCandidates");
        t.setDaemon(true);
        return t;
    });

    private void chkMasterCandidates() {
        masterCandidates.forEach(mc -> {
            chkMasterCandidatesExec.submit(() -> {
                if (service != null)
                    return;
                Thread thread = Thread.currentThread();
                thread.setName("chkMasterCandidates-" + mc);
                try {
                    service = RpcUtil.getClient(IClusterService.class, mc, cmdPort + 1, ClusterNode.NETWORK_TIMEOUT, null);
                } catch (Throwable e) {
                    log.info("chk-master-candidate-fail:{},{}", mc, e.toString());
                } finally {
                    thread.setName("chkMasterCandidates");
                }
            });
        });
    }

    private volatile Thread tPrepareClusterService;
    private final Runnable rPrepareClusterService = new Runnable() {
        @Override
        public void run() {
            try {
                if (service != null) return;

                bcForMaster();

                // in case of multi-responses, so end the loop until sock.receive timeout exception throw.
                while (ClusterClient.this.running) {
                    Cmd cmd = SockUtil.receiveCmd(cmdSock, CMD_SOCK_BUF);
                    log.debug("client-rec-cmd -◀- " + cmd);
                    if (service == null && cmd.action == Cmd.Action.I_AM_THE_MASTER) {
                        try {
                            service = RpcUtil.getClient(IClusterService.class, cmd.ipAddr, cmd.masterPort, 0, null);
                        } catch (Throwable e) {
                            log.error("connect-to-master-service-error.", e);
                        }
                    }
                }
            } catch (Throwable e) {
                if (!(e instanceof SocketTimeoutException))
                    log.error("receive master response error.", e);
            } finally {
                isPreparingClusterService.set(false);
            }
        }

        /**
         * broadcast {@link Cmd.Action#WHO_IS_THE_MASTER_BY_CLIENT} for look up master.
         */
        private void bcForMaster() {
            Cmd cmd = new Cmd(Cmd.Action.WHO_IS_THE_MASTER_BY_CLIENT, "", 0,
                    cmdSock.getLocalPort(), 0);
            Cmd dc = null;
            for (NetFace addr : bcAdds) {
                try {
                    if (addr.isRelay() && dc == null) {
                        dc = cmd.clone();
                        dc.prepareDeliver();
                    }
                    SockUtil.sendCmd(cmdSock,
                            addr.isRelay() ? dc : cmd,
                            addr.getBroadcast(), cmdPort);
                    log.debug("bc-cmd ▶▶▶ " + cmd);
                } catch (IOException e) {
                    log.error("broadcast-for-master-error, on interface: " + addr, e);
                }
            }
        }
    };

    /**
     * @param startTime task submit time.
     * @param timeout   waiting for cluster ready timeout(milli-second).
     *                  <code>0</code> represent for never timeout.
     *                  <code>negative</code> throws {@link ClusterExp.Unready} immediately .
     */
    private IFaceHolder<IClusterService> waitForClusterPreparing(
            final long startTime, final int timeout) throws ClusterExp.Unready, InterruptedException {
        if (timeout < 0)
            throw new ClusterExp.Unready();

        IFaceHolder<IClusterService> cs = null;
        while (this.running && (cs = this.service) == null) {
            this.prepareClusterService();
            Thread.sleep(10);
            if (timeout > 0 && timeout <= System.currentTimeMillis() - startTime)
                throw new ClusterExp.Unready();
        }

        return cs;
    }

    private static boolean isClusterUnready(Throwable e) {
        return ClusterNode.getNodeUnavailableExp(e) != null
                ||
                Exps.isCausedBy(e, ClusterExp.NotMaster.class, ClusterExp.NoWorkers.class,
                        InterruptedException.class) != null
                ;
    }

    /**
     * get all workers' current states.
     */
    public <WS extends WorkerState> Map<String, WS> mgrGetWorkerStates() throws Throwable {
        return runTask(null, new MgrGetWorkerStates<>(), null, ClusterNode.NETWORK_TIMEOUT, 0);
    }

    private final MgrCancelTask cancelTaskUser = new MgrCancelTask();

    /**
     * request cancel task by taskId.
     */
    public void mgrCancelTask(int taskId) throws Throwable {
        runTask(null, cancelTaskUser, taskId, ClusterNode.NETWORK_TIMEOUT, 0);
    }

    public static class UpdateFile {
        private static final int DEFAULT_TIMEOUT = 30_000;

        private UpdateType updateType;
        private String fileName;
        private File file;
        private int timeout = DEFAULT_TIMEOUT;

        public UpdateFile(UpdateType updateType, String fileName, File file) {
            this.updateType = updateType;
            this.fileName = fileName;
            this.file = updateType == UpdateType.DELETE ? null : file;
        }

        /**
         * set file update timeout in millisecond. default value is {@link #DEFAULT_TIMEOUT}.
         */
        public UpdateFile setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        @Override
        public String toString() {
            return "UpdateFile{" +
                    "updateType=" + updateType +
                    ", fileName='" + fileName + '\'' +
                    ", file=" + file +
                    ", timeout=" + timeout +
                    '}';
        }
    }

    /**
     * update cluster files.
     * <br/>
     * WARNING: <br/>
     * there's no transaction and undo here.<br/>
     * when update CORE files: the master node will restart if all files are updated successfully;
     * if one file fails to update, the process will continue on next file, but there will be no
     * restart then.
     *
     * @return files that failed to update
     */
    public List<UpdateFile> mgrUpdateFile(
            FileType fileType, String ns, List<UpdateFile> ufs) throws Throwable {
        List<UpdateFile> failureList = new ArrayList<>();
        if (fileType == null || ufs.size() == 0)
            return failureList;

        boolean hasFailure = false;
        for (UpdateFile uf : ufs) {
            try {
                runTask(null,
                        MgrFileUpdate.update(fileType, ns, uf.updateType, uf.fileName,
                                (uf.file != null ? Files.readAllBytes(uf.file.toPath()) : null)),
                        null, uf.timeout, 0);
            } catch (Throwable e) {
                if (isClusterUnready(e))
                    throw e;

                log.error("fail-to-update-file: " + uf, e);
                hasFailure = true;
                failureList.add(uf);
            }
        }

        if (fileType == FileType.CORE && !hasFailure)
            mgrShutdownRestart(SRType.Restart, SRTarget.MasterOnly, null);
        return failureList;
    }

    /**
     * clear files inside main/su/ns and main/user/ns.
     */
    public void mgrClearFiles(String ns) throws Throwable {
        runTask(null,
                MgrFileUpdate.remove(ns),
                null, 0, 0);
    }

    public enum SRType {
        /**
         * shutdown node and exit VM.
         */
        Shutdown,
        /**
         * shutdown node and restart its VM, which applies new core libs.
         */
        Restart
    }

    public static enum SRTarget {EntireCluster, MasterOnly, MasterAndSpecified, Specified}

    /**
     * shutdown specified nodes.
     *
     * @param srType shutdown/restart type
     * @param target target type
     * @param nodes  specified nodes. will be ignored if target is
     *               {@link SRTarget#EntireCluster} or {@link SRTarget#MasterOnly}.
     */
    public void mgrShutdownRestart(SRType srType, SRTarget target, List<String> nodes) throws Throwable {
        runTask(null, new MgrShutdownRestart(srType, target, nodes), null, 60_000, 0);
    }

    /**
     * update config of current running nodes. <br/>
     * negative/null value will be ignored.
     * new config works only after node restart, but this invoke doesn't restart any node.
     */
    public void mgrUpdateConf(ClusterConf newConf) throws Throwable {
        runTask(null, new MgrUpdateConf(newConf), null, 60_000, 0);
    }
}
