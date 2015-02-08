
package mysh.jpipe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PluginsGenerator {
	private static final Logger log = LoggerFactory.getLogger(PluginsGenerator.class);

	/**
	 * Guarded by PluginsGenerator.class instance.<br/>
	 * 用于生成插件实例的类实体.
	 */
	private static final List<PluginFactory> pluginFactories = new ArrayList<>();

	/**
	 * 初始化插件工厂实例.
	 */
	public static synchronized void initPluginFactories(String[] pluginFactoryClassNames) {

		pluginFactories.clear();

		if (pluginFactoryClassNames != null && pluginFactoryClassNames.length > 0) {
			for (String pluginFactoryClassName : pluginFactoryClassNames) {
				if (pluginFactoryClassName != null && pluginFactoryClassName.trim().length() > 0) {
					try {
						pluginFactories.add((PluginFactory) Class.forName(pluginFactoryClassName).newInstance());
					} catch (Exception e) {
						log.error("加载插件失败: " + pluginFactoryClassName, e);
					}
				}
			}
		}
	}

	/**
	 * 用已初始化的插件工厂生成新的插件实例.<br/>
	 * 这个方法调用发生在每次连接请求建立时, 因而用内部锁同步不会影响效率.
	 *
	 * @param localSock  本地连接套接字
	 * @param remoteSock 远程连接套接字
	 */
	public static synchronized List<Plugin> generatePluginsInstance(Socket localSock,
	                                                                Socket remoteSock) {

		List<Plugin> plugins = new ArrayList<>(pluginFactories.size());

		Plugin newPlugin;
		for (PluginFactory pluginFactory : pluginFactories) {
			try {
				newPlugin = pluginFactory.buildNewPluginInstance(
								localSock.getLocalAddress().getHostAddress(),
								localSock.getLocalPort(),
								remoteSock.getInetAddress().getHostAddress(),
								remoteSock.getPort());
				plugins.add(newPlugin);
			} catch (Exception e) {
				log.error("实例化插件失败: " + pluginFactory.getClass().getName(), e);
			}
		}

		return Collections.unmodifiableList(plugins);
	}
}
