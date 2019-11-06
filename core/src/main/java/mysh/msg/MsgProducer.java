package mysh.msg;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Objects;

/**
 * @since 2019-11-06
 */
public class MsgProducer implements Closeable {
	public interface MsgSender extends Closeable {
		void send(Msg<?> msg) throws IOException;
	}
	
	private static MsgSender DEFAULT_UDP_HANDLER;
	
	private MsgSender handler;
	
	public MsgProducer() throws SocketException {
		this(getDefaultUdpHandler());
	}
	
	private static MsgSender getDefaultUdpHandler() throws SocketException {
		synchronized (MsgConsumer.class) {
			if (DEFAULT_UDP_HANDLER == null) {
				DEFAULT_UDP_HANDLER = DefaultUdpUtil.generateUdpHandler(
						DefaultUdpUtil.DEFAULT_PORT, DefaultUdpUtil.DEFAULT_UDP_PACK_BUF);
			}
			return DEFAULT_UDP_HANDLER;
		}
	}
	
	public MsgProducer(MsgSender handler) {
		this.handler = Objects.requireNonNull(handler, "handler can't be null");
	}
	
	public void produce(InetAddress targetAddr, Integer targetPort, String topic, Object data) throws IOException {
		Msg<Object> msg = new Msg<>(topic, data);
		if (targetAddr != null && targetPort != null)
			msg.setSockAddr(new InetSocketAddress(targetAddr, targetPort));
		handler.send(msg);
	}
	
	@Override
	public void close() throws IOException {
		handler.close();
	}
}
