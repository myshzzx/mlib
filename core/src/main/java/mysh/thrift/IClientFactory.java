package mysh.thrift;

import org.apache.commons.pool.PoolableObjectFactory;

/**
 * Thrift 客户端工厂。
 *
 * @author Mysh
 * @since 13-7-17 下午2:28
 */
public interface IClientFactory<T> {
	T build();

	void setPoolObjFactory(PoolableObjectFactory<T> poolObjFactory);
}
