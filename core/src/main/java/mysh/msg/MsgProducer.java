package mysh.msg;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketException;
import java.util.Objects;

/**
 * @since 2019-11-06
 */
public class MsgProducer implements Closeable {
	public interface MsgHandler extends Closeable {
		void handle(Msg<?> msg) throws IOException;
	}
	
	private static MsgHandler DEFAULT_UDP_HANDLER;
	
	private MsgHandler handler;
	
	public MsgProducer() throws SocketException {
		this(getDefaultUdpHandler());
	}
	
	private static MsgHandler getDefaultUdpHandler() throws SocketException {
		synchronized (MsgConsumer.class) {
			if (DEFAULT_UDP_HANDLER == null) {
				DEFAULT_UDP_HANDLER = DefaultUdpUtil.generateUdpHandler(
						DefaultUdpUtil.DEFAULT_PORT, DefaultUdpUtil.DEFAULT_UDP_PACK_BUF);
			}
			return DEFAULT_UDP_HANDLER;
		}
	}
	
	public MsgProducer(MsgHandler handler) {
		this.handler = Objects.requireNonNull(handler, "handler can't be null");
	}
	
	public void produce(String topic, Object data) throws IOException {
		handler.handle(new Msg<>(topic, data));
	}
	
	@Override
	public void close() throws IOException {
		handler.close();
	}
}
