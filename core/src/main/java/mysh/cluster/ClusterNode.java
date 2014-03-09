package mysh.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.rmi.UnmarshalException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
 * so if you need RMI and have to reset the response-timeout, run the cluster in a standalone VM.
 *
 * @author Mysh
 * @since 14-1-22 上午10:19
 */
public class ClusterNode implements Worker.Listener, Master.Listener {
	/**
	 * ClusterNode state.
	 */
	private static enum State {
		INIT, MASTER, WORKER
	}

	private static final Logger log = LoggerFactory.getLogger(ClusterNode.class);
	/**
	 * network timeout(milli-second).
	 */
	public static final int NETWORK_TIMEOUT = 5000;
	public static final int NODES_SCALE = 1024;
	/**
	 * create rmi client with timeout {@link #NETWORK_TIMEOUT}.
	 */
	static final RMIClientSocketFactory clientSockFact = (host, port) -> {
		Socket sock = new Socket();
		sock.connect(new InetSocketAddress(host, port), ClusterNode.NETWORK_TIMEOUT);
		return sock;
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
	 * UDP command sock.
	 */
	private DatagramSocket cmdSock;

	/**
	 * network interface addresses with broadcast address.
	 */
	private final Set<InterfaceAddress> bcAdds = new HashSet<>();

	private final BlockingQueue<Cmd> cmds = new LinkedBlockingQueue<>();
	private volatile State state;
	private Master master;

	private Worker worker;

	private volatile Cmd masterCandidate;

	/**
	 * @param cmdPort cmd port. UDP(cmdPort) will be used in broadcast communication,
	 *                while TCP(cmdPort) in RMI services dispatching,
	 *                TCP(cmdPort+1) in RMI Master-Node service and TCP(cmdPort+2) in RMI Worker-Node service.
	 * @throws Exception fail to bind UDP port, or no available network interface,
	 *                   or fail to bind rmi service.
	 */
	public ClusterNode(int cmdPort) throws Exception {

		cmdSock = new DatagramSocket(cmdPort);
		cmdSock.setReceiveBufferSize(15 * 1024 * 1024);
		cmdSock.setSendBufferSize(1024 * 1024);

		// prepare all network interface addresses with broadcast address.
		SockUtil.iterateNetworkIF(nif -> {
			nif.getInterfaceAddresses().stream()
							.filter(addr -> addr.getBroadcast() != null && addr.getAddress().getAddress().length == 4)
							.forEach(addr -> {
								if (bcAdds.add(addr)) {
									if (id == null) id = "cn_" + addr.getAddress().toString() + "_" + System.currentTimeMillis();
									log.info("add addr with broadcast: " + addr);
								}
							});
		});

		if (bcAdds.size() < 1) throw new RuntimeException("no available network interface that support broadcast.");

//		http://docs.oracle.com/javase/7/docs/technotes/guides/rmi/sunrmiproperties.html
		System.setProperty("sun.rmi.transport.tcp.responseTimeout", String.valueOf(NETWORK_TIMEOUT));

		Registry registry = LocateRegistry.createRegistry(cmdPort);

		master = new Master(id, cmdPort, this);
		IMasterService.bindService(registry, master, cmdPort + 1);

		worker = new Worker(id, cmdPort, this);
		IWorkerService.bindService(registry, worker, cmdPort + 2);

		startTime = System.currentTimeMillis();

		Thread getCmdT = new Thread(rGetCmd, "clusterNode:get-cmd");
		getCmdT.setDaemon(true);
		getCmdT.setPriority(Thread.MAX_PRIORITY);
		getCmdT.start();

		Thread procCmdT = new Thread(rProcCmd, "clusterNode:proc-cmd");
		procCmdT.setDaemon(true);
		procCmdT.setPriority(Thread.MAX_PRIORITY);
		procCmdT.start();

		changeState(State.INIT);
	}

	private final Runnable rGetCmd = () -> {
		byte[] buf = new byte[10_000];
		DatagramPacket p = new DatagramPacket(buf, 0, buf.length);
		while (true) {
			try {
				Cmd cmd = SockUtil.receiveCmd(cmdSock, p);
				cmd.receiveTime = System.currentTimeMillis();
				cmds.add(cmd);
			} catch (Exception e) {
				log.error("get cmd error.", e);
			}
		}
	};

	private final Runnable rProcCmd = () -> {
		while (true) {
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

						if (state == State.MASTER)
							broadcastCmd(Cmd.Action.I_AM_THE_MASTER);
						break;
					case WHO_IS_THE_MASTER_BY_CLIENT:
						if (state == State.MASTER) {
							broadcastCmd(Cmd.Action.I_AM_THE_MASTER);
							replyClient(cmd);
						}
						break;
					case I_AM_THE_MASTER:
						prepareMasterCandidate(cmd); // only used by Action.CHECK_MASTER check

						if (!cmd.id.equals(id)) { // cmd is from other node
							if (startTime < cmd.startTime) { // current node should be a master
								changeState(State.MASTER);
								broadcastCmd(Cmd.Action.I_AM_THE_MASTER);
							} else if (startTime > cmd.startTime) { // current node should not be a master
								changeState(State.WORKER);
							}
						}
						break;
					case I_AM_A_WORKER:
						prepareMasterCandidate(cmd); // only used by Action.CHECK_MASTER check
						break;
					case CHECK_MASTER:
						if (masterCandidate == null || masterCandidate.id.equals(id)) {
							changeState(State.MASTER);
							broadcastCmd(Cmd.Action.I_AM_THE_MASTER);
						} else
							changeState(State.WORKER);
						break;
					case REINIT:
						changeState(State.INIT);
						break;
					default:
						throw new RuntimeException("unknown action:" + cmd.action);
				}
			} catch (Exception e) {
				log.error("proc cmd failed.", e);
			}
		}
	};

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

	/**
	 * change state to newState.
	 */
	private void changeState(State newState) {
		if (state == newState) return;

		switch (newState) {
			case INIT:
				masterCandidate = null;
				broadcastCmd(Cmd.Action.WHO_IS_THE_MASTER_BY_WORKER);
				new Timer("clusterNode:check-master-timer", true).schedule(new TimerTask() {
					@Override
					public void run() {
						cmds.add(new Cmd(Cmd.Action.CHECK_MASTER, null, 0, null, (short) 0, 0, 0));
					}
				}, NETWORK_TIMEOUT);
				break;
			case MASTER:
				master.setMaster(true);
				break;
			case WORKER:
				master.setMaster(false);
				break;
			default:
				throw new RuntimeException("unknown state:" + newState);
		}

		state = newState;
		log.info("state changed to " + state);
	}


	/**
	 * reply client that "I am the master".
	 */
	private void replyClient(Cmd clientCmd) {
		for (InterfaceAddress addr : bcAdds) {
			if (SockUtil.isInTheSameBroadcastDomain(addr, clientCmd.ipAddr, clientCmd.ipMask)) {
				Cmd cmdForClient = new Cmd(Cmd.Action.I_AM_THE_MASTER, this.id, this.startTime,
								addr.getAddress().getHostAddress(), addr.getNetworkPrefixLength(),
								master != null ? master.getServicePort() : 0,
								worker != null ? worker.getServicePort() : 0);
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
							master != null ? master.getServicePort() : 0,
							worker != null ? worker.getServicePort() : 0);
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
		changeState(State.INIT);
	}

	@Override
	public void broadcastIAmTheMaster() {
		if (this.state == State.MASTER)
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
