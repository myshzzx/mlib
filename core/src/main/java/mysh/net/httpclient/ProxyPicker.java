package mysh.net.httpclient;

import java.net.Proxy;

/**
 * ProxyPicker
 *
 * @author mysh
 * @since 2016/2/17
 */
public interface ProxyPicker {
	/**
	 * pick a proxy. <code>null</code> refer to direct connect.
	 */
	Proxy pick();
}
