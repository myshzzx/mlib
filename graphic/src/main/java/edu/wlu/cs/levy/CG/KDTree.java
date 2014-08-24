package edu.wlu.cs.levy.CG;

import java.io.Serializable;

/**
 * KDTree is a class supporting KD-tree insertion, deletion, equality
 * search, range search, and nearest neighbor(s) using double-precision
 * floating-point keys.  Splitting dimension is chosen naively, by
 * depth modulo K.  Semantics are as follows:
 * <p/>
 * <UL>
 * <LI> Two different keys containing identical numbers should retrieve the
 * same value from a given KD-tree.  Therefore keys are cloned when a
 * node is inserted.
 * <BR><BR>
 * <LI> As with Hashtables, values inserted into a KD-tree are <I>not</I>
 * cloned.  Modifying a value between insertion and retrieval will
 * therefore modify the value stored in the tree.
 * </UL>
 * <p/>
 * Implements the Nearest Neighbor algorithm (Table 6.4) of
 * <p/>
 * <PRE>
 * &#064;techreport{AndrewMooreNearestNeighbor,
 * author  = {Andrew Moore},
 * title   = {An introductory tutorial on kd-trees},
 * institution = {Robotics Institute, Carnegie Mellon University},
 * year    = {1991},
 * number  = {Technical Report No. 209, Computer Laboratory,
 * University of Cambridge},
 * address = {Pittsburgh, PA}
 * }
 * </PRE>
 *
 * @author Simon Levy, Bjoern Heckel
 * @version %I%, %G%
 * @since JDK1.2
 */
public class KDTree<UO> implements Serializable {

	private static final long serialVersionUID = 3413993329844381227L;

	private static final long DEFAULT_TIMEOUT = 100_000;

	/**
	 * micro-second timeout for each {@link #getnbrs(float[], int)} .
	 */
	long m_timeout = DEFAULT_TIMEOUT;

	// K = number of dimensions
	final private int m_K;

	// root of KD-tree
	private KDNode<UO> m_root;

	// count of nodes
	private int m_count;

	/**
	 * Creates a KD-tree with specified number of dimensions.
	 */
	public KDTree(int k) {
		this(k, 0);
	}

	/**
	 * Creates a KD-tree with k dimensions and each feature search timeout(micro-second).
	 */
	public KDTree(int k, long timeout) {
		this.m_timeout = timeout;
		m_K = k;
		m_root = null;
	}

	public boolean isEmpty() {
		return m_root == null;
	}

	/**
	 * Insert a node in a KD-tree.  Uses algorithm translated from 352.ins.c of
	 * <p/>
	 * <PRE>
	 * &#064;Book{GonnetBaezaYates1991,
	 * author =    {G.H. Gonnet and R. Baeza-Yates},
	 * title =     {Handbook of Algorithms and Data Structures},
	 * publisher = {Addison-Wesley},
	 * year =      {1991}
	 * }
	 * </PRE>
	 *
	 * @param key   key for KD-tree node
	 * @param value value at that key
	 */
	public int insert(float[] key, UO value) {

		if (key.length != m_K) {
			throw new IllegalArgumentException("key sizes mismatch.");
		}

		if (m_root == null) {
			m_root = new KDNode<>(key, value);
			m_count = 1;
			return 1;
		} else {
			int insert = KDNode.insert(key, value, m_root, 0, m_K);
			m_count += insert;
			return insert;
		}
	}

	/**
	 * Find  KD-tree node whose key is identical to key.  Uses algorithm
	 * translated from 352.srch.c of Gonnet & Baeza-Yates.
	 *
	 * @param key key for KD-tree node
	 * @return object at key, or null if not found
	 */
	public UO search(float[] key) {

		if (key.length != m_K) {
			throw new IllegalArgumentException("key sizes mismatch.");
		}

		KDNode<UO> kd = KDNode.srch(key, m_root, m_K);

		return (kd == null ? null : kd.v);
	}

	/**
	 * Delete a node from a KD-tree.  Instead of actually deleting node and
	 * rebuilding tree, marks node as deleted.  Hence, it is up to the caller
	 * to rebuild the tree as needed for efficiency.
	 */
	public void delete(float[] key) {

		if (key.length != m_K)
			throw new IllegalArgumentException("key sizes mismatch.");

		KDNode<UO> t = KDNode.srch(key, m_root, m_K);
		if (t != null && !t.deleted) {
			t.v = null;
			t.deleted = true;
			m_count--;
		}

	}

	public int size() { /* added by MSL */
		return m_count;
	}

	public String toString() {
		return m_root.toString(0);
	}

	/**
	 * Object[][KDNode<UO>, dist]
	 */
	public Object[][] getnbrs(float[] key, int n) {

		if (key.length != m_K) {
			throw new IllegalArgumentException("key sizes mismatch.");
		}

//		NnbrList<KDNode<UO>> nnl = new NnbrList<>(n);
		NnbrStore nnl = n == 1 ? new NnbrNode() : new NnbrList(n);

		// initial call is with infinite hyper-rectangle and max distance
		HRect hr = HRect.infiniteHRect(m_K);

		if (m_count > 0) {
			long timeLimit = (this.m_timeout > 0) ?
							System.nanoTime() + this.m_timeout * 1000 :
							System.nanoTime() + DEFAULT_TIMEOUT;
			KDNode.nnbr(m_root, key, hr, Float.MAX_VALUE, 0, m_K, nnl, timeLimit);
		}

		Object[][] result = new Object[n][2];

		if (n > 1) {
			for (int i = n - 1; i > -1; i--) {
				NnbrList.NeighborEntry<KDNode<UO>> nbrEtry = ((NnbrList<KDNode<UO>>) nnl).removeHighestEntry();
				result[i][0] = nbrEtry.data;
				result[i][1] = nbrEtry.value;
			}
		} else {
			result[0][0] = ((NnbrNode<KDNode<UO>>) nnl).data;
			result[0][1] = ((NnbrNode<KDNode<UO>>) nnl).value;
		}

		return result;

	}

	/**
	 * timeout for each feature search(micro-second).
	 */
	public void setTimeout(long timeout) {
		this.m_timeout = timeout;
	}
}

