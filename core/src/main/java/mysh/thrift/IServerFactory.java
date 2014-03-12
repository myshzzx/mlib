package mysh.thrift;

import org.apache.thrift.server.TServer;

/**
 * Thrift 服务端工厂。
 *
 * @author Mysh
 * @since 13-7-17 下午2:28
 */
public interface IServerFactory {
	/**
	 * 构建并启动服务。
	 *
	 * @throws Exception
	 */
	TServer buildAndServe() throws Exception;
}
