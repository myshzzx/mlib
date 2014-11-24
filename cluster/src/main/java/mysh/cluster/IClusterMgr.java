package mysh.cluster;

/**
 * cluster user for manage purpose.
 * master is accessible in fork and join, and worker is accessible in procSubTask.
 *
 * @author Mysh
 * @since 2014/10/11 21:57
 */
public abstract class IClusterMgr<T, ST, SR, R> extends IClusterUser<T, ST, SR, R> {

	private static final long serialVersionUID = -3160966940111585903L;

	protected transient IMaster master;
	protected transient IWorker worker;

}
