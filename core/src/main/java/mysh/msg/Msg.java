package mysh.msg;

import lombok.Data;
import mysh.util.IdGen;

import java.io.Serializable;
import java.net.SocketAddress;

/**
 * @since 2019-11-06
 */
@Data
public class Msg<T> implements Serializable {
	private static final long serialVersionUID = 7878169029248133376L;
	
	private long id;
	private String topic;
	private T data;
	private transient SocketAddress sockAddr;
	
	public Msg(String topic, T data) {
		this.id= IdGen.timeBasedId();
		this.topic = topic;
		this.data = data;
	}
	
}
