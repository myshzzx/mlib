
package mysh.jpipe;

/**
 * 推送器的状态控制器.<br/>
 * 实现不需要是线程安全的, 因为插件是顺序执行的.
 *
 * @author ZhangZhx
 */
public interface PusherStateController {

	/**
	 * 取数据缓冲区本身.
	 */
	byte[] getDataBuf();

	/**
	 * 取有效数据长度.
	 */
	int getAvailableDataLength();
}
