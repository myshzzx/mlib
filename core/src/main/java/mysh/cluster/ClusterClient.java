package mysh.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InterfaceAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Mysh
 * @since 14-2-23 下午2:21
 */
public final class ClusterClient {
	private static final Logger log = LoggerFactory.getLogger(ClusterClient.class);
	private static final int CMD_SOCK_BUF = 1024 * 1024;

	private final int cmdPort;
	private final DatagramSocket cmdSock;
	/**
	 * network interface addresses with broadcast address.
	 */
	private final Set<InterfaceAddress> bcAdds = new HashSet<>();
	private volatile IClusterService service;


	/**
	 * it takes expensive cost to create such a client, so reuse it.
	 *
	 * @param pCmdPort cluster master node cmd port.
	 * @throws java.net.SocketException
	 */
	public ClusterClient(int pCmdPort) throws SocketException {

		this.cmdPort = pCmdPort;
		this.cmdSock = new DatagramSocket();
		this.cmdSock.setSoTimeout(ClusterNode.NETWORK_TIMEOUT);
		this.cmdSock.setReceiveBufferSize(CMD_SOCK_BUF);
		this.cmdSock.setSendBufferSize(CMD_SOCK_BUF);

		SockUtil.iterateNetworkIF(nif -> {
			for (InterfaceAddress addr : nif.getInterfaceAddresses()) {
				if (addr.getBroadcast() != null && bcAdds.add(addr))
					log.info("add addr with broadcast: " + addr);
			}
		});

		this.prepareClusterService();
	}

	/**
	 * run task in cluster.
	 *
	 * @param cUser          task define.
	 * @param task           task data.
	 * @param timeout        task execution timeout(milli-second).
	 *                       <code>0</code> represent for never timeout, even waiting for cluster getting ready.
	 *                       <code>negative</code> represent for never timeout for task execution,
	 *                       but throwing {@link ClusterExcp.Unready} immediately if cluster is not ready.
	 * @param subTaskTimeout suggested subTask execution timeout,
	 *                       obeying it or not depends on the implementation of cUser.
	 * @throws ClusterExcp.Unready IClusterService is not ready until timeout.
	 */
	public <T, ST, SR, R> R runTask(final IClusterUser<T, ST, SR, R> cUser, final T task,
	                                final int timeout, final int subTaskTimeout)
					throws RemoteException, ClusterExcp.Unready, ClusterExcp.TaskTimeout, InterruptedException {

		if (this.service == null && timeout < 0) {
			this.prepareClusterService();
			throw new ClusterExcp.Unready();
		}

		long startTime = System.currentTimeMillis();
		int leftTime;

		IClusterService cs = this.service;
		while (true) {
			if (timeout == 0) leftTime = 0;
			else {
				leftTime = timeout - (int) (System.currentTimeMillis() - startTime);
				if (leftTime <= 0) throw new ClusterExcp.Unready();
			}

			if (cs == null) cs = this.waitForClusterPreparing(startTime, leftTime);

			try {
				return cs.runTask(cUser, task, leftTime, subTaskTimeout);
			} catch (Exception e) {
				log.error("client run cluster task error.", e);
				if (isClusterUnready(e)) {
					cs = this.service = null;
					this.prepareClusterService();
					if (timeout < 0) throw new ClusterExcp.Unready(e);
				} else if (e instanceof RemoteException) {
					throw (RemoteException) e;
				} else if (e instanceof ClusterExcp.TaskTimeout) {
					throw (ClusterExcp.TaskTimeout) e;
				} else
					throw new RuntimeException("unknown exception: " + e, e);
			}
		}
	}

	private final AtomicBoolean isPreparingClusterService = new AtomicBoolean(false);

	private void prepareClusterService() {
		if (isPreparingClusterService.compareAndSet(false, true)) {
			Thread t = new Thread(this.rPrepareClusterService,
							"clusterClient:prepare-cluster-service" + System.currentTimeMillis());
			t.setDaemon(true);
			t.start();
		}
	}

	private final Runnable rPrepareClusterService = new Runnable() {
		@Override
		public void run() {
			try {
				if (service != null) return;

				bcForMaster();

				// in case of multi-responses, so end the loop until sock.receive timeout exception throw.
				while (true) {
					Cmd cmd = SockUtil.receiveCmd(cmdSock, CMD_SOCK_BUF);
					log.debug("receive cmd: " + cmd);
					if (service == null && cmd.action == Cmd.Action.I_AM_THE_MASTER) {
						try {
							service = IMasterService.getService(cmd.ipAddr, cmd.masterPort);
						} catch (Exception e) {
							log.error("connect to master service error.", e);
						}
					}
				}
			} catch (Exception e) {
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
			for (InterfaceAddress addr : bcAdds) {
				try {
					Cmd cmd = new Cmd(Cmd.Action.WHO_IS_THE_MASTER_BY_CLIENT, null, 0,
									addr.getAddress().getHostAddress(), addr.getNetworkPrefixLength(),
									cmdSock.getLocalPort(), 0);
					SockUtil.sendCmd(cmdSock, cmd, addr.getBroadcast(), cmdPort);
					log.debug("broadcast cmd: " + cmd);
				} catch (IOException e) {
					log.error("broadcast for master error, on interface: " + addr, e);
				}
			}
		}
	};

	/**
	 * @param startTime task submit time.
	 * @param timeout   waiting for cluster ready timeout(milli-second).
	 *                  <code>0</code> represent for never timeout.
	 *                  <code>negative</code> throws {@link ClusterExcp.Unready} immediately .
	 */
	private IClusterService waitForClusterPreparing(final long startTime, final int timeout)
					throws ClusterExcp.Unready, InterruptedException {
		if (timeout < 0) throw new ClusterExcp.Unready();

		IClusterService cs;
		while ((cs = this.service) == null) {
			this.prepareClusterService();
			Thread.sleep(10);
			if (timeout > 0 && timeout <= System.currentTimeMillis() - startTime)
				throw new ClusterExcp.Unready();
		}

		return cs;
	}

	private static boolean isClusterUnready(Exception e) {
		return ClusterNode.isNodeUnavailable(e)
						|| e instanceof ClusterExcp.NotMaster
						|| e instanceof ClusterExcp.NoWorkers
						|| e instanceof InterruptedException
						;
	}
}
