package mysh.msg;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

/**
 * @since 2019-11-06
 */
public class MsgProducer {
	public interface MsgSender extends Closeable {
		void send(Msg<?> msg) throws IOException;
		
		void close();
	}
	
	private MsgSender msgSender;
	
	public MsgProducer(MsgSender msgSender) {
		this.msgSender = Objects.requireNonNull(msgSender, "msgSender can't be null");
	}
	
	public void produce(Msg<?> msg) throws IOException {
		msgSender.send(msg);
	}
	
}
