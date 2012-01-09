
package mysh.net.httpclient;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.AuthenticationHandler;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.DefaultRequestDirector;
import org.apache.http.impl.client.RoutedRequest;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

/**
 * 配合 RouteableHttpClient 以取得当前页面的真实地址.
 * 
 * @author ZhangZhx
 * @see RouteableHttpClient
 * @see RouteableHttpResponse
 */
public class RouteableRequestDirector extends DefaultRequestDirector {

	private volatile String currentURL;

	public RouteableRequestDirector(final Log log, final HttpRequestExecutor requestExec,
			final ClientConnectionManager conman, final ConnectionReuseStrategy reustrat,
			final ConnectionKeepAliveStrategy kastrat, final HttpRoutePlanner rouplan,
			final HttpProcessor httpProcessor, final HttpRequestRetryHandler retryHandler,
			final RedirectStrategy redirectStrategy,
			final AuthenticationHandler targetAuthHandler,
			final AuthenticationHandler proxyAuthHandler,
			final UserTokenHandler userTokenHandler, final HttpParams params) {

		super(log, requestExec, conman, reustrat, kastrat, rouplan, httpProcessor,
				retryHandler, redirectStrategy, targetAuthHandler, proxyAuthHandler,
				userTokenHandler, params);
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
		RouteableHttpResponse rResp = new RouteableHttpResponse(resp);
		rResp.setCurrentURL(this.currentURL);
		return rResp;
	}

}
