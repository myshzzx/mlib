package mysh.net.httpclient;

import org.junit.Ignore;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * CommonTest
 *
 * @author mysh
 * @since 2016/1/9
 */
@Ignore
public class ProxyTest {

	@Test
	public void hca() throws Exception {
//		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("115.160.137.178", 8088));
		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("192.99.54.41", 3128));

		HttpClientAssist hca = new HttpClientAssist(null, ProxySelectors.single(proxy));

		String plainUrl = "http://img1.cache.netease.com/cnews/css13/img/logo_ent.png";
		String sslUrl = "https://ss0.bdstatic.com/5aV1bjqh_Q23odCf/static/superman/img/blank_56a92bd4.png";
		try (HttpClientAssist.UrlEntity ue = hca.access(plainUrl)) {
			System.out.println(ue.getEntityStr());
		}
		System.out.println("=============================");
		try (HttpClientAssist.UrlEntity ue = hca.access(sslUrl)) {
			System.out.println(ue.getEntityStr());
		}
	}

}
