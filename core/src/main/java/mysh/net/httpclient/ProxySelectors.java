package mysh.net.httpclient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

/**
 * proxy and selector facade.
 *
 * @since 2019/6/16
 */
public abstract class ProxySelectors {

	public static Proxy socks(String host, int port) {
		return new Proxy(Proxy.Type.SOCKS,
				new InetSocketAddress(host, port));
	}

	public static Proxy http(String host, int port) {
		return new Proxy(Proxy.Type.HTTP,
				new InetSocketAddress(host, port));
	}

	public static SingleProxySelector single(Proxy proxy) {
		return new SingleProxySelector(proxy);
	}

	public static SingleProxySelector singleSocks(String host, int port) {
		return new SingleProxySelector(socks(host, port));
	}

	public static SingleProxySelector singleHttp(String host, int port) {
		return new SingleProxySelector(http(host, port));
	}

	public static DynamicProxySelector dynamic() {
		return new DynamicProxySelector();
	}

	public static DynamicProxySelector dynamic(List<Proxy> ps) {
		return new DynamicProxySelector(ps);
	}

	public static DynamicProxySelector dynamic(int maxHostCacheSize, List<Proxy> ps) {
		return new DynamicProxySelector(maxHostCacheSize, ps);
	}
}
