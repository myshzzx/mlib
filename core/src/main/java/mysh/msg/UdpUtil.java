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
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 以太网EthernetII最大的数据帧是1518B, 其中最大承载数据量是1500B(MTU)
 * <p>
 * 单帧UDP包最大可承载数据 = MTU - IP头(20) - UDP头(8) = 1472B
 * <p>
 * MTU(PPPoE) = 1492B, 相应单帧UDP包最大承载 1464B
 * <p>
 * MTU(Internet) = 576B, 相应单帧UDP包最大承载 548B
 * <p>
 * UDP数据报包含报头在内最大64kB(OS底层函数一次可发的最大UDP包尺寸),
 * 超过MTU时将被拆分成多帧发送, 接收方再进行重组, 只要有一个分片重组失败,
 * 则整个数据报作废丢弃
 *
 * @since 2019-11-06
 */
public abstract class UdpUtil {
	private static final Logger log = LoggerFactory.getLogger(UdpUtil.class);
	
	public static final int UDP_PACK_BUF = 65535;
	
	public static MsgConsumer.MsgReceiver generateUdpReceiver(int port) throws SocketException {
		DatagramSocket sock = bindBroadcastUdpSock(port);
		return generateUdpReceiver(sock, UDP_PACK_BUF);
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
		DatagramSocket sock = bindBroadcastUdpSock(null);
		return generateUdpSender(sock, broadcastPort, UDP_PACK_BUF, null);
	}
	
	private static MsgProducer.MsgSender generateUdpSender(DatagramSocket sock, int broadcastPort, int maxUdpPackSize,
	                                                       @Nullable Collection<SocketAddress> repeaters) {
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
			int port, @Nullable Collection<SocketAddress> repeaters) throws SocketException {
		return generateUdpReceiverSender(port, UDP_PACK_BUF, repeaters);
	}
	
	public static Pair<MsgConsumer.MsgReceiver, MsgProducer.MsgSender> generateUdpReceiverSender(
			int port, int udpPackBufSize, @Nullable Collection<SocketAddress> repeaters) throws SocketException {
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
