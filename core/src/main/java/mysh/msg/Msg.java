package mysh.msg;

import lombok.Data;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * @since 2019-11-06
 */
@Data
public class Msg<T> implements Serializable {
	private static final long serialVersionUID = 7878169029248133376L;
	
	private String topic;
	private T data;
	private transient InetSocketAddress sockAddr;
	
	public Msg(String topic, T data) {
		this.topic = topic;
		this.data = data;
	}
}
