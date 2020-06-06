package mysh.socks5.log;

import io.netty.channel.ChannelHandlerContext;

public interface ProxyFlowLog {

	void log(ChannelHandlerContext ctx);
	
}
