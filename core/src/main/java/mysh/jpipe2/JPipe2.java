package mysh.jpipe2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import mysh.util.Range;
import mysh.util.Strings;

import java.io.Closeable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author <pre>凯泓(zhixian.zzx@alibaba-inc.com)</pre>
 * @since 2019-12-20
 */
@Slf4j
public class JPipe2 implements Closeable {
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private ChannelFuture mainChannel;
	
	public JPipe2(int port, String remoteHost, int remotePort, String name, int workerThreadCount) {
		name = Strings.isNotBlank(name) ? name : "jpipe2";
		workerThreadCount = Range.within(1, Runtime.getRuntime().availableProcessors() * 2, workerThreadCount);
		
		bossGroup = new NioEventLoopGroup(1, newFactory(name + "-b-"));
		workerGroup = new NioEventLoopGroup(workerThreadCount, newFactory(name + "-w-"));
		ServerBootstrap b = new ServerBootstrap()
				.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 128)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline()
						  .addLast(new ChannelInboundHandlerAdapter() {
							  @Override
							  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
								  ByteBuf b = (ByteBuf) msg;
								  // ctx.writeAndFlush(msg);
								  b.resetReaderIndex();
								  b.resetWriterIndex();
							  }
						  }, new ChannelInboundHandlerAdapter() {
							  @Override
							  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
								  ByteBuf b = (ByteBuf) msg;
								  ctx.writeAndFlush(msg);
								  // b.resetReaderIndex();
							  }
						  })
						;
					}
				});
		
		try {
			mainChannel = b.bind(port);
		} catch (Exception e) {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
			throw e;
		}
	}
	
	private ThreadFactory newFactory(String name) {
		AtomicInteger c = new AtomicInteger(1);
		return r -> {
			Thread thread = new Thread(r, name + c.getAndIncrement());
			thread.setDaemon(true);
			return thread;
		};
	}
	
	@Override
	public void close() {
		try {
			mainChannel.channel().close();
		} catch (Exception e) {
			log.error("jpipe2 close fail", e);
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}
}
