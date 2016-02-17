package mysh.net.httpclient;

import org.junit.Ignore;
import org.junit.Test;

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
		HttpClientConfig hcc = new HttpClientConfig();
		hcc.setUseProxy(true);

		hcc.setProxyHost("192.99.54.41");
		hcc.setProxyPort(3128);
		hcc.setProxyType(Proxy.Type.HTTP);

		HttpClientAssist hca = new HttpClientAssist(hcc);

		String plainUrl = "http://img1.cache.netease.com/cnews/css13/img/logo_ent.png";
		String sslUrl = "https://ss0.bdstatic.com/5aV1bjqh_Q23odCf/static/superman/img/logo/bd_logo1_31bdc765.png";
		try (HttpClientAssist.UrlEntity ue = hca.access(plainUrl)) {
			System.out.println(ue.getEntityStr());
		}
		System.out.println("=============================");
		try (HttpClientAssist.UrlEntity ue = hca.access(sslUrl)) {
			System.out.println(ue.getEntityStr());
		}
	}

}
