package mysh.net.httpclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * SingleProxySelector
 *
 * @author mysh
 * @since 2016/2/20
 */
public class SingleProxySelector extends ProxySelector {
	private static final Logger log = LoggerFactory.getLogger(SingleProxySelector.class);
	public static final List<Proxy> NoProxies = Collections.singletonList(Proxy.NO_PROXY);

	private final List<Proxy> ps;

	public SingleProxySelector(Proxy p) {
		ps = p == null ? NoProxies : Collections.singletonList(p);
	}

	@Override
	public List<Proxy> select(URI uri) {
		return ps;
	}

	@Override
	public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
		Proxy proxy = ps.get(0);
		if (proxy != Proxy.NO_PROXY)
			log.error("proxy {} broken when access {}", proxy, uri, ioe);
	}

	public static ProxySelector of(Proxy proxy) {
		return new SingleProxySelector(proxy);
	}
}
