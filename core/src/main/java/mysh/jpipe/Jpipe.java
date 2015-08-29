
package mysh.jpipe;

import java.io.Closeable;
import java.io.IOException;

public class Jpipe implements Closeable {

	private final int listeningPort;
	private final String targetHost;
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
	public void close() throws IOException {
		this.listener.close();
	}
}
