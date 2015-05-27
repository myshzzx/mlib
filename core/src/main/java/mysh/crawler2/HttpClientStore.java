package mysh.crawler2;

import mysh.net.httpclient.HttpClientConfig;

public abstract class HttpClientStore {
	/**
	 * get config from url, the config should be reused.
	 */
	public abstract HttpClientConfig getClientConfig(String url);

	public static HttpClientStore of(HttpClientConfig hcc) {
		return new HttpClientStore() {
			@Override
			public HttpClientConfig getClientConfig(String url) {
				return hcc;
			}
		};
	}

	/**
	 * if the config returned by {@link #getClientConfig(String)} is revoked and the related client
	 * should be closed, invoke this.
	 */
	public void configRevoked(HttpClientConfig hcc) {
		if (hcc != null && listener != null)
			listener.configRevoked(hcc);
	}

	volatile Listener listener;

	interface Listener {
		void configRevoked(HttpClientConfig hcc);
	}
}
