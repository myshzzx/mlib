
package mysh.jpipe;

/**
 * 推送器的状态控制器.<br/>
 * 实现不需要是线程安全的, 因为插件是顺序执行的.
 * 
 * @author ZhangZhx
 * 
 */
public interface PusherStateControler {

	/**
	 * 取数据缓冲区本身.
	 * 
	 * @return
	 */
	byte[] getDataBuf();

	/**
	 * 取有效数据长度.
	 * 
	 * @return
	 */
	int getAvailableDataLength();

	/**
	 * 设置一个新的数据缓冲区.<br/>
	 * 注意: 缓冲区太小会影响其他插件的数据处理.
	 * 
	 * @param buf
	 */
	void setDataBuf(byte[] buf);

	/**
	 * 设置数据缓冲区有效数据长度.
	 * 
	 * @param length
	 */
	void setDataLength(int length);
}