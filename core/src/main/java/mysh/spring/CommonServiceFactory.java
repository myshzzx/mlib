
package mysh.spring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务工厂。
 *
 * @author 张智贤
 * @version 2012-8-21
 */
public abstract class CommonServiceFactory {

	/**
	 * 服务 Map。
	 */
	private static final Map<String, Object> serviceMap = new ConcurrentHashMap<String, Object>();

	/**
	 * 从缓存中取给定服务名的服务。
	 * 注意：此方法仅用于单例服务。
	 *
	 * @param serviceName 服务名
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getBufferedService(String serviceName) {

		T service = (T) serviceMap.get(serviceName);
		if (service == null) {
			service = (T) CtxAware.getCtx().getBean(serviceName);
			serviceMap.put(serviceName, service);
		}
		return service;
	}

}
