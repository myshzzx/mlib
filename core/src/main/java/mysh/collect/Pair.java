package mysh.collect;

import java.io.Serializable;

/**
 * Pair
 *
 * @author mysh
 * @since 2016/1/11
 */
public class Pair<L, R> implements Serializable {
	private static final long serialVersionUID = 2211820834340034966L;

	private L l;
	private R r;

	private Pair(L l, R r) {
		this.l = l;
		this.r = r;
	}

	public L getL() {
		return l;
	}

	public R getR() {
		return r;
	}

	public static <L, R> Pair<L, R> of(L l, R r) {
		return new Pair<>(l, r);
	}

	@Override
	public String toString() {
		return "Pair{" +
				"l=" + l +
				", r=" + r +
				'}';
	}
}
