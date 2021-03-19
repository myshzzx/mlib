package mysh.jpipe2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author mysh
 * @since 2019-12-21
 */
@Disabled
public class JPipe2Test {
	
	@Test
	public void pipe() throws InterruptedException {
		JPipe2 j = new JPipe2(8080, "l", 80, "test", 1);
		new CountDownLatch(1).await();
	}
	
	@Test
	public void handlers() throws InterruptedException {
		JPipe2 j = new JPipe2(8080, "l", 80, "test", 1,
				new ChannelHandler[]{
						new ByteToMessageDecoder() {
							@Override
							protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
								ByteBuf buf = ctx.alloc().buffer().writeBytes(in);
								ctx.write(ctx.alloc().buffer().writeBytes("that's your input: ".getBytes()));
								ctx.writeAndFlush(buf.copy());
								out.add(buf);
							}
						},
						new MessageToByteEncoder<ByteBuf>() {
							@Override
							protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
								out.writeBytes(msg);
							}
						}
				}, null);
		new CountDownLatch(1).await();
	}
}