// Hyper-Rectangle class supporting KDTree class

package edu.wlu.cs.levy.CG;

class HRect {

	protected float[] min;
	protected float[] max;

	protected HRect(float[] vmin, float[] vmax, int dimension) {

		min = new float[dimension];
		System.arraycopy(vmin, 0, min, 0, dimension);
		max = new float[dimension];
		System.arraycopy(vmax, 0, max, 0, dimension);
	}

	// from Moore's eqn. 6.6
	protected float[] closest(float[] t) {

		float[] p = new float[t.length];

		for (int i = 0; i < t.length; ++i) {
			if (t[i] <= min[i]) {
				p[i] = min[i];
			} else if (t[i] >= max[i]) {
				p[i] = max[i];
			} else {
				p[i] = t[i];
			}
		}

		return p;
	}

	// used in initial conditions of KDTree.nearest()
	protected static HRect infiniteHRect(int d) {

		float[] vmin = new float[d];
		float[] vmax = new float[d];

		for (int i = 0; i < d; ++i) {
			vmin[i] = Float.NEGATIVE_INFINITY;
			vmax[i] = Float.POSITIVE_INFINITY;
		}

		return new HRect(vmin, vmax, d);
	}

}

