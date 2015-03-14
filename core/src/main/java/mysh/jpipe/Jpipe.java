
package mysh.jpipe;

import java.io.Closeable;
import java.io.IOException;

public class Jpipe implements Closeable {

	private int listeningPort;
	private String targetHost;
	private int targetPort;
	private String pluginFactoryClassNames;
	private Listener listener;

	public Jpipe(int localPort, String remoteHost, int remotePort, String factClassName) {
		this.listeningPort = localPort;
		this.targetHost = remoteHost;
		this.targetPort = remotePort;
		this.pluginFactoryClassNames = factClassName;
	}

	public void startJpipe() throws IOException {
		PluginsGenerator.initPluginFactories(this.pluginFactoryClassNames.split("[\\s,;]"));
		listener = new Listener(this.listeningPort, this.targetHost, this.targetPort);
		listener.start();
	}

	@Override
	public void close() throws IOException {
		this.listener.close();
	}
}
