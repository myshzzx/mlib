
package mysh.jpipe;

public class Jpipe {

	private int listeningPort;

	private String targetHost;

	private int targetPort;

	private String pluginFactoryClassNames = "";

	public void startJpipe() {

		PluginsGenerator.initPluginFactories(this.pluginFactoryClassNames.split("[\\s,;]"));
		new Listener(this.listeningPort, this.targetHost, this.targetPort).start();
	}

	public Jpipe setListeningPort(int listeningPort) {

		this.listeningPort = listeningPort;
		return this;
	}

	public Jpipe setTargetHost(String targetHost) {

		this.targetHost = targetHost;
		return this;
	}

	public Jpipe setTargetPort(int targetPort) {

		this.targetPort = targetPort;
		return this;
	}

	public Jpipe setPluginFactoryClassNames(String pluginFactoryClassNames) {

		this.pluginFactoryClassNames = pluginFactoryClassNames;
		return this;
	}

}
