package mysh.net.httpclient;

import okhttp3.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Objects;

/**
 * @since 2019/6/16
 */
public class EventListenerAdapter extends EventListener {
	private List<EventListener> listeners;

	public EventListenerAdapter(List<EventListener> listeners) {
		Objects.requireNonNull(listeners, "listeners is null");
		this.listeners = listeners;
	}
	
	public void callStart(Call call) {
		this.listeners.forEach(l->l.callStart(call));
	}

	public void dnsStart(Call call, String domainName) {
		this.listeners.forEach(l->l.dnsStart(call, domainName));
	}

	public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
		this.listeners.forEach(l->l.dnsEnd(call, domainName, inetAddressList));
	}

	public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
		this.listeners.forEach(l->l.connectStart(call, inetSocketAddress, proxy));
	}

	public void secureConnectStart(Call call) {
		this.listeners.forEach(l->l.secureConnectStart(call));
	}

	public void secureConnectEnd(Call call, @Nullable Handshake handshake) {
		this.listeners.forEach(l->l.secureConnectEnd(call, handshake));
	}

	public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, @Nullable Protocol protocol) {
		this.listeners.forEach(l->l.connectEnd(call, inetSocketAddress, proxy, protocol));
	}

	public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, @Nullable Protocol protocol, IOException ioe) {
		this.listeners.forEach(l->l.connectFailed(call, inetSocketAddress, proxy, protocol, ioe));
	}

	public void connectionAcquired(Call call, Connection connection) {
		this.listeners.forEach(l->l.connectionAcquired(call, connection));
	}

	public void connectionReleased(Call call, Connection connection) {
		this.listeners.forEach(l->l.connectionReleased(call, connection));
	}

	public void requestHeadersStart(Call call) {
		this.listeners.forEach(l->l.requestHeadersStart(call));
	}

	public void requestHeadersEnd(Call call, Request request) {
		this.listeners.forEach(l->l.requestHeadersEnd(call, request));
	}

	public void requestBodyStart(Call call) {
		this.listeners.forEach(l->l.requestBodyStart(call));
	}

	public void requestBodyEnd(Call call, long byteCount) {
		this.listeners.forEach(l->l.requestBodyEnd(call, byteCount));
	}

	public void requestFailed(Call call, IOException ioe) {
		this.listeners.forEach(l->l.requestFailed(call, ioe));
	}

	public void responseHeadersStart(Call call) {
		this.listeners.forEach(l->l.responseHeadersStart(call));
	}

	public void responseHeadersEnd(Call call, Response response) {
		this.listeners.forEach(l->l.responseHeadersEnd(call, response));
	}

	public void responseBodyStart(Call call) {
		this.listeners.forEach(l->l.responseBodyStart(call));
	}

	public void responseBodyEnd(Call call, long byteCount) {
		this.listeners.forEach(l->l.responseBodyEnd(call, byteCount));
	}

	public void responseFailed(Call call, IOException ioe) {
		this.listeners.forEach(l->l.responseFailed(call, ioe));
	}

	public void callEnd(Call call) {
		this.listeners.forEach(l->l.callEnd(call));
	}

	public void callFailed(Call call, IOException ioe) {
		this.listeners.forEach(l->l.callFailed(call, ioe));
	}

}
