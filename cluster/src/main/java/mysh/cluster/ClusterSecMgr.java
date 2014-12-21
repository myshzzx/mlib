package mysh.cluster;

import sun.security.util.SecurityConstants;

/**
 * security manager.
 *
 * @author Mysh
 * @since 2014/12/18 15:44
 */
public final class ClusterSecMgr extends SecurityManager {
	/**
	 * forbid thread creation from user libs.
	 */
	public void checkAccess(ThreadGroup g) {
		super.checkAccess(g);
		checkPermission(SecurityConstants.MODIFY_THREADGROUP_PERMISSION);
	}

	/**
	 * forbid thread modification from user libs.
	 */
	@Override
	public void checkAccess(Thread t) {
		super.checkAccess(t);
		checkPermission(SecurityConstants.MODIFY_THREAD_PERMISSION);
	}
}
