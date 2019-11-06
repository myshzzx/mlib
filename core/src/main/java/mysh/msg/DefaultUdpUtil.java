package mysh.msg;

import lombok.extern.slf4j.Slf4j;
import mysh.collect.Colls;
import mysh.net.Nets;
import mysh.util.Serializer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
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
		DatagramPacket p = new DatagramPacket(new byte[bufSize], bufSize);
		
		return new MsgConsumer.MsgSupplier() {
			@Override
			public void close() {
				sock.close();
			}
			
			@Override
			public Msg<?> fetch() throws IOException {
				sock.receive(p);
				return Serializer.FST.deserialize(p.getData(), 0, p.getLength(), null);
			}
		};
	}
	
	public static MsgProducer.MsgHandler generateUdpHandler(int targetPort, int maxDataSize) throws SocketException {
		DatagramSocket sock = new DatagramSocket();
		
		return new MsgProducer.MsgHandler() {
			@Override
			public void handle(Msg<?> msg) throws IOException {
				List<InetAddress> ba = getBroadcastAddress();
				if (Colls.isEmpty(ba)) {
					log.warn("sendMsg-canceled,no-broadcast-address");
					return;
				}
				byte[] buf = Serializer.FST.serialize(msg);
				if (buf.length > maxDataSize)
					throw new RuntimeException("dataTooBig,serializationSize-exceeds:" + maxDataSize);
				DatagramPacket p = new DatagramPacket(buf, buf.length);
				p.setPort(targetPort);
				
				for (InetAddress addr : ba) {
					p.setAddress(addr);
					try {
						sock.send(p);
					} catch (Exception e) {
						log.error("sendMsg-fail,addr={}", addr, e);
					}
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
