package mysh.msg;

import lombok.extern.slf4j.Slf4j;
import mysh.collect.Colls;
import mysh.net.Nets;
import mysh.util.Serializer;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @since 2019-11-06
 */
@Slf4j
public abstract class DefaultUdpUtil {
	
	public static final int DEFAULT_PORT = 44444;
	public static final int DEFAULT_UDP_PACK_BUF = 1200;
	
	public static MsgConsumer.MsgSupplier generateUdpConsumer(int port, int bufSize) throws SocketException {
		DatagramSocket sock = new DatagramSocket(port);
		ThreadLocal<DatagramPacket> tp = ThreadLocal.withInitial(() -> new DatagramPacket(new byte[bufSize], bufSize));
		
		return new MsgConsumer.MsgSupplier() {
			@Override
			public void close() {
				sock.close();
			}
			
			@Override
			public Msg<?> fetch() throws IOException {
				DatagramPacket p = tp.get();
				sock.receive(p);
				Msg<?> msg = Serializer.FST.deserialize(p.getData(), 0, p.getLength(), null);
				msg.setSockAddr(p.getSocketAddress());
				return msg;
			}
		};
	}
	
	public static MsgProducer.MsgSender generateUdpHandler(int broadcastPort, int maxDataSize) throws SocketException {
		DatagramSocket sock = new DatagramSocket();
		
		return new MsgProducer.MsgSender() {
			@Override
			public void send(Msg<?> msg) throws IOException {
				byte[] buf = Serializer.FST.serialize(msg);
				if (buf.length > maxDataSize)
					throw new RuntimeException("dataTooBig,serializationSize-exceeds:" + maxDataSize);
				DatagramPacket p = new DatagramPacket(buf, buf.length);
				
				if (msg.getSockAddr() == null) {
					List<InetAddress> ba = getBroadcastAddress();
					if (Colls.isEmpty(ba)) {
						log.warn("sendMsg-canceled,no-broadcast-address");
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
			}
			
			@Override
			public void close() {
				sock.close();
			}
		};
	}
	
	private static AtomicLong lastRenewBroadcastAddr = new AtomicLong();
	private static volatile List<InetAddress> broadcastAddr = new ArrayList<>();
	
	private static List<InetAddress> getBroadcastAddress() throws SocketException {
		long now = System.currentTimeMillis();
		long last = lastRenewBroadcastAddr.get();
		if (last + 60_000 < now) {
			if (lastRenewBroadcastAddr.compareAndSet(last, now)) {
				List<InetAddress> ba = new ArrayList<>();
				Nets.iterateNetworkIF(i -> i.getInterfaceAddresses().forEach(
						ia -> {
							if (ia.getBroadcast() != null)
								ba.add(ia.getBroadcast());
						}
				));
				broadcastAddr = ba;
			}
		}
		return broadcastAddr;
	}
}
