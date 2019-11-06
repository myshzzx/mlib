package mysh.net.httpclient;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * Dynamic select a proxy from proxy pool by historical connection state.
 * <p/>
 * typical case: with one or few proxies, some hosts can only be access from specified proxy,
 * a few access problems are acceptable. the purpose of DynamicProxySelector is to simplify
 * host->proxy config. for example, in China, some hosts are blocked permanently,
 * which can only be accessed via proxy.
 * <p/>
 * Add {@link Proxy#NO_PROXY} if DIRECT connection is available.
 * <p/>
 * to ensure successful access, try as many times as proxies count, use {@link mysh.util.Exps#retryOnExp}
 *
 * @see ProxySelectors
 * @since 2019/6/16
 */
public class DynamicProxySelector extends ProxySelector {
	private static final Logger log = LoggerFactory.getLogger(DynamicProxySelector.class);
	private static final long startSec = System.currentTimeMillis() / 1000;
	
	private static class PriorityProxy extends Proxy implements Comparable<PriorityProxy> {
		static final InetSocketAddress SA = InetSocketAddress.createUnresolved("0.0.0.0", 12345);
		
		volatile int priority;
		final Proxy p;
		
		PriorityProxy(Proxy p, int priority) {
			super(Type.SOCKS, SA);
			if (p == null)
				throw new IllegalArgumentException("proxy cann't be NULL");
			this.p = p;
			this.priority = priority;
		}
		
		@Override
		public Type type() {
			return p.type();
		}
		
		@Override
		public SocketAddress address() {
			return p.address();
		}
		
		@Override
		public int compareTo(PriorityProxy o) {
			return this.priority - o.priority;
		}
	}
	
	private volatile List<PriorityProxy> ps;
	private volatile Map<SocketAddress, PriorityProxy> saProxyMap;
	private LoadingCache<String, PriorityQueue<PriorityProxy>> hostProxyCache;
	
	public DynamicProxySelector() {
		this(2000, Collections.emptyList());
	}
	
	public DynamicProxySelector(List<Proxy> ps) {
		this(2000, ps);
	}
	
	public DynamicProxySelector(int maxHostCacheSize, List<Proxy> ps) {
		hostProxyCache = CacheBuilder
				.newBuilder()
				.maximumSize(maxHostCacheSize)
				.build(new CacheLoader<String, PriorityQueue<PriorityProxy>>() {
					@Override
					public PriorityQueue<PriorityProxy> load(String h) {
						return new PriorityQueue<>(DynamicProxySelector.this.ps);
					}
				});
		reset(ps);
	}
	
	public synchronized final void reset(List<Proxy> ps) {
		List<PriorityProxy> plst = new ArrayList<>();
		Map<SocketAddress, PriorityProxy> spMap = new HashMap<>();
		for (int i = 0; i < ps.size(); i++) {
			final Proxy p = ps.get(i);
			final PriorityProxy pp = new PriorityProxy(p, i);
			plst.add(pp);
			spMap.put(pp.address(), pp);
		}
		this.ps = plst;
		this.saProxyMap = spMap;
		this.hostProxyCache.cleanUp();
	}
	
	private static final List<Proxy> directProxy = Arrays.asList(Proxy.NO_PROXY);
	
	@Override
	public List<Proxy> select(URI uri) {
		PriorityQueue<PriorityProxy> proxies = cachedProxy(uri);
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (proxies) {
			final PriorityProxy pp = proxies.peek();
			if (pp == null)
				return directProxy;
			else
				return Collections.singletonList(pp);
		}
	}
	
	@Override
	public void connectFailed(URI uri, SocketAddress proxySa, IOException ioe) {
		PriorityQueue<PriorityProxy> proxies = cachedProxy(uri);
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (proxies) {
			final PriorityProxy pp = saProxyMap.get(proxySa);
			if (pp == null)
				return;
			pp.priority = (int) (System.currentTimeMillis() / 1000 - startSec);
			proxies.remove(pp);
			proxies.add(pp);
		}
	}
	
	private PriorityQueue<PriorityProxy> cachedProxy(URI uri) {
		try {
			final String host = uri.getHost();
			return hostProxyCache.get(host);
		} catch (Exception e) {
			log.error("that's impossible", e);
			return null;
		}
	}
}
