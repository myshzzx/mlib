
package mysh.jpipe;

/**
 * 插件工厂. <br/>
 * 每次需要取得新插件实例时, 通过工厂的方法生成.<br/>
 * 这种机制可用来实现单实例插件, 只是需要保证实现是线程安全的.
 *
 * @author ZhangZhx
 */
public interface PluginFactory {

	/**
	 * 生成新插件实例的工厂方法. <br/>
	 * 管道组件初始化插件时, 会用反射的方式生成一个插件实例, 将此实例用来作为插件生成工厂, <br/>
	 * 当一个新的管道建立时, 此方法将被调用以取得一个新插件实例, <br/>
	 * 这种机制用来实现单插件实例, 需要注意的是单插件实例需要保证线程安全性.
	 *
	 * @param localHost  本地主机
	 * @param localPort  本地端口
	 * @param remoteHost 远程主机
	 * @param remotePort 远程端口
	 */
	Plugin buildNewPluginInstance(String localHost, int localPort, String remoteHost,
	                              int remotePort);

}
