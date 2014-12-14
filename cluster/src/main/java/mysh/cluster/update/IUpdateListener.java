package mysh.cluster.update;

/**
 * @author Mysh
 * @since 2014/12/7 21:06
 */
public interface IUpdateListener {

	/**
	 * notify possible files update
	 *
	 * @param dispatcherId dispatcher id
	 * @param thumbStamp   files thumbStamp
	 */
	void updateFiles(String dispatcherId, String thumbStamp);

}
