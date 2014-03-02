package edu.wlu.cs.levy.CG;

/**
 * @author Mysh
 * @since 14-1-17 下午3:30
 */
class NnbrNode<T> implements NnbrStore<T> {
	T data;

	float value = Float.POSITIVE_INFINITY;


	@Override
	public boolean isCapacityReached() {
		return data != null;
	}

	@Override
	public float getMaxPriority() {
		return value;
	}

	@Override
	public boolean insert(T data, float value) {
		if (value < this.value) {
			this.data = data;
			this.value = value;
			return true;
		}
		return false;
	}
}
