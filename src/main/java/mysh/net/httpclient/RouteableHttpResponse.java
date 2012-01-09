
package mysh.net.httpclient;

import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.params.HttpParams;

/**
 * 配合 RouteableRequestDirector. 可通过此类的实例取得当前访问的页面的真实地址.
 * 
 * @author ZhangZhx
 * @see RouteableHttpClient
 * @see RouteableRequestDirector
 */
public class RouteableHttpResponse implements HttpResponse {

	private final HttpResponse resp;

	private volatile String currentURL;

	public RouteableHttpResponse(HttpResponse resp) {

		this.resp = resp;
	}

	/**
	 * 取当前地址.
	 * 
	 * @return
	 */
	public String getCurrentURL() {

		return currentURL;
	}

	/**
	 * 设置当前地址.
	 * 
	 * @param currentURL
	 */
	public void setCurrentURL(String currentURL) {

		this.currentURL = currentURL;
	}

	// 下面都是代理方法

	@Override
	public ProtocolVersion getProtocolVersion() {

		return this.resp.getProtocolVersion();
	}

	@Override
	public boolean containsHeader(String name) {

		return this.resp.containsHeader(name);
	}

	@Override
	public Header[] getHeaders(String name) {

		return this.resp.getHeaders(name);
	}

	@Override
	public Header getFirstHeader(String name) {

		return this.resp.getFirstHeader(name);
	}

	@Override
	public Header getLastHeader(String name) {

		return this.resp.getLastHeader(name);
	}

	@Override
	public Header[] getAllHeaders() {

		return this.resp.getAllHeaders();
	}

	@Override
	public void addHeader(Header header) {

		this.resp.addHeader(header);
	}

	@Override
	public void addHeader(String name, String value) {

		this.resp.addHeader(name, value);

	}

	@Override
	public void setHeader(Header header) {

		this.resp.setHeader(header);

	}

	@Override
	public void setHeader(String name, String value) {

		this.resp.setHeader(name, value);
	}

	@Override
	public void setHeaders(Header[] headers) {

		this.resp.setHeaders(headers);
	}

	@Override
	public void removeHeader(Header header) {

		this.resp.removeHeader(header);

	}

	@Override
	public void removeHeaders(String name) {

		this.resp.removeHeaders(name);

	}

	@Override
	public HeaderIterator headerIterator() {

		return this.resp.headerIterator();
	}

	@Override
	public HeaderIterator headerIterator(String name) {

		return this.resp.headerIterator(name);
	}

	@Override
	public HttpParams getParams() {

		return this.resp.getParams();
	}

	@Override
	public void setParams(HttpParams params) {

		this.resp.setParams(params);

	}

	@Override
	public StatusLine getStatusLine() {

		return this.resp.getStatusLine();
	}

	@Override
	public void setStatusLine(StatusLine statusline) {

		this.resp.setStatusLine(statusline);

	}

	@Override
	public void setStatusLine(ProtocolVersion ver, int code) {

		this.resp.setStatusLine(ver, code);

	}

	@Override
	public void setStatusLine(ProtocolVersion ver, int code, String reason) {

		this.resp.setStatusLine(ver, code, reason);

	}

	@Override
	public void setStatusCode(int code) throws IllegalStateException {

		this.resp.setStatusCode(code);

	}

	@Override
	public void setReasonPhrase(String reason) throws IllegalStateException {

		this.resp.setReasonPhrase(reason);

	}

	@Override
	public HttpEntity getEntity() {

		return this.resp.getEntity();
	}

	@Override
	public void setEntity(HttpEntity entity) {

		this.resp.setEntity(entity);

	}

	@Override
	public Locale getLocale() {

		return this.resp.getLocale();
	}

	@Override
	public void setLocale(Locale loc) {

		this.resp.setLocale(loc);

	}

}
