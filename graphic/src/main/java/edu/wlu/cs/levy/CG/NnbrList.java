package edu.wlu.cs.levy.CG;

// Bjoern Heckel's solution to the KD-Tree n-nearest-neighbor problem

class NnbrList<T> implements NnbrStore<T> {

	java.util.PriorityQueue<NeighborEntry<T>> m_Queue;
	int m_Capacity = 0;

	static class NeighborEntry<T> implements Comparable<NeighborEntry<T>> {
		final T data;

		final float value;

		public NeighborEntry(final T data,
		                     final float value) {
			this.data = data;
			this.value = value;
		}

		public int compareTo(NeighborEntry<T> t) {
			// note that the positions are reversed!
			return Float.compare(t.value, this.value);
		}


	}

	// constructor
	public NnbrList(int capacity) {
		m_Capacity = capacity;
		m_Queue = new java.util.PriorityQueue<>(m_Capacity);
	}

	public float getMaxPriority() {
		NeighborEntry p = m_Queue.peek();
		return (p == null) ? Float.POSITIVE_INFINITY : p.value;
	}

	public boolean insert(T object, float priority) {
		if (isCapacityReached()) {
			if (priority > getMaxPriority()) {
				// do not insert - all elements in queue have lower priority
				return false;
			}
			m_Queue.add(new NeighborEntry<T>(object, priority));
			// remove object with highest priority
			m_Queue.poll();
		} else {
			m_Queue.add(new NeighborEntry<T>(object, priority));
		}
		return true;
	}

	public boolean isCapacityReached() {
		return m_Queue.size() >= m_Capacity;
	}

	public T getHighest() {
		NeighborEntry<T> p = m_Queue.peek();
		return (p == null) ? null : p.data;
	}

	public boolean isEmpty() {
		return m_Queue.size() == 0;
	}

	public int getSize() {
		return m_Queue.size();
	}

	public T removeHighest() {
		// remove object with highest priority
		NeighborEntry<T> p = m_Queue.poll();
		return (p == null) ? null : p.data;
	}

	public NeighborEntry<T> removeHighestEntry() {
		return m_Queue.poll();
	}
}
