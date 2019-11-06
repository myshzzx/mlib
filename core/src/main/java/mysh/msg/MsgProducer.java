package mysh.msg;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

/**
 * @since 2019-11-06
 */
public class MsgProducer implements Closeable {
	public interface MsgSender extends Closeable {
		void send(Msg<?> msg) throws IOException;
	}
	
	private MsgSender handler;
	
	public MsgProducer(MsgSender msgSender) {
		this.handler = Objects.requireNonNull(msgSender, "msgSender can't be null");
	}
	
	public void produce(Msg<?> msg) throws IOException {
		handler.send(msg);
	}
	
	@Override
	public void close() throws IOException {
		handler.close();
	}
}
