package edu.wlu.cs.levy.CG;

/**
 * @author Mysh
 * @since 2014/9/21 20:52
 */
public class UoDist<UO> {
	public KDNode<UO> node;
	public float dist;

	public UoDist(KDNode<UO> node, float dist) {
		this.node = node;
		this.dist = dist;
	}
}
