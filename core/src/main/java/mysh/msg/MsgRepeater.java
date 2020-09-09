package mysh.msg;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AllArgsConstructor;
import lombok.Data;
import mysh.collect.Colls;
import mysh.util.Asserts;
import mysh.util.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @since 2020-06-03
 */
public class MsgRepeater {
	private static final Logger log = LoggerFactory.getLogger(MsgRepeater.class);
	
	private static final int heartBeatInterval = 3 * 60_000;
	
	private enum Cmd {
		/** 心跳 */
		HEARTBEAT,
		/** 单发 */
		DELIVER,
		/** 中继间转发 */
		REPEAT;
	}
	
	@Data
	@AllArgsConstructor
	static class RMsg implements Serializable {
		private static final long serialVersionUID = 500445993271143917L;
		private Cmd cmd;
		private byte[] data;
		private SocketAddress src;
		private SocketAddress dst;
		
		@Override
		public String toString() {
			return "RMsg{" +
					"cmd=" + cmd +
					", src=" + src +
					", dst=" + dst +
					'}';
		}
	}
	
	public interface Client extends Closeable {
		
		void send(@Nullable SocketAddress dst, byte[] buf);
		
		void close();
	}
	
	private static void sendMsg(RMsg msg, DatagramSocket sock, List<SocketAddress> targets) {
		if (Colls.isEmpty(targets))
			return;
		byte[] buf = Serializer.BUILD_IN.serialize(msg);
		if (buf.length > UdpUtil.UDP_PACK_SIZE) {
			log.error("sendMsg-dataTooBig,serializationSize-exceeds:" + UdpUtil.UDP_PACK_SIZE);
			return;
		}
		DatagramPacket p = new DatagramPacket(buf, buf.length);
		for (SocketAddress target : targets) {
			if (sock.isClosed())
				return;
			try {
				p.setSocketAddress(target);
				sock.send(p);
			} catch (Exception e) {
				log.error("send-msg-fail,target:{},msg:{}", target, msg, e);
			}
		}
	}
	
	public static Client createClient(DatagramSocket sock, List<SocketAddress> repeaters) {
		Asserts.notNull(sock, "sock");
		Asserts.require(Colls.isNotEmpty(repeaters), "repeaters can't be blank");
		
		ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(
				r -> {
					Thread thread = new Thread(r, MsgRepeater.class.getSimpleName() + "-client");
					thread.setDaemon(true);
					return thread;
				}
		);
		exec.scheduleAtFixedRate(() -> {
			sendMsg(new RMsg(Cmd.HEARTBEAT, null, null, null), sock, repeaters);
		}, 0, heartBeatInterval, TimeUnit.MILLISECONDS);
		
		log.info("{}-client start with repeaters:{}", MsgRepeater.class.getSimpleName(), repeaters);
		
		return new Client() {
			@Override
			public void send(@Nullable SocketAddress dst, byte[] buf) {
				sendMsg(new RMsg(Cmd.DELIVER, buf, null, dst), sock, repeaters);
			}
			
			@Override
			public void close() {
				exec.shutdownNow();
			}
		};
	}
	
	public interface Server extends Closeable {
		
		void close();
	}
	
	public static Server createServer(int port, int dispatchThreads, int udpPackBufSize) throws SocketException {
		DatagramSocket sock = new DatagramSocket(port);
		ThreadPoolExecutor exec =
				new ThreadPoolExecutor(dispatchThreads + 1, dispatchThreads + 1, 1, TimeUnit.MINUTES,
						new LinkedBlockingQueue<>(2000),
						r -> {
							Thread thread = new Thread(r, MsgRepeater.class.getSimpleName() + "-server");
							thread.setDaemon(true);
							return thread;
						},
						new ThreadPoolExecutor.DiscardOldestPolicy());
		exec.allowCoreThreadTimeOut(true);
		
		DatagramPacket p = new DatagramPacket(new byte[udpPackBufSize], udpPackBufSize);
		Cache<SocketAddress, Long> listeners =
				Caffeine.newBuilder()
				        .expireAfterWrite(heartBeatInterval * 3, TimeUnit.MILLISECONDS)
				        .build();
		exec.submit(() -> {
			Thread thread = Thread.currentThread();
			while (!thread.isInterrupted()) {
				try {
					sock.receive(p);
					SocketAddress src = p.getSocketAddress();
					RMsg msg = Serializer.BUILD_IN.deserialize(p.getData(), p.getOffset(), p.getLength(), null);
					long now = System.currentTimeMillis();
					switch (msg.cmd) {
						case HEARTBEAT:
							listeners.put(src, now);
							log.debug("listener-heartbeat:{}", src);
							break;
						case DELIVER:
							listeners.put(src, now);
							msg.src = src;
							exec.submit(() -> {
								try {
									log.debug("deliver-msg:{}", msg);
									if (msg.dst != null) {
										sendMsg(msg, sock, Collections.singletonList(msg.dst));
									} else {
										List<SocketAddress> targets =
												listeners.asMap().keySet().stream()
												         .filter(listener -> !Objects.equals(msg.src, listener))
												         .collect(Collectors.toList());
										sendMsg(msg, sock, targets);
									}
								} catch (Exception e) {
									log.error("exp-on-deliver:msg={}", msg, e);
								}
							});
							break;
					}
				} catch (Throwable e) {
					log.error("receive-msg-fail,from:{}", p.getSocketAddress(), e);
				}
			}
		});
		log.info("{}-server starts in {}, threads={}", MsgRepeater.class.getSimpleName(), port, dispatchThreads);
		return () -> {
			sock.close();
			exec.shutdownNow();
		};
	}
}
