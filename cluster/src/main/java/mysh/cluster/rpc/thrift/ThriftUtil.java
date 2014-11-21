package mysh.cluster.rpc.thrift;

import mysh.cluster.rpc.IfaceHolder;
import mysh.thrift.ThriftClientFactory;
import mysh.thrift.ThriftServerFactory;
import mysh.util.SerializeUtil;
import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServerEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mysh
 * @since 2014/11/19 11:02
 */
public class ThriftUtil {
	private static final Logger log = LoggerFactory.getLogger(ThriftUtil.class);

	public static void startTServer(TServer s) {
		Thread t = new Thread("thrift-server-holder") {
			@Override
			public void run() {
				s.serve();
			}
		};
		t.setDaemon(true);
		t.start();
	}

	public static <I> TServer exportTServer(
					Class<I> svIf, I sv, int port, TServerEventHandler eventHandler, int poolSize) throws Exception {
		ThriftServerFactory f = new ThriftServerFactory();
		f.setServerHost("0.0.0.0");
		f.setServerPort(port);
		f.setProcessor(new TClusterService.Processor<>(wrapService(svIf, sv)));
		f.setServerEventHandler(eventHandler);
		f.setServerPoolSize(poolSize);
		return f.build();
	}

	private static <I> TClusterService.Iface wrapService(Class<I> svIf, I sv) throws IOException {
		return new TClusterService.Iface() {
			ByteBuffer EMPTY = serialize("");
			Map<String, Method> methods = new HashMap<>();

			{
				for (Method method : svIf.getMethods()) {
					methods.put(method.getName(), method);
				}
			}

			@Override
			public ByteBuffer invokeSvMethod(String methodName, ByteBuffer params) throws TException {
				try {
					Object result = methods.get(methodName).invoke(sv, unSerialize(params));
					return result == null ? EMPTY : serialize((Serializable) result);
				} catch (Exception e) {
					return wrapExp(e);
				}
			}
		};
	}

	/**
	 * @param <I> client interface type.
	 */
	public static <I> IfaceHolder<I> getClient(
					Class<I> svIf, String svHost, int svPort, int soTimeout) throws Exception {
		ThriftClientFactory.Config<TClusterService.Iface> conf = new ThriftClientFactory.Config<>();
		conf.setServerHost(svHost);
		conf.setServerPort(svPort);
		conf.setClientSocketTimeout(soTimeout);
		conf.setIface(TClusterService.Iface.class);
		conf.setTClientClass(TClusterService.Client.class);
		ThriftClientFactory<TClusterService.Iface> f = new ThriftClientFactory<>(conf);
		ThriftClientFactory.ClientHolder<TClusterService.Iface> ch = f.buildPooled();
		return wrapSyncClient(svIf, ch);
	}

	@SuppressWarnings("unchecked")
	private static <I> IfaceHolder<I> wrapSyncClient(
					Class<I> svIf, ThriftClientFactory.ClientHolder<TClusterService.Iface> ch) {
		return new IfaceHolder<>(
						(I) Proxy.newProxyInstance(svIf.getClassLoader(), new Class[]{svIf},
										(obj, method, args) -> {
											ByteBuffer result = ch.getClient().invokeSvMethod(method.getName(), serialize(args));
											Serializable sr = unSerialize(result);
											if (sr instanceof Throwable) throw (Throwable) sr;
											else return sr;
										}),
						ch);
	}

	private static ByteBuffer wrapExp(Exception e) {
		try {
			return serialize(e);
		} catch (IOException ex) {
			log.error("serialize exception error.", ex);
			return null;
		}
	}

	private static ByteBuffer serialize(Serializable obj) throws IOException {
		if (obj == null) return null;

		return ByteBuffer.wrap(SerializeUtil.serialize(obj));
	}

	private static <T extends Serializable> T unSerialize(ByteBuffer buf)
					throws IOException, ClassNotFoundException {
		if (buf == null) return null;

		return SerializeUtil.unSerialize(buf.array(), buf.position(), buf.capacity() - buf.position());
	}

}
