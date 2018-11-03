package mysh.net.httpclient;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HttpClientCookieStore
 *
 * @author mysh
 * @since 2018/11/3
 */
@ThreadSafe
public class HttpClientCookieStore {

	public String getCookie(String host) {
		List<Cookie> cookies = cookieJar.loadForRequest(HttpUrl.get("http://" + host));
		StringBuilder sb = new StringBuilder();
		for (Cookie cookie : cookies) {
			sb.append(cookie.name()).append('=').append(cookie.value()).append("; ");
		}
		return sb.toString();
	}

	/**
	 * @param cookiesStr such as "a=b; c=d"
	 */
	public HttpClientCookieStore putCookie(String host, String cookiesStr) {
		String[] csa = cookiesStr.split("; ?");
		List<Cookie> cookies = new ArrayList<>();
		for (String cs : csa) {
			String[] ca = cs.split("=", 2);
			cookies.add(new Cookie.Builder().name(ca[0]).value(ca[1]).domain(host).build());
		}
		cookieJar.saveFromResponse(HttpUrl.get("http://" + host), cookies);
		return this;
	}

	public CookieJar getCookieJar() {
		return cookieJar;
	}

	private CookieJar cookieJar = new CookieJar() {

		Map<String, Map<String, Cookie>> cs = new ConcurrentHashMap<>();

		@Override
		public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
			for (Cookie cookie : cookies) {
				cs.computeIfAbsent(cookie.domain(), d -> new ConcurrentHashMap<>())
						.put(cookie.name(), cookie);
			}
		}

		@Override
		public synchronized List<Cookie> loadForRequest(HttpUrl url) {
			ArrayList<Cookie> cookies = new ArrayList<>();
			String host = url.host();
			int limit = host.lastIndexOf('.');
			int idx = 0;
			while (idx < limit) {
				Map<String, Cookie> domainCookies = cs.getOrDefault(host.substring(idx), Collections.emptyMap());
				cookies.addAll(domainCookies.values());
				idx = host.indexOf('.', idx + 1) + 1;
				if (idx == 0 || idx >= limit) {
					break;
				}
			}
			return cookies;
		}
	};

}
