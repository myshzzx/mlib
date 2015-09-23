package mysh.collect;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * ConcurrentBiMap
 *
 * @author mysh
 * @since 2015/9/23
 */
public class ConcurrentBiMap<K, V> implements Map<K, V> {
	private ConcurrentHashMap<K, V> self;
	private ConcurrentHashMap<V, K> reverse;
	private ConcurrentBiMap<V, K> reverseView;

	public static <K, V> ConcurrentBiMap<K, V> create() {
		ConcurrentHashMap<K, V> self = new ConcurrentHashMap<>();
		ConcurrentHashMap<V, K> reverse = new ConcurrentHashMap<>();
		ConcurrentBiMap<K, V> biMap = new ConcurrentBiMap<>(self, reverse);
		biMap.reverseView = new ConcurrentBiMap<>(reverse, self);
		biMap.reverseView.reverseView = biMap;
		return biMap;
	}

	private ConcurrentBiMap(ConcurrentHashMap<K, V> self, ConcurrentHashMap<V, K> reverse) {
		this.self = self;
		this.reverse = reverse;
	}

	public ConcurrentBiMap<V, K> getReverse() {
		return reverseView;
	}

	// =========================================

	@Override
	public void clear() {
		self.clear();
		reverse.clear();
	}

	@Override
	public Set<K> keySet() {
		return self.keySet();
	}

	@Override
	public int size() {
		return self.size();
	}

	@Override
	public boolean isEmpty() {
		return self.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return self.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return reverse.containsKey(value);
	}

	@Override
	public V get(Object key) {
		return self.get(key);
	}

	@Override
	public V put(K key, V value) {
		V put = self.put(key, value);
		reverse.put(value, key);
		return put;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		self.putAll(m);
		for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
			reverse.put(entry.getValue(), entry.getKey());
		}
	}

	@Override
	public V remove(Object key) {
		V remove = self.remove(key);
		if (remove != null)
			reverse.remove(remove);
		return remove;
	}

	@Override
	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
		throw new UnsupportedOperationException("implement this when need");
	}

	@Override
	public Collection<V> values() {
		return reverse.keySet();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return self.entrySet();
	}
}
