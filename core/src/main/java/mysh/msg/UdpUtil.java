package mysh.msg;

import mysh.collect.Colls;
import mysh.collect.Pair;
import mysh.net.Nets;
import mysh.util.Asserts;
import mysh.util.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @since 2019-11-06
 */
public abstract class UdpUtil {
	private static final Logger log = LoggerFactory.getLogger(UdpUtil.class);
	
	public static final int UDP_PACK_SIZE = 1472;
	public static final int UDP_PACK_SIZE_INTERNET = 508;
	
	public static MsgConsumer.MsgReceiver generateUdpReceiver(int port) throws SocketException {
		return generateUdpReceiver(port, UDP_PACK_SIZE);
	}
	
	public static MsgConsumer.MsgReceiver generateUdpReceiver(int port, int bufSize) throws SocketException {
		DatagramSocket sock = bindBroadcastUdpSock(port);
		return generateUdpReceiver(sock, bufSize);
	}
	
	private static MsgConsumer.MsgReceiver generateUdpReceiver(DatagramSocket sock, int bufSize) {
		Asserts.require(bufSize > 0, "illegal-udpPackBufSize:" + bufSize);
		ThreadLocal<DatagramPacket> tp = ThreadLocal.withInitial(() -> new DatagramPacket(new byte[bufSize], bufSize));
		
		return new MsgConsumer.MsgReceiver() {
			@Override
			public void close() {
				sock.close();
			}
			
			@Override
			public Msg<?> fetch() throws IOException {
				DatagramPacket p = tp.get();
				sock.receive(p);
				Object m = Serializer.BUILD_IN.deserialize(p.getData(), p.getOffset(), p.getLength(), null);
				Msg<?> msg;
				if (m instanceof MsgRepeater.RMsg) {
					MsgRepeater.RMsg rm = (MsgRepeater.RMsg) m;
					msg = Serializer.BUILD_IN.deserialize(rm.getData());
					msg.setSockAddr(rm.getSrc());
				} else {
					msg = (Msg<?>) m;
					msg.setSockAddr(p.getSocketAddress());
				}
				return msg;
			}
		};
	}
	
	public static MsgProducer.MsgSender generateUdpSender(int broadcastPort) throws SocketException {
		return generateUdpSender(broadcastPort, UDP_PACK_SIZE);
	}
	
	public static MsgProducer.MsgSender generateUdpSender(int broadcastPort, int maxUdpPackSize) throws SocketException {
		DatagramSocket sock = bindBroadcastUdpSock(null);
		return generateUdpSender(sock, broadcastPort, maxUdpPackSize, null);
	}
	
	private static MsgProducer.MsgSender generateUdpSender(DatagramSocket sock, int broadcastPort, int maxUdpPackSize,
	                                                       @Nullable List<SocketAddress> repeaters) {
		Asserts.notNull(sock, "sock");
		Asserts.require(broadcastPort > 0 && broadcastPort < 65536, "illegal-broadcastPort:" + broadcastPort);
		Asserts.require(maxUdpPackSize > 0, "illegal-maxUdpPackSize:" + maxUdpPackSize);
		
		MsgRepeater.Client msgRepeater = Colls.isNotEmpty(repeaters) ? MsgRepeater.createClient(sock, repeaters) : null;
		return new MsgProducer.MsgSender() {
			@Override
			public void send(Msg<?> msg) throws IOException {
				byte[] buf = Serializer.BUILD_IN.serialize(msg);
				if (buf.length > maxUdpPackSize)
					throw new RuntimeException("dataTooBig,serializationSize-exceeds:" + maxUdpPackSize);
				DatagramPacket p = new DatagramPacket(buf, buf.length);
				
				if (msg.getSockAddr() == null) {
					List<InetAddress> ba = getBroadcastAddress();
					if (Colls.isEmpty(ba) && msgRepeater == null) {
						log.warn("sendMsg-canceled,no-broadcast-address-or-repeater");
						return;
					}
					p.setPort(broadcastPort);
					for (InetAddress addr : ba) {
						p.setAddress(addr);
						try {
							sock.send(p);
						} catch (Exception e) {
							log.error("sendMsg-broadcast-fail,addr={}", addr, e);
						}
					}
				} else {
					p.setSocketAddress(msg.getSockAddr());
					sock.send(p);
				}
				
				if (msgRepeater != null)
					msgRepeater.send(msg.getSockAddr(), buf);
			}
			
			public void close() {
				if (msgRepeater != null)
					msgRepeater.close();
				sock.close();
			}
		};
	}
	
	public static Pair<MsgConsumer.MsgReceiver, MsgProducer.MsgSender> generateUdpReceiverSender(
			int port, @Nullable List<SocketAddress> repeaters) throws SocketException {
		return generateUdpReceiverSender(port, UDP_PACK_SIZE, repeaters);
	}
	
	public static Pair<MsgConsumer.MsgReceiver, MsgProducer.MsgSender> generateUdpReceiverSender(
			int port, int udpPackBufSize, @Nullable List<SocketAddress> repeaters) throws SocketException {
		DatagramSocket sock = bindBroadcastUdpSock(port);
		MsgConsumer.MsgReceiver msgReceiver = generateUdpReceiver(sock, udpPackBufSize);
		MsgProducer.MsgSender msgSender = generateUdpSender(sock, port, udpPackBufSize, repeaters);
		return Pair.of(msgReceiver, msgSender);
	}
	
	private static DatagramSocket bindBroadcastUdpSock(Integer port) throws SocketException {
		DatagramSocket sock = new DatagramSocket(null);
		sock.setBroadcast(true);
		sock.bind(port != null ? new InetSocketAddress(port) : null);
		return sock;
	}
	
	private static AtomicLong lastRenewBroadcastAddr = new AtomicLong();
	private static volatile List<InetAddress> broadcastAddr = new ArrayList<>();
	
	private static List<InetAddress> getBroadcastAddress() throws SocketException {
		long now = System.currentTimeMillis();
		long last = lastRenewBroadcastAddr.get();
		if (last + 60_000 < now) {
			if (lastRenewBroadcastAddr.compareAndSet(last, now)) {
				broadcastAddr = Nets.getBroadcastAddress();
			}
		}
		return broadcastAddr;
	}
}
