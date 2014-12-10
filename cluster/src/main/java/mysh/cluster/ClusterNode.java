package mysh.cluster;

import mysh.cluster.rpc.IFaceHolder;
import mysh.cluster.rpc.thrift.ThriftUtil;
import mysh.cluster.update.FilesMgr;
import mysh.util.ExpUtil;
import org.apache.thrift.server.TServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.rmi.UnmarshalException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * self-organized and decentralized Master-Worker cluster network keeper.<br/>
 * designed for small scale(hundreds nodes) and heavy communication cluster.
 * <p>
 * WARNING: <br/>
 * 1. need ipv4<br/>
 * 2. Can only be used in simple network(inside the same broadcast domain)
 * (do not exist such a node that is in two different network,
 * that one can't access the other). Because two disconnected network will cause
 * Master node confusing, while this cluster use UDP broadcast to discover nodes.<br/>
 *
 * @author Mysh
 * @since 14-1-22 上午10:19
 */
public class ClusterNode implements Worker.Listener, Master.Listener {
	/**
	 * ClusterNode state.
	 */
	private static enum ClusterState {
		INIT, MASTER, WORKER
	}

	private static final Logger log = LoggerFactory.getLogger(ClusterNode.class);

	private final int cmdPort;

	/**
	 * ClusterNode ID.
	 */
	private String id;
	/**
	 * ClusterNode created time in milli-second.
	 */
	private volatile long startTime;

	/**
	 * network timeout(milli-second).
	 */
	public static final int NETWORK_TIMEOUT = 5000;
	public static final int NODES_SCALE = 1024;
	/**
	 * UDP command sock.
	 */
	private DatagramSocket cmdSock;
	/**
	 * network interface addresses with broadcast address.
	 */
	private volatile Set<InterfaceAddress> bcAdds = new HashSet<>();
	/**
	 * received cmds.
	 */
	private final BlockingQueue<Cmd> cmds = new LinkedBlockingQueue<>();
	private volatile Cmd masterCandidate;
	private volatile ClusterState clusterState;

	private final FilesMgr filesMgr = new FilesMgr();
	private Master master;
	private Worker worker;

	private TServer tMasterServer;
	private TServer tWorkerServer;
	private BlockingQueue<Closeable> thriftConns = new LinkedBlockingQueue<>();

	/**
	 * @param cmdPort        cmd port. UDP(cmdPort) will be used in broadcast communication,
	 *                       while TCP(cmdPort) in services dispatching,
	 *                       TCP(cmdPort+1) in Master-Node service and TCP(cmdPort+2) in Worker-Node service.
	 * @param initState      initial state of the worker, which can be updated and sent to master node automatically.
	 *                       can be <code>null</code>.
	 * @param serverPoolSize rpc server pool size.
	 *                       see {@link mysh.thrift.ThriftServerFactory#setServerPoolSize}
	 * @throws Exception fail to bind UDP port, or no available network interface.
	 */
	public ClusterNode(int cmdPort, WorkerState initState, int serverPoolSize) throws Exception {

		cmdSock = new DatagramSocket(cmdPort);
		cmdSock.setReceiveBufferSize(15 * 1024 * 1024);
		cmdSock.setSendBufferSize(1024 * 1024);

		// prepare all network interface addresses with broadcast address.
		renewNetworkIf();
		if (id == null && bcAdds.size() > 0)
			id = "cn_" + bcAdds.iterator().next().getAddress().toString() + "_" + System.currentTimeMillis();

		if (bcAdds.size() < 1)
			throw new RuntimeException("no available network interface that support broadcast.");

		this.cmdPort = cmdPort;

		master = new Master(id, this, filesMgr);
		tMasterServer = ThriftUtil.exportTServer(
						IMaster.class, master, this.cmdPort + 1, null, serverPoolSize, filesMgr.clFetcher);
		ThriftUtil.startTServer(tMasterServer);

		worker = new Worker(id, this, initState, filesMgr);
		tWorkerServer = ThriftUtil.exportTServer(
						IWorker.class, worker, this.cmdPort + 2, null, serverPoolSize, filesMgr.clFetcher);
		ThriftUtil.startTServer(tWorkerServer);

		startTime = System.currentTimeMillis();

		tGetCmd.setDaemon(true);
		tGetCmd.setPriority(Thread.MAX_PRIORITY);
		tGetCmd.start();

		tProcCmd.setDaemon(true);
		tProcCmd.setPriority(Thread.MAX_PRIORITY);
		tProcCmd.start();

		changeState(ClusterState.INIT);
	}

	private final Thread tGetCmd = new Thread("clusterNode:get-cmd") {
		@Override
		public void run() {
			byte[] buf = new byte[10_000];
			DatagramPacket p = new DatagramPacket(buf, 0, buf.length);

			while (!this.isInterrupted()) {
				try {
					Cmd cmd = SockUtil.receiveCmd(cmdSock, p);
					cmd.receiveTime = System.currentTimeMillis();
					cmds.add(cmd);
				} catch (InterruptedException e) {
					return;
				} catch (Exception e) {
					log.error("get cmd error.", e);
				}
			}
		}
	};

	private final Thread tProcCmd = new Thread("clusterNode:proc-cmd") {
		@Override
		public void run() {
			while (!this.isInterrupted()) {
				try {
					Cmd cmd = cmds.take();
					log.debug("rec cmd <<< " + cmd);
					if (cmd.action == Cmd.Action.I_AM_THE_MASTER
									|| cmd.action == Cmd.Action.I_AM_A_WORKER
									|| cmd.action == Cmd.Action.WHO_IS_THE_MASTER_BY_WORKER)
						master.newNode(cmd);
					if (cmd.action == Cmd.Action.I_AM_THE_MASTER)
						worker.newMaster(cmd);

					switch (cmd.action) {
						case WHO_IS_THE_MASTER_BY_WORKER:
							prepareMasterCandidate(cmd); // only used by Action.CHECK_MASTER check

							if (clusterState == ClusterState.MASTER)
								broadcastCmd(Cmd.Action.I_AM_THE_MASTER);
							break;
						case WHO_IS_THE_MASTER_BY_CLIENT:
							if (clusterState == ClusterState.MASTER) {
								broadcastCmd(Cmd.Action.I_AM_THE_MASTER);
								replyClient(cmd);
							}
							break;
						case I_AM_THE_MASTER:
							prepareMasterCandidate(cmd); // only used by Action.CHECK_MASTER check

							if (!cmd.id.equals(id)) { // cmd is from other node
								if (startTime < cmd.startTime) { // current node should be a master
									changeState(ClusterState.MASTER);
									broadcastCmd(Cmd.Action.I_AM_THE_MASTER);
								} else if (startTime > cmd.startTime) { // current node should not be a master
									changeState(ClusterState.WORKER);
								}
							}
							break;
						case I_AM_A_WORKER:
							prepareMasterCandidate(cmd); // only used by Action.CHECK_MASTER check
							break;
						case CHECK_MASTER:
							if (masterCandidate == null || masterCandidate.id.equals(id)) {
								changeState(ClusterState.MASTER);
								broadcastCmd(Cmd.Action.I_AM_THE_MASTER);
							} else
								changeState(ClusterState.WORKER);
							break;
						case REINIT:
							changeState(ClusterState.INIT);
							break;
						default:
							throw new RuntimeException("unknown action:" + cmd.action);
					}
				} catch (InterruptedException e) {
					return;
				} catch (Exception e) {
					log.error("proc cmd failed.", e);
				}
			}
		}
	};

	@Override
	public IWorker getWorkerService(String host, int port) throws Exception {
		IFaceHolder<IWorker> ch = ThriftUtil.getClient(IWorker.class, host, port, NETWORK_TIMEOUT,
						filesMgr.clFetcher);
		this.thriftConns.add(ch);
		return ch.getClient();
	}

	@Override
	public IMaster getMasterService(String host, int port) throws Exception {
		IFaceHolder<IMaster> ch = ThriftUtil.getClient(IMaster.class, host, port, NETWORK_TIMEOUT,
						filesMgr.clFetcher);
		this.thriftConns.add(ch);
		return ch.getClient();
	}

	/**
	 * shutdown cluster node and release resource.
	 */
	public void shutdownNode() {
		log.info("closing cluster node.");

		log.debug("closing cmdSock.");
		cmdSock.close();

		log.debug("closing files manager.");
		try {
			filesMgr.close();
		} catch (IOException e) {
			log.error("close files manager error.", e);
		}

		try {
			log.debug("closing node.tGetCmd.");
			tGetCmd.interrupt();
			tGetCmd.join();
		} catch (Exception e) {
			log.error("node.tGetCmd close error.", e);
		}
		try {
			log.debug("closing node.tProcCmd.");
			tProcCmd.interrupt();
			tProcCmd.join();
		} catch (Exception e) {
			log.error("node.tProcCmd close error.", e);
		}
		try {
			log.debug("closing node.rChkMaster.");
			rChkMaster.shutdownNow();
		} catch (Exception e) {
			log.error("node.rChkMaster close error.", e);
		}

		try {
			log.debug("stopping master.");
			tMasterServer.stop();
		} catch (Exception e) {
			log.error("stopping master service error.", e);
		}
		try {
			log.debug("stopping worker.");
			tWorkerServer.stop();
		} catch (Exception e) {
			log.error("stopping worker service error.", e);
		}

		log.debug("closing thrift connections");
		do {
			Closeable tc = this.thriftConns.poll();
			if (tc == null) break;
			try {
				tc.close();
			} catch (IOException e) {
			}
		} while (true);

		master.closeMaster();
		worker.closeWorker();

		log.info("cluster node closed.");
	}

	/**
	 * whether the exception throw by remote invoking means node unavailable.<br/>
	 * in conditions below: <br/>
	 * unknown host, ip unreachable, no service on the port,
	 * response timeout in remote invoking
	 * network cut when connected, fire wall block when connected,
	 *
	 * @param e remote invoke exception.
	 */
	static boolean isNodeUnavailable(Exception e) {
		return ExpUtil.isCausedBy(e, UnknownHostException.class, ConnectException.class,
						SocketTimeoutException.class, SocketException.class) != null;
	}

	@Deprecated
	private static boolean isNodeUnavailableRMI(Exception e) {
		return e instanceof java.rmi.ConnectException // re-connect to remote ipAddr failed
						|| e instanceof java.rmi.ConnectIOException // can't connect to remote ipAddr
						|| e instanceof java.rmi.NoSuchObjectException
						|| e instanceof java.rmi.ServerError
						|| e instanceof java.rmi.UnknownHostException
						|| (
						e instanceof UnmarshalException // remote ipAddr response timeout
										&& (((UnmarshalException) e).detail instanceof SocketTimeoutException
										|| ((UnmarshalException) e).detail instanceof SocketException));
	}

	private void prepareMasterCandidate(Cmd cmd) {
		if (cmd.masterPort > 0 && cmd.startTime > 0) {
			if (this.masterCandidate == null) this.masterCandidate = cmd;
			else if (cmd.startTime < this.masterCandidate.startTime
							||
							(cmd.startTime == this.masterCandidate.startTime &&
											cmd.id.compareTo(this.masterCandidate.id) < -1)
							) {
				this.masterCandidate = cmd;
			}
		}
	}

	private ScheduledExecutorService rChkMaster = Executors.newScheduledThreadPool(1, r -> {
		Thread t = new Thread(r, "clusterNode:check-master");
		t.setDaemon(true);
		return t;
	});

	/**
	 * change state to newState.
	 */
	private void changeState(ClusterState newClusterState) {
		if (clusterState == newClusterState) return;

		switch (newClusterState) {
			case INIT:
				masterCandidate = null;
				broadcastCmd(Cmd.Action.WHO_IS_THE_MASTER_BY_WORKER);
				rChkMaster.schedule(() -> cmds.add(new Cmd(Cmd.Action.CHECK_MASTER, null, 0, null, (short) 0, 0, 0)),
								NETWORK_TIMEOUT, TimeUnit.MILLISECONDS);
				break;
			case MASTER:
				master.setMaster(true);
				break;
			case WORKER:
				master.setMaster(false);
				break;
			default:
				throw new RuntimeException("unknown state:" + newClusterState);
		}

		clusterState = newClusterState;
		log.info("state changed to " + clusterState);
	}


	/**
	 * reply client that "I am the master".
	 */
	private void replyClient(Cmd clientCmd) {
		for (InterfaceAddress addr : bcAdds) {
			if (SockUtil.isInTheSameBroadcastDomain(addr, clientCmd.ipAddr, clientCmd.ipMask)) {
				Cmd cmdForClient = new Cmd(Cmd.Action.I_AM_THE_MASTER, this.id, this.startTime,
								addr.getAddress().getHostAddress(), addr.getNetworkPrefixLength(),
								master != null ? this.cmdPort + 1 : 0,
								worker != null ? this.cmdPort + 2 : 0);
				try {
					SockUtil.sendCmd(this.cmdSock, cmdForClient, InetAddress.getByName(clientCmd.ipAddr), clientCmd.masterPort);
					log.debug("reply client >>> " + cmdForClient);
				} catch (Exception e) {
					log.error("reply client error.", e);
				}
				break;
			}
		}
	}

	private void broadcastCmd(Cmd.Action action) {
		for (InterfaceAddress ifa : bcAdds) {
			Cmd c = new Cmd(action, this.id, this.startTime,
							ifa.getAddress().getHostAddress(), ifa.getNetworkPrefixLength(),
							master != null ? this.cmdPort + 1 : 0,
							worker != null ? this.cmdPort + 2 : 0);
			try {
				SockUtil.sendCmd(cmdSock, c, ifa.getBroadcast(), cmdSock.getLocalPort());
				log.debug("bc cmd >>> " + c);
			} catch (Exception e) {
				log.error("broadcast cmd error, interface:" + ifa.toString(), e);
				try {
					renewNetworkIf();
				} catch (SocketException ex) {
					log.error("renew network error.", ex);
					return;
				}
				broadcastCmd(action);
				return;
			}
		}
	}

	private void renewNetworkIf() throws SocketException {
		Set<InterfaceAddress> tBcAdds = new HashSet<>();
		SockUtil.iterateNetworkIF(nif -> {
			nif.getInterfaceAddresses().stream()
							.filter(addr -> addr.getBroadcast() != null && addr.getAddress().getAddress().length == 4)
							.forEach(addr -> {
								if (tBcAdds.add(addr)) {
									log.info("add addr with broadcast: " + addr);
								}
							});
		});
		bcAdds = tBcAdds;
	}

	@Override
	public void masterTimeout() {
		changeState(ClusterState.INIT);
	}

	@Override
	public void broadcastIAmTheMaster() {
		if (this.clusterState == ClusterState.MASTER)
			this.broadcastCmd(Cmd.Action.I_AM_THE_MASTER);
	}

	@Override
	public void workerUnavailable(String workerId) {
		if (worker != null) worker.removeMaster(workerId);
	}

	@Override
	public void masterUnavailable(String masterId) {
		if (master != null) master.removeWorker(masterId);
	}
}
