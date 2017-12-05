package mysh.collect;

import mysh.util.Asserts;

import java.util.*;

/**
 * Collection utils.
 *
 * @author mysh
 * @since 2016/3/1
 */
public abstract class Colls {
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
        if (kvs != null && kvs.length % 2 == 1)
            throw new IllegalArgumentException("params should be paired");

        if (kvs != null) {
            int len = kvs.length;
            for (int i = 0; i < len; i += 2) {
                m.put((K) kvs[i], (V) kvs[i + 1]);
            }
        }
        return m;
    }

    public static <T> Set<T> ofHashSet(T... es) {
        Set<T> s = new HashSet<>();

        if (es != null) {
            for (T e : es) {
                s.add(e);
            }
        }

        return s;
    }

    public static <T> List<T> fillNull(int len) {
        Asserts.require(len >= 0, "negative len");
        List<T> l = new ArrayList<>(len);
        while (l.size() < len) {
            l.add(null);
        }
        return l;
    }
}
