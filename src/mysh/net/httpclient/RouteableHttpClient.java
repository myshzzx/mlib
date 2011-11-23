
package mysh.net.httpclient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.client.AuthenticationHandler;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

/**
 * 由于 DefaultHttpClient 可以自动处理页面跳转 ( 302 304 ), 为取得当前页面的真实地址, 实现此类.
 * 
 * @author ZhangZhx
 * @see RouteableRequestDirector
 * @see RouteableHttpResponse
 */
public class RouteableHttpClient extends DefaultHttpClient {

	private final Log log = LogFactory.getLog(getClass());

	protected RequestDirector createClientRequestDirector(final HttpRequestExecutor requestExec,
			final ClientConnectionManager conman, final ConnectionReuseStrategy reustrat,
			final ConnectionKeepAliveStrategy kastrat, final HttpRoutePlanner rouplan,
			final HttpProcessor httpProcessor, final HttpRequestRetryHandler retryHandler,
			final RedirectStrategy redirectStrategy,
			final AuthenticationHandler targetAuthHandler,
			final AuthenticationHandler proxyAuthHandler,
			final UserTokenHandler stateHandler, final HttpParams params) {

		return new RouteableRequestDirector(log, requestExec, conman, reustrat, kastrat,
				rouplan, httpProcessor, retryHandler, redirectStrategy,
				targetAuthHandler, proxyAuthHandler, stateHandler, params);
	}

}
