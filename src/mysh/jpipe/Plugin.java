
package mysh.jpipe;

/**
 * 插件. <br/>
 * 一个管道对应本地和远程两个推送器, 这两个对应同一个插件实例.<br/>
 * 插件可以更改推送器获取到的, 还未推送出去的数据, <br/>
 * 由于插件列表中的插件目前在推送器中是顺序执行的, 因而多个插件对推送器的状态修改是同步的.<br/>
 * 插件的两个方法 handleLocalData 和 handleRemoteData 是异步的, 因为它们由两个不同的推送器调用. <br/>
 * 若插件是单例的, 那么插件的实现需要是线程安全的.
 * 
 * @author ZhangZhx
 * 
 */
public interface Plugin {

	/**
	 * 处理本地推送器数据. <br/>
	 * 当缓冲区被填满时, 或收到一个完整的 TCP 包但未填满缓冲区时, 由本地推送器调用.
	 * 
	 * @param controler
	 *               推送器的状态控制器
	 */
	void handleLocalData(PusherStateControler controler);

	/**
	 * 处理远程推送器数据. <br/>
	 * 当缓冲区被填满时, 或收到一个完整的 TCP 包但未填满缓冲区时, 由远程推送器调用.
	 * 
	 * @param controler
	 *               推送器的状态控制器
	 */
	void handleRemoteData(PusherStateControler controler);

}
