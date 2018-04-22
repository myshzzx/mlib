package mysh.collect;

import mysh.util.Asserts;

import java.util.*;
import java.util.function.Predicate;

/**
 * Collection utils.
 *
 * @author mysh
 * @since 2016/3/1
 */
public abstract class Colls {
	private static final Random rnd = new Random();
	
	public static boolean isEmpty(Map<?, ?> map) {
		return map == null || map.isEmpty();
	}
	
	public static boolean isNotEmpty(Map<?, ?> map) {
		return !isEmpty(map);
	}
	
	public static boolean isEmpty(Collection<?> coll) {
		return coll == null || coll.isEmpty();
	}
	
	public static boolean isNotEmpty(Collection<?> coll) {
		return !isEmpty(coll);
	}
	
	/**
	 * create a hashMap without compile time type check.
	 */
	public static <K, V> Map<K, V> ofHashMap(Object... kvs) {
		Map<K, V> m = new HashMap<>();
		if (kvs != null && kvs.length % 2 == 1) {
			throw new IllegalArgumentException("params should be paired");
		}
		
		if (kvs != null) {
			int len = kvs.length;
			for (int i = 0; i < len; i += 2) {
				m.put((K) kvs[i], (V) kvs[i + 1]);
			}
		}
		return m;
	}
	
	public static <T> Set<T> ofHashSet(T... ts) {
		if (ts == null || ts.length == 0) {
			return new HashSet<>();
		}
		// 尽量避免扩容
		HashSet<T> s = new HashSet<>(Math.max(ts.length, 16));
		s.addAll(Arrays.asList(ts));
		return s;
	}
	
	public static <T> List<T> ofList(T... es) {
		if (es == null || es.length == 0) {
			return new ArrayList<>();
		} else {
			return new ArrayList<T>(Arrays.asList(es));
		}
	}
	
	public static <T> T random(List<T> lst) {
		return lst.get(rnd.nextInt(lst.size()));
	}
	
	public static <T> List<T> fillNull(int len) {
		Asserts.require(len >= 0, "negative len");
		List<T> l = new ArrayList<>(len);
		while (l.size() < len) {
			l.add(null);
		}
		return l;
	}
	
	public static <OT> List<List<OT>> split(List<OT> entire, int splitCount) {
		Objects.requireNonNull(entire, "entire-obj-should-not-be-null");
		if (entire.size() < 1 || splitCount < 1) {
			throw new IllegalArgumentException(
					"can't split " + entire.size() + "-ele-array into " + splitCount + " parts.");
		}
		splitCount = Math.min(entire.size(), splitCount);
		
		@SuppressWarnings("unchecked")
		List<List<OT>> s = new ArrayList<>();
		
		int start = 0, left = entire.size(), leftSplit = splitCount, step;
		while (left > 0) {
			step = left % leftSplit == 0 ?
					left / leftSplit :
					left / leftSplit + 1;
			List<OT> subR = new ArrayList<>(entire.subList(start, start + step));
			start += step;
			left -= step;
			leftSplit--;
			s.add(subR);
		}
		
		return s;
	}
	
	public static <K, V> Map<K, V> filterNew(Map<K, V> m, Predicate<Map.Entry<K, V>> filter) {
		Map<K, V> nm = new HashMap<>();
		m.entrySet().stream().filter(filter).forEach(e -> nm.put(e.getKey(), e.getValue()));
		return nm;
	}
	
	public static Object[][] of2D(int rows, int cols, Object... eles) {
		if (rows <= 0 || cols <= 0 || rows * cols != eles.length) {
			throw new IllegalArgumentException(String.format("%d,%d,%d", rows, cols, eles.length));
		}
		Object[][] m = new Object[rows][];
		for (int ri = 0, ei = 0; ri < rows; ri++) {
			m[ri] = new Object[cols];
			for (int ci = 0; ci < cols; ci++) {
				m[ri][ci] = eles[ei++];
			}
		}
		return m;
	}
	
	
	public static Map<Object, Integer> of2DRowHeader(Object[][] m) {
		Object[] h = m[0];
		Map<Object, Integer> hm = new HashMap<>();
		for (int i = 1; i < h.length; i++) {
			hm.put(h[i], i);
		}
		return hm;
	}
	
	public static Map<Object, Integer> of2DColHeader(Object[][] m) {
		Map<Object, Integer> hm = new HashMap<>();
		for (int i = 1; i < m.length; i++) {
			hm.put(m[i][0], i);
		}
		return hm;
	}
}
