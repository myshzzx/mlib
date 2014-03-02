package edu.wlu.cs.levy.CG;

/**
 * @author Mysh
 * @since 14-1-17 下午3:36
 */
public interface NnbrStore<T> {
	boolean isCapacityReached();

	float getMaxPriority();

	boolean insert(T node, float dist_sqd);
}
