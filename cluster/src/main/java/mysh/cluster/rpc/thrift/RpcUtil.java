package mysh.cluster.rpc.thrift;

import mysh.cluster.rpc.IFaceHolder;
import mysh.thrift.ThriftClientFactory;
import mysh.thrift.ThriftClientFactory.ClientHolder;
import mysh.thrift.ThriftServerFactory;
import mysh.util.Exps;
import mysh.util.Serializer;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mysh
 * @since 2014/11/19 11:02
 */
public class RpcUtil {
	private static final Logger log = LoggerFactory.getLogger(RpcUtil.class);
	public static final Serializer s = Serializer.fst;

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
					Class<I> svIf, I sv, int port, TServerEventHandler eventHandler, int poolSize,
					Map<String, ClassLoader> loaders) throws Throwable {
		ThriftServerFactory f = new ThriftServerFactory();
		f.setServerHost("0.0.0.0");
		f.setServerPort(port);
		f.setProcessor(new TClusterService.Processor<>(
						wrapService(svIf, sv, loaders == null ? new HashMap<>() : loaders)));
		f.setServerEventHandler(eventHandler);
		f.setServerPoolSize(poolSize);
		return f.build();
	}

	private static <I> TClusterService.Iface wrapService(
					Class<I> svIf, I sv, Map<String, ClassLoader> loaders) throws IOException {
		return new TClusterService.Iface() {
			ByteBuffer EMPTY = serialize("");
			Map<String, Method> methods = new HashMap<>();

			{
				for (Method method : svIf.getMethods()) {
					Method previous = methods.put(method.getName(), method);
					if (previous != null)
						throw new IllegalArgumentException("method overload is not allowed: " + method.getName());
				}
			}

			@Override
			public ByteBuffer invokeSvMethod(String ns, String methodName, ByteBuffer params) throws TException {
				try {
					ClassLoader cl = ns == null ? null : loaders.get(ns);
					Object result = methods.get(methodName).invoke(sv, unSerialize(params, cl));
					return result == null ? EMPTY : serialize((Serializable) result);
				} catch (Throwable e) {
					return wrapExp(e);
				}
			}
		};
	}

	/**
	 * @param <I> client interface type.
	 */
	public static <I> IFaceHolder<I> getClient(
					Class<I> svIf, String svHost, int svPort, int soTimeout, Map<String, ClassLoader> loaders) throws Throwable {
		ThriftClientFactory.Config<TClusterService.Iface> conf = new ThriftClientFactory.Config<>();
		conf.setServerHost(svHost);
		conf.setServerPort(svPort);
		conf.setClientSocketTimeout(soTimeout);
		conf.setIface(TClusterService.Iface.class);
		conf.setTClientClass(TClusterService.Client.class);
		ThriftClientFactory<TClusterService.Iface> f = new ThriftClientFactory<>(conf);
		ClientHolder<TClusterService.Iface> ch = f.buildPooled();
		return wrapSyncClient(svIf, ch, loaders == null ? new HashMap<>() : loaders);
	}

	@SuppressWarnings("unchecked")
	private static <I> IFaceHolder<I> wrapSyncClient(
					Class<I> svIf, ClientHolder<TClusterService.Iface> ch, Map<String, ClassLoader> loaders) {
		return new IFaceHolder<>(
						(I) Proxy.newProxyInstance(
										svIf.getClassLoader(),
										new Class[]{svIf},
										(obj, method, args) -> {
											String ns = null;
											String methodName = method.getName();
											if (methodName.equals("runSubTask")
															|| methodName.equals("subTaskComplete")
															|| methodName.equals("runTask"))
												ns = (String) args[0];

											ByteBuffer result = ch.getClient().invokeSvMethod(
															ns, methodName, serialize(args));
											Serializable sr = unSerialize(result, ns == null ? null : loaders.get(ns));
											if (sr instanceof Throwable) throw (Throwable) sr;
											else return sr;
										}),
						ch);
	}

	private static ByteBuffer wrapExp(Throwable e) {
		return serialize(e);
	}

	private static ByteBuffer serialize(Serializable obj) {
		if (obj == null) return null;

		try {
			return ByteBuffer.wrap(s.serialize(obj));
		} catch (Throwable e) {
			log.error("serialize obj error:" + obj, e);
			throw new RuntimeException(e);
		}
	}

	private static <T extends Serializable> T unSerialize(ByteBuffer buf, ClassLoader cl) {
		if (buf == null) return null;

		try {
			return s.deserialize(buf.array(), buf.position(), buf.limit() - buf.position(), cl);
		} catch (Throwable e) {
			byte[] b = new byte[buf.capacity() - buf.position()];
			System.arraycopy(buf.array(), buf.position(), b, 0, b.length);
			log.error("deserialize obj error, byte=" + Base64.getEncoder().encodeToString(b), e);
			throw Exps.unchecked(e);
		}
	}

}
