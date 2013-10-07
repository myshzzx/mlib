
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
 * @see RouteableRequestDirector
 * @see RouteableHttpResponse
 */
public class RouteableHttpClient extends DefaultHttpClient {

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

		return new RouteableRequestDirector(log,
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
	 * 配合 RouteableHttpClient 以取得当前页面的真实地址.
	 *
	 * @author ZhangZhx
	 * @see RouteableHttpClient
	 * @see RouteableHttpResponse
	 */
	public static class RouteableRequestDirector extends DefaultRequestDirector {

		private volatile String currentURL;

		public RouteableRequestDirector(
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

				this.currentURL = HttpClientAssistor.getShortURL(uri.toString());

			}

			return routedRequest;
		}

		public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context)
						throws HttpException, IOException {

			HttpResponse resp = super.execute(target, request, context);
			return (RouteableHttpResponse) Proxy.newProxyInstance(
							RouteableHttpClient.class.getClassLoader(),
							new Class[]{RouteableHttpResponse.class},
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
	 * 配合 RouteableRequestDirector. 可通过此类的实例取得当前访问的页面的真实地址.
	 *
	 * @author ZhangZhx
	 * @see RouteableHttpClient
	 * @see RouteableHttpClient.RouteableRequestDirector
	 */
	public static interface RouteableHttpResponse extends HttpResponse {

		/**
		 * 取当前地址.
		 *
		 * @return
		 */
		String getCurrentURL();
	}
}
