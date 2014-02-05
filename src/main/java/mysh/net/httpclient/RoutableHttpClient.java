
package mysh.net.httpclient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRequestDirector;
import org.apache.http.impl.client.RoutedRequest;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URI;

/**
 * 由于 DefaultHttpClient 可以自动处理页面跳转 ( 302 304 ), 为取得当前页面的真实地址, 实现此类.
 *
 * @author ZhangZhx
 * @see mysh.net.httpclient.RoutableHttpClient.RoutableRequestDirector
 * @see RoutableHttpClient.RoutableHttpResponse
 */
public class RoutableHttpClient extends DefaultHttpClient {

	private final Log log = LogFactory.getLog(getClass());

	protected RequestDirector createClientRequestDirector(
					final HttpRequestExecutor requestExec,
					final ClientConnectionManager conman,
					final ConnectionReuseStrategy reustrat,
					final ConnectionKeepAliveStrategy kastrat,
					final HttpRoutePlanner rouplan,
					final HttpProcessor httpProcessor,
					final HttpRequestRetryHandler retryHandler,
					final RedirectStrategy redirectStrategy,
					final AuthenticationStrategy targetAuthStrategy,
					final AuthenticationStrategy proxyAuthStrategy,
					final UserTokenHandler userTokenHandler,
					final HttpParams params) {

		return new RoutableRequestDirector(log,
						requestExec,
						conman,
						reustrat,
						kastrat,
						rouplan,
						httpProcessor,
						retryHandler,
						redirectStrategy,
						targetAuthStrategy,
						proxyAuthStrategy,
						userTokenHandler,
						params);
	}


	/**
	 * 配合 RoutableHttpClient 以取得当前页面的真实地址.
	 *
	 * @author ZhangZhx
	 * @see RoutableHttpClient
	 * @see RoutableHttpClient.RoutableHttpResponse
	 */
	public static class RoutableRequestDirector extends DefaultRequestDirector {

		private volatile String currentURL;

		public RoutableRequestDirector(
						final Log log,
						final HttpRequestExecutor requestExec,
						final ClientConnectionManager conman,
						final ConnectionReuseStrategy reustrat,
						final ConnectionKeepAliveStrategy kastrat,
						final HttpRoutePlanner rouplan,
						final HttpProcessor httpProcessor,
						final HttpRequestRetryHandler retryHandler,
						final RedirectStrategy redirectStrategy,
						final AuthenticationStrategy targetAuthStrategy,
						final AuthenticationStrategy proxyAuthStrategy,
						final UserTokenHandler userTokenHandler,
						final HttpParams params) {

			super(log,
							requestExec,
							conman,
							reustrat,
							kastrat,
							rouplan,
							httpProcessor,
							retryHandler,
							redirectStrategy,
							targetAuthStrategy,
							proxyAuthStrategy,
							userTokenHandler,
							params);
		}

		protected RoutedRequest handleResponse(RoutedRequest roureq, HttpResponse response,
		                                       HttpContext context) throws HttpException, IOException {

			RoutedRequest routedRequest = super.handleResponse(roureq, response, context);

			if (routedRequest == null) {
				URI uri = ((HttpGet) roureq.getRequest().getOriginal()).getURI();

				this.currentURL = HttpClientAssist.getShortURL(uri.toString());

			}

			return routedRequest;
		}

		public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context)
						throws HttpException, IOException {

			HttpResponse resp = super.execute(target, request, context);
			return (RoutableHttpResponse) Proxy.newProxyInstance(
							RoutableHttpClient.class.getClassLoader(),
							new Class[]{RoutableHttpResponse.class},
							(proxy, method, args) -> {
								switch (method.getName()) {
									case "getCurrentURL":
										return currentURL;
									default:
										return method.invoke(resp, args);
								}
							});
		}

	}

	/**
	 * 配合 RoutableRequestDirector. 可通过此类的实例取得当前访问的页面的真实地址.
	 *
	 * @author ZhangZhx
	 * @see RoutableHttpClient
	 * @see mysh.net.httpclient.RoutableHttpClient.RoutableRequestDirector
	 */
	public static interface RoutableHttpResponse extends HttpResponse {

		/**
		 * 取当前地址.
		 *
		 */
		String getCurrentURL();
	}
}
