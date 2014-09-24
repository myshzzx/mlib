package edu.wlu.cs.levy.CG;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

// K-D Tree node class

public class KDNode<UO> implements Serializable {
	private static final Logger log = LoggerFactory.getLogger(KDNode.class);
	private static final long serialVersionUID = 4305509566763270255L;

	// these are seen by KDTree
	protected float[] k;
	public UO v;
	protected KDNode<UO> left, right;
	protected boolean deleted;

	// constructor is used only by class; other methods are static
	KDNode(float[] key, UO val) {

		k = key;
		v = val;
		left = null;
		right = null;
		deleted = false;
	}

	// Method ins translated from 352.ins.c of Gonnet & Baeza-Yates
	protected static <UO> int insert(
					float[] key, UO value, KDNode<UO> target, int lev, int K) {
		KDNode<UO> next_node;
		int next_lev = (lev + 1) % K;
		if (checkEquals(key, target.k, K)) {
			boolean was_deleted = target.deleted;
			if (was_deleted) {
				target.v = value;
				target.deleted = false;
			} else {
				if (target.v.equals(value))
					log.debug("duplicated feature in same uo found, uo:" + value);
				else
					log.error("duplicated feature found, old uo:" + target.v + ", new uo:" + value);
			}
			return was_deleted ? 1 : 0;
		} else if (key[lev] > target.k[lev]) {
			next_node = target.right;
			if (next_node == null) {
				target.right = new KDNode<>(key, value);
				return target.right.deleted ? 0 : 1;
			}
		} else {
			next_node = target.left;
			if (next_node == null) {
				target.left = new KDNode<>(key, value);
				return target.left.deleted ? 0 : 1;
			}
		}

		return insert(key, value, next_node, next_lev, K);
	}

	public static boolean checkEquals(float[] a, float[] b, int dimension) {

		// seems faster than java.util.Arrays.checkEquals(), which is not
		// currently supported by Matlab anyway
		for (int i = 0; i < dimension; ++i)
			if (a[i] != b[i])
				return false;

		return true;
	}

	// Method srch translated from 352.srch.c of Gonnet & Baeza-Yates
	protected static <UO> KDNode<UO> srch(float[] key, KDNode<UO> t, int K) {

		for (int lev = 0; t != null; lev = (lev + 1) % K) {

			if (!t.deleted && checkEquals(key, t.k, K)) {
				return t;
			} else if (key[lev] > t.k[lev]) {
				t = t.right;
			} else {
				t = t.left;
			}
		}

		return null;
	}

	// Method rsearch translated from 352.range.c of Gonnet & Baeza-Yates
	protected static <UO> void rsearch(
					float[] lowk, float[] uppk, KDNode<UO> target, int lev, int K, List<KDNode<UO>> v) {

		if (target == null) return;
		if (lowk[lev] <= target.k[lev]) {
			rsearch(lowk, uppk, target.left, (lev + 1) % K, K, v);
		}
		if (!target.deleted) {
			int j = 0;
			while (j < K && lowk[j] <= target.k[j] &&
							uppk[j] >= target.k[j]) {
				j++;
			}
			if (j == K) v.add(target);
		}
		if (uppk[lev] > target.k[lev]) {
			rsearch(lowk, uppk, target.right, (lev + 1) % K, K, v);
		}
	}


