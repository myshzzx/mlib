package mysh.cluster;

/**
 * security manager.
 *
 * @author Mysh
 * @see sun.security.util.SecurityConstants
 * @since 2014/12/18 15:44
 */
public final class ClusterSecMgr extends SecurityManager {
	
	public static final RuntimePermission MODIFY_THREADGROUP_PERMISSION =
			new RuntimePermission("modifyThreadGroup");
	
	public static final RuntimePermission MODIFY_THREAD_PERMISSION =
			new RuntimePermission("modifyThread");
	
	/**
	 * forbid thread creation from user libs.
	 */
	public void checkAccess(ThreadGroup g) {
		super.checkAccess(g);
		checkPermission(MODIFY_THREADGROUP_PERMISSION);
	}
	
	/**
	 * forbid thread modification from user libs.
	 */
	@Override
	public void checkAccess(Thread t) {
		super.checkAccess(t);
		checkPermission(MODIFY_THREAD_PERMISSION);
	}
}
