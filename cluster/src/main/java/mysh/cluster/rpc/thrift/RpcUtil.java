package mysh.cluster.rpc.thrift;

import mysh.cluster.rpc.IFaceHolder;
import mysh.thrift.ThriftClientFactory;
import mysh.thrift.ThriftServerFactory;
import mysh.util.Serializer;
import mysh.util.Serializer.ClassLoaderFetcher;
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
					ClassLoaderFetcher clFetcher) throws Exception {
		ThriftServerFactory f = new ThriftServerFactory();
		f.setServerHost("0.0.0.0");
		f.setServerPort(port);
		f.setProcessor(new TClusterService.Processor<>(wrapService(svIf, sv, clFetcher)));
		f.setServerEventHandler(eventHandler);
		f.setServerPoolSize(poolSize);
		return f.build();
	}

	private static <I> TClusterService.Iface wrapService(
					Class<I> svIf, I sv, ClassLoaderFetcher clFetcher) throws IOException {
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
					Object result = methods.get(methodName).invoke(sv, unSerialize(params, clFetcher));
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
	public static <I> IFaceHolder<I> getClient(
					Class<I> svIf, String svHost, int svPort, int soTimeout, ClassLoaderFetcher clFetcher) throws Exception {
		ThriftClientFactory.Config<TClusterService.Iface> conf = new ThriftClientFactory.Config<>();
		conf.setServerHost(svHost);
		conf.setServerPort(svPort);
		conf.setClientSocketTimeout(soTimeout);
		conf.setIface(TClusterService.Iface.class);
		conf.setTClientClass(TClusterService.Client.class);
		ThriftClientFactory<TClusterService.Iface> f = new ThriftClientFactory<>(conf);
		ThriftClientFactory.ClientHolder<TClusterService.Iface> ch = f.buildPooled();
		return wrapSyncClient(svIf, ch, clFetcher);
	}

	@SuppressWarnings("unchecked")
	private static <I> IFaceHolder<I> wrapSyncClient(
					Class<I> svIf, ThriftClientFactory.ClientHolder<TClusterService.Iface> ch, ClassLoaderFetcher clFetcher) {
		return new IFaceHolder<>(
						(I) Proxy.newProxyInstance(svIf.getClassLoader(), new Class[]{svIf},
										(obj, method, args) -> {
											ByteBuffer result = ch.getClient().invokeSvMethod(method.getName(), serialize(args));
											Serializable sr = unSerialize(result, clFetcher);
											if (sr instanceof Throwable) throw (Throwable) sr;
											else return sr;
										}),
						ch);
	}

	private static ByteBuffer wrapExp(Exception e) {
		return serialize(e);
	}

	private static ByteBuffer serialize(Serializable obj) {
		if (obj == null) return null;

		try {
			return ByteBuffer.wrap(s.serialize(obj));
		} catch (Exception e) {
			log.error("serialize obj error:" + obj, e);
			throw new RuntimeException(e);
		}
	}

	private static <T extends Serializable> T unSerialize(ByteBuffer buf, ClassLoaderFetcher clFetcher) {
		if (buf == null) return null;

		try {
			return s.unSerialize(buf.array(), buf.position(), buf.limit() - buf.position(), clFetcher);
		} catch (Exception e) {
			byte[] b = new byte[buf.capacity() - buf.position()];
			System.arraycopy(buf.array(), buf.position(), b, 0, b.length);
			log.error("unSerialize obj error, byte=" + Base64.getEncoder().encodeToString(b), e);
			throw new RuntimeException(e);
		}
	}

}
