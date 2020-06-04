package mysh.socksproxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import mysh.util.Exps;

import java.io.Closeable;

public final class SocksServer implements Closeable {
	
	private Channel mainChannel;
	private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
	private EventLoopGroup workerGroup = new NioEventLoopGroup();
	
	public synchronized SocksServer start(int port) {
		if (mainChannel != null)
			throw new RuntimeException(SocksServer.class.getSimpleName() + " can't be started again");
		
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup)
		 .channel(NioServerSocketChannel.class)
		 // .handler(new LoggingHandler(LogLevel.ERROR))
		 .childHandler(new SocksServerInitializer());
		try {
			mainChannel = b.bind(port).sync().channel();
		} catch (Exception e) {
			close();
			throw Exps.unchecked(e);
		}
		return this;
	}
	
	@Override
	public void close() {
		try {
			if (mainChannel != null)
				mainChannel.close();
		} catch (Exception e) {
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}
