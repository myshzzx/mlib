package mysh.cluster;

import mysh.cluster.rpc.IFaceHolder;
import mysh.cluster.rpc.thrift.RpcUtil;
import mysh.util.ExpUtil;
import mysh.util.OSUtil;
import org.apache.thrift.server.TServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.rmi.UnmarshalException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
public class ClusterNode {

	/**
	 * ClusterNode state.
	 */
	static enum ClusterState {
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
	private volatile ClusterState nodeState;

	private final FilesMgr filesMgr = new FilesMgr();
	private Master master;
	private Worker worker;

	private TServer tMasterServer;
	private TServer tWorkerServer;
	private BlockingQueue<Closeable> thriftConns = new LinkedBlockingQueue<>();

	/**
	 * @throws Throwable fail to bind UDP port, or no available network interface.
	 */
	public ClusterNode(ClusterConf conf) throws Throwable {
		try {
			OSUtil.changePriority(OSUtil.getPid(), OSUtil.OsProcPriority.BelowNormal);
		} catch (Throwable e) {
			log.info("change process failed.", e);
		}

		this.id = conf.id;
		this.cmdPort = conf.cmdPort;

		cmdSock = new DatagramSocket(cmdPort);
		cmdSock.setReceiveBufferSize(15 * 1024 * 1024);
		cmdSock.setSendBufferSize(1024 * 1024);

		// prepare all network interface addresses with broadcast address.
		renewNetworkIf();
		if ((this.id == null || this.id.length() == 0) && bcAdds.size() > 0) {
			this.id = "cn_" + bcAdds.iterator().next().getAddress().toString() + "_" + System.currentTimeMillis();
			conf.id = this.id;
		}

		if (bcAdds.size() < 1)
			throw new RuntimeException("no available network interface that support broadcast.");

		master = new Master(id, this, conf.heartBeatTime, filesMgr);
		tMasterServer = RpcUtil.exportTServer(
						IMaster.class, master, this.cmdPort + 1, null, conf.serverPoolSize, filesMgr.loaders);
		RpcUtil.startTServer(tMasterServer);

		worker = new Worker(id, this, conf.initState, conf.heartBeatTime, filesMgr);
		tWorkerServer = RpcUtil.exportTServer(
						IWorker.class, worker, this.cmdPort + 2, null, conf.serverPoolSize, filesMgr.loaders);
		RpcUtil.startTServer(tWorkerServer);

		startTime = conf.startTime;

		tGetCmd.setDaemon(true);
		tGetCmd.setPriority(Thread.MAX_PRIORITY);
		tGetCmd.start();

		tProcCmd.setDaemon(true);
		tProcCmd.setPriority(Thread.MAX_PRIORITY);
		tProcCmd.start();

		conf.save();
		changeState(ClusterState.INIT);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				shutdownNode();
			}
		});
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
				} catch (Throwable e) {
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

							if (nodeState == ClusterState.MASTER)
								broadcastCmd(Cmd.Action.I_AM_THE_MASTER);
							break;
						case WHO_IS_THE_MASTER_BY_CLIENT:
							if (nodeState == ClusterState.MASTER) {
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
				} catch (Throwable e) {
					log.error("proc cmd failed.", e);
				}
			}
		}
	};

	private final AtomicBoolean shutdownFlag = new AtomicBoolean(false);


	ClusterState getNodeState() {
		return nodeState;
	}

	/**
	 * shutdown cluster node and release resource.
	 */
	public void shutdownNode() {
		if (!shutdownFlag.compareAndSet(false, true))
			return;

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
		} catch (Throwable e) {
			log.error("node.tGetCmd close error.", e);
		}
		try {
			log.debug("closing node.tProcCmd.");
			tProcCmd.interrupt();
			tProcCmd.join();
		} catch (Throwable e) {
			log.error("node.tProcCmd close error.", e);
		}
		try {
			log.debug("closing node.rChkMaster.");
			rChkMaster.shutdownNow();
		} catch (Throwable e) {
			log.error("node.rChkMaster close error.", e);
		}

		try {
			log.debug("stopping master.");
			tMasterServer.stop();
		} catch (Throwable e) {
			log.error("stopping master service error.", e);
		}
		try {
			log.debug("stopping worker.");
			tWorkerServer.stop();
		} catch (Throwable e) {
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
	 * whether the Throwable throw by remote invoking means node unavailable.<br/>
	 * in conditions below: <br/>
	 * unknown host, ip unreachable, no service on the port,
	 * response timeout in remote invoking
	 * network cut when connected, fire wall block when connected,
	 *
	 * @param e remote invoke exception.
	 */
	static boolean isNodeUnavailable(Throwable e) {
		return ExpUtil.isCausedBy(e, UnknownHostException.class, ConnectException.class,
						SocketTimeoutException.class, SocketException.class) != null;
	}

	@Deprecated
	private static boolean isNodeUnavailableRMI(Throwable e) {
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
		if (nodeState == newClusterState) return;

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

		nodeState = newClusterState;
		log.info("state changed to " + nodeState);
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
				} catch (Throwable e) {
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
			} catch (Throwable e) {
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

	/**
	 * get worker service. (by master)
	 */
	IWorker getWorkerService(String host, int port) throws Throwable {
		IFaceHolder<IWorker> ch =
						RpcUtil.getClient(IWorker.class, host, port, NETWORK_TIMEOUT, filesMgr.loaders);
		this.thriftConns.add(ch);
		return ch.getClient();
	}

	/**
	 * tell current node to check whether it's master, if it is, broadcast to other nodes. (by master)
	 */
	void broadcastIAmTheMaster() {
		if (this.nodeState == ClusterState.MASTER)
			this.broadcastCmd(Cmd.Action.I_AM_THE_MASTER);
	}

	/**
	 * tell current node that one worker node is unavailable. (by master)
	 */
	void workerUnavailable(String workerId) {
		if (worker != null) worker.removeMasterFromCache(workerId);
	}

	/**
	 * get master service. (by worker)
	 */
	IMaster getMasterService(String host, int port) throws Throwable {
		IFaceHolder<IMaster> ch =
						RpcUtil.getClient(IMaster.class, host, port, NETWORK_TIMEOUT, filesMgr.loaders);
		this.thriftConns.add(ch);
		return ch.getClient();
	}

	/**
	 * tell node that master lost(node heartBeat time out or service unavailable). (by worker)
	 * <br/>
	 * current node state will be reset,
	 * and a new {@link mysh.cluster.Cmd.Action#WHO_IS_THE_MASTER_BY_WORKER} check will be started.
	 */
	void masterLost(String masterId) {
		if (master != null && masterId != null)
			master.removeWorker(masterId);

		// master lost may caused by network changes
		try {
			renewNetworkIf();
		} catch (SocketException e) {
			log.error("renew network interfaces fails when master lost.", e);
		}

		changeState(ClusterState.INIT);
	}

	/**
	 * shutdown current node (and restart current VM).
	 *
	 * @param restart restart or not.
	 */
	void shutdownVM(boolean restart) {
		new Thread() {
			@Override
			public void run() {
				try {
					// wait for replying master (if this is worker node)
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				log.info("begin to shutdown cluster node.");
				shutdownNode();

				if (restart) {
					String[] cmd = filesMgr.getRestartCmd();
					log.debug("restartVM with cmd: " + Arrays.toString(cmd));
					try {
						ProcessBuilder pb = new ProcessBuilder(cmd);
						pb.inheritIO();
						pb.start();
					} catch (IOException e) {
						log.error("restartVM cluster node error, try restartVM process", e);
						try {
							OSUtil.restart(true);
						} catch (IOException ex) {
							log.error("restartVM process error, the cluster node need manual start.", ex);
						}
					}
				}

				System.exit(0);
			}
		}.start();
	}
}