	// Method Nearest Neighbor from Andrew Moore's thesis. Numbered
	// comments are direct quotes from there.   NnbrList solution
	// courtesy of Bjoern Heckel.
	protected static <UO> void nnbr(
					KDNode<UO> node, float[] target, HRect hr, float max_dist_sqd, int lev, int K,
					NnbrStore<KDNode<UO>> nnl, long timeLimit) {

		// 1. if node is empty then set dist-sqd to infinity and exit.
//		if (node == null) {
//			return;
//		}

//		if ((timeout > 0) && (timeout < System.currentTimeMillis())) {
//			return;
//		}
		// 2. s := split field of node
		int s = lev % K;

		// 3. pivot := dom-elt field of node
		float[] pivot = node.k;
		float pivot_to_target = sqrdist(pivot, target, K);

		// 4. Cut hr into to sub-hyperrectangles left-hr and right-hr.
		//    The cut plane is through pivot and perpendicular to the s
		//    K.
		HRect left_hr = hr; // optimize by not cloning
		HRect right_hr = new HRect(hr.min, hr.max, K);
		left_hr.max[s] = pivot[s];
		right_hr.min[s] = pivot[s];

		// 5. target-in-left := target_s <= pivot_s
		boolean target_in_left = target[s] < pivot[s];

		KDNode<UO> nearer_kd;
		HRect nearer_hr;
		KDNode<UO> further_kd;
		HRect further_hr;

		// 6. if target-in-left then
		//    6.1. nearer-node := left field of node and nearer-hr := left-hr
		//    6.2. further-node := right field of node and further-hr := right-hr
		if (target_in_left) {
			nearer_kd = node.left;
			nearer_hr = left_hr;
			further_kd = node.right;
			further_hr = right_hr;
		}
		//
		// 7. if not target-in-left then
		//    7.1. nearer-node := right field of node and nearer-hr := right-hr
		//    7.2. further-node := left field of node and further-hr := left-hr
		else {
			nearer_kd = node.right;
			nearer_hr = right_hr;
			further_kd = node.left;
			further_hr = left_hr;
		}

		// 8. Recursively call Nearest Neighbor with paramters
		//    (nearer-node, target, nearer-hr, max-dist-sqd), storing the
		//    results in nearest and dist-sqd
		if (nearer_kd != null)
			nnbr(nearer_kd, target, nearer_hr, max_dist_sqd, lev + 1, K, nnl, timeLimit);

//		KDNode<UO> nearest = nnl.getHighest();
		float dist_sqd;

		if (!nnl.isCapacityReached()) {
			dist_sqd = Float.MAX_VALUE;
		} else {
			dist_sqd = nnl.getMaxPriority();
		}

		// 9. max-dist-sqd := minimum of max-dist-sqd and dist-sqd
		max_dist_sqd = Math.min(max_dist_sqd, dist_sqd);

		// 10. A nearer point could only lie in further-node if there were some
		//     part of further-hr within distance max-dist-sqd of
		//     target.
		float[] closest = further_hr.closest(target);
		if (sqrdist(closest, target, K) < max_dist_sqd) {

			// 10.1 if (pivot-target)^2 < dist-sqd then
			if (pivot_to_target < dist_sqd) {

				// 10.1.1 nearest := (pivot, range-elt field of node)
//				nearest = node;

				// 10.1.2 dist-sqd = (pivot-target)^2
				dist_sqd = pivot_to_target;

				// add to nnl
				if (!node.deleted) {
					nnl.insert(node, dist_sqd);
				}

				// 10.1.3 max-dist-sqd = dist-sqd
				// max_dist_sqd = dist_sqd;
				if (nnl.isCapacityReached()) {
					max_dist_sqd = nnl.getMaxPriority();
				} else {
					max_dist_sqd = Float.MAX_VALUE;
				}
			}

			// 10.2 Recursively call Nearest Neighbor with parameters
			//      (further-node, target, further-hr, max-dist_sqd),
			//      storing results in temp-nearest and temp-dist-sqd
			if (further_kd != null && System.nanoTime() < timeLimit)
				nnbr(further_kd, target, further_hr, max_dist_sqd, lev + 1, K, nnl, timeLimit);
		}
	}

	protected static float sqrdist(float[] x, float[] y, int K) {

		float dist = 0, diff;

		for (int i = 0; i < K; ++i) {
			diff = (x[i] - y[i]);
			dist += diff * diff;
		}

		return dist;
	}

	protected String toString(int depth) {
		String s = k + "  " + v + (deleted ? "*" : "");
		if (left != null) {
			s = s + "\n" + pad(depth) + "L " + left.toString(depth + 1);
		}
		if (right != null) {
			s = s + "\n" + pad(depth) + "R " + right.toString(depth + 1);
		}
		return s;
	}

	private static String pad(int n) {
		String s = "";
		for (int i = 0; i < n; ++i) {
			s += " ";
		}
		return s;
	}

}
