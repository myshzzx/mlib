package mysh.thrift;


import org.apache.commons.pool2.PooledObjectFactory;

/**
 * Thrift 客户端工厂。
 *
 * @author Mysh
 * @since 13-7-17 下午2:28
 */
public interface IClientFactory<T> {
	T build();

	void setPoolObjFactory(PooledObjectFactory<T> poolObjFactory);
}
