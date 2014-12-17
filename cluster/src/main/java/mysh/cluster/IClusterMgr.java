package mysh.cluster;

/**
 * cluster user for manage purpose.
 * master is accessible in fork and join, and worker is accessible in procSubTask.
 * <p>
 * WARNING: keep this class package-private to prevent subclass from user libs,
 * because user class loaded by different class loader can't access this class, even if they are
 * in the package with the same name. classes loaded by different class loader is not equal even
 * if they have the same name, so as package.
 *
 * @author Mysh
 * @since 2014/10/11 21:57
 */
abstract class IClusterMgr<T, ST, SR, R> extends IClusterUser<T, ST, SR, R> {

	private static final long serialVersionUID = -3160966940111585903L;

	protected transient Master master;
	protected transient Worker worker;

}
