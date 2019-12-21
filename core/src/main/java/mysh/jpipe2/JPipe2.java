package mysh.jpipe2;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import mysh.util.Range;
import mysh.util.Strings;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.util.Objects;
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
	private EventLoopGroup remoteGroup;
	private ChannelFuture mainChannel;
	@Getter
	private String remoteHost;
	
	public JPipe2(int port, String remoteHost, int remotePort, String name, int workerThreadCount) {
		this(port, remoteHost, remotePort, name, workerThreadCount, null, null);
	}
	
	public JPipe2(int port, String remoteHost, int remotePort, String name, int workerThreadCount,
	              @Nullable ChannelHandler[] localHandlers, @Nullable ChannelHandler[] remoteHandlers) {
		this.remoteHost = remoteHost;
		name = Strings.isNotBlank(name) ? name : "jpipe2";
		workerThreadCount = Range.within(1, Runtime.getRuntime().availableProcessors() * 2, workerThreadCount);
		
		bossGroup = new NioEventLoopGroup(1, newFactory(name + "-boss-"));
		workerGroup = new NioEventLoopGroup(workerThreadCount, newFactory(name + "-worker-"));
		remoteGroup = new NioEventLoopGroup(workerThreadCount, newFactory(name + "-remote-"));
		ServerBootstrap b = new ServerBootstrap()
				.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 128)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						if (localHandlers != null)
							ch.pipeline().addLast(localHandlers);
						
						ch.pipeline().addLast(
								new ChannelInboundHandlerAdapter() {
									private ChannelFuture remoteChannel;
									
									@Override
									public void channelRead(ChannelHandlerContext localCtx, Object localMsg) throws Exception {
										if (remoteChannel == null) {
											Bootstrap b = new Bootstrap();
											b.group(remoteGroup);
											b.channel(NioSocketChannel.class);
											b.option(ChannelOption.SO_KEEPALIVE, true);
											b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
											b.handler(new ChannelInitializer<SocketChannel>() {
												@Override
												public void initChannel(SocketChannel ch) throws Exception {
													if (remoteHandlers != null)
														ch.pipeline().addLast(remoteHandlers);
													
													ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
														@Override
														public void channelRead(ChannelHandlerContext remoteCtx, Object remoteMsg) throws Exception {
															localCtx.writeAndFlush(remoteMsg);
														}
														
														@Override
														public void exceptionCaught(ChannelHandlerContext remoteCtx, Throwable cause) throws Exception {
															if (!Objects.equals("Connection reset", cause.getMessage()))
																log.error("remote-channel-exp,{}:{}->local:{},exp={}", remoteHost, remotePort, port, cause.toString());
															remoteCtx.close();
															localCtx.close();
														}
														
														@Override
														public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
															super.channelUnregistered(ctx);
															// remote unreachable
															if (!ctx.channel().isOpen())
																localCtx.close();
														}
														
													});
												}
											});
											remoteChannel = b.connect(remoteHost, remotePort).addListener(
													(ChannelFutureListener) future -> future.channel().writeAndFlush(localMsg));
										} else
											remoteChannel.channel().writeAndFlush(localMsg);
									}
									
									@Override
									public void exceptionCaught(ChannelHandlerContext localCtx, Throwable cause) throws Exception {
										if (!Objects.equals("Connection reset", cause.getMessage()))
											log.error("local-channel-exp,local:{}->{}:{},exp={}", port, remoteHost, remotePort, cause.toString());
										localCtx.close();
										if (remoteChannel != null)
											remoteChannel.channel().close();
									}
								});
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
