
package mysh.jpipe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

/**
 * 数据推送器.<br/>
 * 把 源Socket 发送的数据取下来, 缓存, 并交插件进行处理( 按插件列表顺序执行插件 ), 然后发送给 目标Socket.<br/>
 * Pusher 有类型, 即 本地Pusher (把本地数据发送到远程) 还是 远程Pusher.
 *
 * @author ZhangZhx
 */
public class Pusher implements PusherStateController {
	private static final Logger log = LoggerFactory.getLogger(Pusher.class);

	/**
	 * Pusher 类型.<br/>
	 * 本地Pusher (把本地数据发送到远程) 还是 远程Pusher.
	 *
	 * @author ZhangZhx
	 */
	public static enum Type {
		/**
		 * 本地Pusher.
		 */
		LOCAL,

		/**
		 * 远程Pusher
		 */
		REMOTE;
	}

	private final Type type;

	private final Socket src;

	private final Socket dst;

	private final List<Plugin> plugins;
	private Runnable closeNotifier;

	/**
	 * 缓冲区.<br/>
	 */
	private final byte[] buf;

	/**
	 * 缓冲区有效数据长度.<br/>
	 * 这里用 volatile 保证它的可见性, 原因是插件可能把这个 PusherStateController 实例发布到其他线程中.
	 */
	private volatile int bufDataLength;

	private final Thread dataPusher = new Thread() {

		public void run() {

			try (InputStream in = Pusher.this.src.getInputStream();
			     OutputStream out = Pusher.this.dst.getOutputStream()) {

				while ((Pusher.this.bufDataLength = in.read(Pusher.this.buf)) != -1) {
					// 交插件处理
					for (Plugin plugin : Pusher.this.plugins) {
						if (Pusher.this.type == Pusher.Type.LOCAL)
							plugin.handleLocalData(Pusher.this);
						else if (Pusher.this.type == Pusher.Type.REMOTE)
							plugin.handleRemoteData(Pusher.this);
					}

					// 发送数据
					out.write(Pusher.this.buf, 0, Pusher.this.bufDataLength);
					out.flush();
				}
			} catch (IOException e) {
				// System.err.println("从 " + Pusher.this.src + " 到 " + Pusher.this.dst
				// + " 的数据推送器异常, " + e);
			} finally {
				closeNotifier.run();
			}
		}
	};

	public Pusher(Type type, Socket src, Socket dst, List<Plugin> plugins, int bufLen, Runnable closeNotifier) {
		this.type = type;
		this.src = src;
		this.dst = dst;
		this.plugins = plugins;
		this.closeNotifier = closeNotifier;

		this.buf = new byte[bufLen];
		this.dataPusher.setName("jpipe.Pusher." + type);
	}

	/**
	 * 启动推送器.
	 */
	public void start() {

		this.dataPusher.start();
	}

	@Override
	public byte[] getDataBuf() {

		return this.buf;
	}

	@Override
	public int getAvailableDataLength() {

		return this.bufDataLength;
	}

}
