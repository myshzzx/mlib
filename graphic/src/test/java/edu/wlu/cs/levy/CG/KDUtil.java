package edu.wlu.cs.levy.CG;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mysh
 * @since 14-1-13 上午9:15
 */
public class KDUtil {

	public static void checkUserObjNum(KDTree t) {
		try {
			Field m_root = t.getClass().getDeclaredField("m_root");
			m_root.setAccessible(true);
			KDNode root = (KDNode) m_root.get(t);

			AtomicInteger totalCount = new AtomicInteger(0);
			AtomicInteger duCount = new AtomicInteger(0);
			if (root != null)
				checkUONum(root, totalCount, duCount);
			System.out.println("total KDNode count: " + totalCount.get());
//			System.out.println("duplicated sift descriptor count: " + duCount.get());
		} catch (Exception e) {
		}
	}

	private static void checkUONum(KDNode node, AtomicInteger totalCount, AtomicInteger duCount) {
		totalCount.incrementAndGet();
//		if (node.v.size() > 1) {
//			duCount.incrementAndGet();
//			System.out.println("uoNum: " + node.v.size() + " " + Arrays.toString(node.v.toArray()));
//		}

		if (node.left != null) checkUONum(node.left, totalCount, duCount);
		if (node.right != null) checkUONum(node.right, totalCount, duCount);
	}
}
