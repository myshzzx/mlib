package mysh.socks5;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.logging.LoggingHandler;
import mysh.socks5.auth.PasswordAuth;
import mysh.socks5.handler.ChannelListener;
import mysh.socks5.handler.ss5.Socks5CommandRequestHandler;
import mysh.socks5.handler.ss5.Socks5InitialRequestHandler;
import mysh.socks5.handler.ss5.Socks5PasswordAuthRequestHandler;
import mysh.socks5.log.ProxyFlowLog;
import mysh.util.Exps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * copyright
 * https://github.com/xtuhcy/socks5-netty
 * https://my.oschina.net/u/2336761
 */
public class Socks5Proxy implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(Socks5Proxy.class);
	
	private PasswordAuth passwordAuth;
	private boolean nettyLog;
	private ProxyFlowLog proxyFlowLog;
	private ChannelListener channelListener;
	
	public Socks5Proxy nettyLog(boolean nettyLog) {
		this.nettyLog = nettyLog;
		return this;
	}
	
	public Socks5Proxy proxyFlowLog(ProxyFlowLog proxyFlowLog) {
		this.proxyFlowLog = proxyFlowLog;
		return this;
	}
	
	public Socks5Proxy channelListener(ChannelListener channelListener) {
		this.channelListener = channelListener;
		return this;
	}
	
	public Socks5Proxy passwordAuth(PasswordAuth passwordAuth) {
		if (passwordAuth != null) {
			this.passwordAuth = passwordAuth;
		}
		return this;
	}
	
	private boolean isAuth() {
		return passwordAuth != null;
	}
	
	private Channel mainChannel;
	private int port;
	private EventLoopGroup dispatcherGroup;
	private EventLoopGroup workerGroup;
	private EventLoopGroup connGroup;
	
	private ThreadFactory newFactory(String name) {
		AtomicInteger c = new AtomicInteger(1);
		return r -> {
			Thread thread = new Thread(r, name + c.getAndIncrement());
			thread.setDaemon(true);
			return thread;
		};
	}
	
	public synchronized Socks5Proxy start(int port) throws Exception {
		if (mainChannel != null)
			throw new RuntimeException(Socks5Proxy.class.getSimpleName() + " can't be started again");
		
		dispatcherGroup = new NioEventLoopGroup(1, newFactory("sock5Proxy-" + port + "-dispatcher-"));
		workerGroup = new NioEventLoopGroup(newFactory("sock5Proxy-" + port + "-worker-"));
		connGroup = new NioEventLoopGroup(newFactory("sock5Proxy-" + port + "-conn-"));
		this.port = port;
		
		try {
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(dispatcherGroup, workerGroup)
			         .channel(NioServerSocketChannel.class)
			         .option(ChannelOption.SO_BACKLOG, 1024)
			         .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
			         .childHandler(new ChannelInitializer<SocketChannel>() {
				         @Override
				         protected void initChannel(SocketChannel ch) throws Exception {
					         //流量统计
					         // ch.pipeline().addLast(
					         //         ProxyChannelTrafficShapingHandler.PROXY_TRAFFIC,
					         //         new ProxyChannelTrafficShapingHandler(3000, proxyFlowLog, channelListener)
					         // );
					         //channel超时处理
					         // ch.pipeline().addLast(new IdleStateHandler(3, 30, 0));
					         // ch.pipeline().addLast(new ProxyIdleHandler());
					         //netty日志
					         if (nettyLog) {
						         ch.pipeline().addLast(new LoggingHandler());
					         }
					         //Socks5MessagByteBuf
					         ch.pipeline().addLast(Socks5ServerEncoder.DEFAULT);
					         //sock5 init
					         ch.pipeline().addLast(new Socks5InitialRequestDecoder());
					         //sock5 init
					         ch.pipeline().addLast(new Socks5InitialRequestHandler(isAuth()));
					         if (isAuth()) {
						         //socks auth
						         ch.pipeline().addLast(new Socks5PasswordAuthRequestDecoder());
						         //socks auth
						         ch.pipeline().addLast(new Socks5PasswordAuthRequestHandler(passwordAuth));
					         }
					         //socks connection
					         ch.pipeline().addLast(new Socks5CommandRequestDecoder());
					         //Socks connection
					         ch.pipeline().addLast(new Socks5CommandRequestHandler(connGroup));
				         }
			         });
			
			mainChannel = bootstrap.bind(port).sync().channel();
			logger.info("Socks5Proxy-bind-port: {}", port);
			return this;
		} catch (Exception e) {
			close();
			throw Exps.unchecked(e);
		}
	}
	
	@Override
	public void close() {
		try {
			if (mainChannel != null) {
				logger.warn("Socks5Proxy-closing: {}", port);
				mainChannel.close();
			}
		} catch (Exception e) {
		} finally {
			dispatcherGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}
