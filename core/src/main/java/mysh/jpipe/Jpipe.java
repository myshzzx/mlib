
package mysh.jpipe;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;

@Slf4j
public class Jpipe implements Closeable {
	
	@Getter
	private final int listeningPort;
	@Getter
	private final String targetHost;
	@Getter
	private final int targetPort;
	private final int bufLen;
	private final String pluginFactoryClassNames;
	private Listener listener;
	
	public Jpipe(int localPort, String remoteHost, int remotePort, int bufLen, String factClassName) {
		this.listeningPort = localPort;
		this.targetHost = remoteHost;
		this.targetPort = remotePort;
		this.bufLen = bufLen;
		this.pluginFactoryClassNames = factClassName;
	}
	
	public void startJpipe() throws IOException {
		PluginsGenerator.initPluginFactories(this.pluginFactoryClassNames.split("[\\s,;]"));
		listener = new Listener(this.listeningPort, this.targetHost, this.targetPort, this.bufLen);
		listener.start();
	}
	
	@Override
	public void close() {
		try {
			this.listener.close();
		} catch (IOException e) {
		} catch (Exception e) {
			log.error("closing-jpipe-listener-fail", e);
		}
	}
}
