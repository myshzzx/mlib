package mysh.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;

/**
 * self-organized and decentralized Master-Worker cluster network keeper.<br/>
 * <p>
 * WARNING: <br/>
 * 1. need ipv4<br/>
 * 2. Can only be used in simple network(inside the same broadcast domain)
 * (do not exist such a node that is in two different network,
 * that one can't access the other). Because two disconnected network will cause
 * Master node confusing, while this cluster use UDP broadcast to discover nodes.<br/>
 * 3. <code>
 * System.setProperty("sun.rmi.transport.tcp.responseTimeout", String.valueOf(NETWORK_TIMEOUT))
 * </code>
 * will be executed when instantiate {@link ClusterNode},
 * which means RMI timeout for remote-invoking will be set to {@link ClusterNode#NETWORK_TIMEOUT},
 * if the remote method doesn't return in timeout,
 * a <code>UnmarshalException[detail=java.net.SocketTimeoutException("Read timed out")]</code> will be thrown,
 * so if you need RMI and have to reset the response-timeout, run the cluster in a standalone VM.<br/>
 * 4. ClusterClient and ClusterNode should be used in different VM(or loaded by different classloader),
 * because ClusterNode will reset response-timeout.
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

	/**
	 * http://docs.oracle.com/javase/7/docs/technotes/guides/rmi/sunrmiproperties.html
	 * <br/>
	 * The value of this property represents the length of time (in milliseconds)
	 * that the client-side Java RMI runtime will use as a socket read timeout
	 * on an established JRMP connection when reading response data for a remote method invocation.
	 * Therefore, this property can be used to impose a timeout on waiting for
	 * the results of remote invocations; if this timeout expires,
	 * the associated invocation will fail with a java.rmi.RemoteException.
	 * Setting this property should be done with due consideration,
	 * however, because it effectively places an upper bound on the allowed duration of
	 * any successful outgoing remote invocation. The maximum value is Integer.MAX_VALUE,
	 * and a value of zero indicates an infinite timeout. The default value is zero (no timeout).
	 */
	private static final String RESPONSE_TIMEOUT_PROP = "sun.rmi.transport.tcp.responseTimeout";
	static final String MASTER_RMI_NAME = IMaster.class.getSimpleName();
	private static final String WORKER_RMI_NAME = IWorker.class.getSimpleName();
	private final int rmiPort;
	private final Registry registry;
	private final Queue<Closeable> rmiCreatedSocks = new ConcurrentLinkedQueue<>();
	/**
	 * create rmi client with timeout {@link #NETWORK_TIMEOUT}.
	 */
	private final RMIClientSocketFactory clientSockFact = (host, port) -> {
		Socket sock = new Socket();
		sock.connect(new InetSocketAddress(host, port), ClusterNode.NETWORK_TIMEOUT);
		this.rmiCreatedSocks.add(sock);
		return sock;
	};
	private final RMIServerSocketFactory serverSockFact = port -> {
		ServerSocket s = new ServerSocket(port);
		this.rmiCreatedSocks.add(s);
		return s;
	};

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
	private final Set<InterfaceAddress> bcAdds = new HashSet<>();
	/**
	 * received cmds.
	 */
	private final BlockingQueue<Cmd> cmds = new LinkedBlockingQueue<>();
	private volatile Cmd masterCandidate;
	private volatile ClusterState clusterState;

	private Master master;
	private Worker worker;

	/**
	 * @param cmdPort   cmd port. UDP(cmdPort) will be used in broadcast communication,
	 *                  while TCP(cmdPort) in RMI services dispatching,
	 *                  TCP(cmdPort+1) in RMI Master-Node service and TCP(cmdPort+2) in RMI Worker-Node service.
	 * @param initState initial state of the worker, which can be updated and sent to master node automatically.
	 *                  can be <code>null</code>.
	 * @throws Exception fail to bind UDP port, or no available network interface,
	 *                   or fail to bind rmi service.
	 */
	public ClusterNode(int cmdPort, WorkerState initState) throws Exception {

		cmdSock = new DatagramSocket(cmdPort);
		cmdSock.setReceiveBufferSize(15 * 1024 * 1024);
		cmdSock.setSendBufferSize(1024 * 1024);

		// prepare all network interface addresses with broadcast address.
		SockUtil.iterateNetworkIF(nif -> {
			nif.getInterfaceAddresses().stream()
							.filter(addr -> addr.getBroadcast() != null && addr.getAddress().getAddress().length == 4)
							.forEach(addr -> {
								if (bcAdds.add(addr)) {
									if (id == null)
										id = "cn_" + addr.getAddress().toString() + "_" + System.currentTimeMillis();
									log.info("add addr with broadcast: " + addr);
								}
							});
		});

		if (bcAdds.size() < 1)
			throw new RuntimeException("no available network interface that support broadcast.");

//		http://docs.oracle.com/javase/7/docs/technotes/guides/rmi/sunrmiproperties.html
		System.setProperty(RESPONSE_TIMEOUT_PROP, String.valueOf(NETWORK_TIMEOUT));

		this.rmiPort = cmdPort;
		registry = LocateRegistry.createRegistry(this.rmiPort, clientSockFact, serverSockFact);

		master = new Master(id, this);
		bind(registry, MASTER_RMI_NAME, master, this.rmiPort + 1);

		worker = new Worker(id, this, initState);
		bind(registry, WORKER_RMI_NAME, worker, this.rmiPort + 2);

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
					log.debug("receive cmd: " + cmd);
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

	/**
	 * shutdown cluster node and release resource.
	 */
	public void shutdownNode() {
		log.info("closing cluster node.");

		log.debug("closing cmdSock.");
		cmdSock.close();

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
			log.debug("unbinding master.");
			unbind(this.registry, MASTER_RMI_NAME, master);
		} catch (Exception e) {
			log.error("unbind master service error.", e);
		}
		try {
			log.debug("unbinding worker.");
			unbind(this.registry, WORKER_RMI_NAME, worker);
		} catch (Exception e) {
			log.error("unbind worker service error.", e);
		}
		log.debug("closing rmi socks, count:" + this.rmiCreatedSocks.size());
		Closeable rmiSock;
		while ((rmiSock = this.rmiCreatedSocks.poll()) != null) {
			try {
				rmiSock.close();
			} catch (IOException e) {
			}
		}

		master.closeMaster();
		worker.closeWorker();

		log.info("cluster node closed.");
	}

	// ============ RMI below ===============

	@SuppressWarnings("unchecked")
	static <T> T getRMIService(String host, int port, String serviceRmiName, RMIClientSocketFactory clientSockFact)
					throws RemoteException, NotBoundException {
		Registry registry = LocateRegistry.getRegistry(host, port, clientSockFact);
		return (T) registry.lookup(serviceRmiName);
	}

	public IMaster getMasterService(String host, int port) throws RemoteException, NotBoundException {
		return getRMIService(host, port, MASTER_RMI_NAME, this.clientSockFact);
	}

	public IWorker getWorkerService(String host, int port) throws RemoteException, NotBoundException {
		return getRMIService(host, port, WORKER_RMI_NAME, this.clientSockFact);
	}

	private static void bind(Registry registry, String name, Remote remote, int port)
					throws RemoteException, AlreadyBoundException {
		registry.bind(name, UnicastRemoteObject.exportObject(remote, port));
	}

	private static void unbind(Registry registry, String name, Remote remote)
					throws RemoteException, NotBoundException {
		registry.unbind(name);
		UnicastRemoteObject.unexportObject(remote, true);
	}

	// ============ RMI above ===============

	/**
	 * whether the exception throw by remote invoking means node unavailable.
	 *
	 * @param e remote invoke exception.
	 */
	static boolean isNodeUnavailable(Exception e) {
		return e instanceof java.rmi.ConnectException // re-connect to remote ipAddr failed
						|| e instanceof java.rmi.ConnectIOException // can't connect to remote ipAddr
						|| e instanceof java.rmi.NoSuchObjectException
						|| e instanceof java.rmi.ServerError
						|| e instanceof java.rmi.UnknownHostException
						|| (
						e instanceof java.rmi.UnmarshalException // remote ipAddr response timeout
										&& (((UnmarshalException) e).detail instanceof SocketTimeoutException
										|| ((UnmarshalException) e).detail instanceof SocketException))
						;
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
								master != null ? this.rmiPort : 0,
								worker != null ? this.rmiPort : 0);
				try {
					SockUtil.sendCmd(this.cmdSock, cmdForClient, InetAddress.getByName(clientCmd.ipAddr), clientCmd.masterPort);
					log.debug("reply client: " + cmdForClient);
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
							master != null ? this.rmiPort : 0,
							worker != null ? this.rmiPort : 0);
			try {
				SockUtil.sendCmd(cmdSock, c, ifa.getBroadcast(), cmdSock.getLocalPort());
				log.debug("broadcast cmd: " + c);
			} catch (Exception e) {
				log.error("broadcast cmd error, interface:" + ifa.toString(), e);
			}
		}
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
