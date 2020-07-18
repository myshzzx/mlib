package mysh.socks5.handler.ss5;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Socks5InitialRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5InitialRequest> {
	private static final Logger logger = LoggerFactory.getLogger(Socks5InitialRequestHandler.class);
	
	private final boolean isAuth;
	
	public Socks5InitialRequestHandler(boolean isAuth) {
		this.isAuth = isAuth;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5InitialRequest msg) throws Exception {
		// logger.debug("init ss5 conn: " + msg);
		if (msg.decoderResult().isFailure()) {
			// logger.debug("NOT socks5: {}", msg);
			// ctx.fireChannelRead(msg);
			ctx.close();
		} else {
			if (msg.version().equals(SocksVersion.SOCKS5)) {
				Socks5InitialResponse initialResponse;
				if (isAuth) {
					initialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD);
				} else {
					initialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH);
				}
				ctx.writeAndFlush(initialResponse);
			}
		}
	}
	
}
