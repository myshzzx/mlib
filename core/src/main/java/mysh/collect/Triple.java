package mysh.collect;

import java.io.Serializable;

/**
 * Triple
 *
 * @author mysh
 * @since 2016/1/11
 */
public class Triple<L, M, R> implements Serializable {
	private static final long serialVersionUID = 6961926166226547937L;

	private L l;
	private M m;
	private R r;

	private Triple(L l, M m, R r) {
		this.l = l;
		this.m = m;
		this.r = r;
	}

	public L getL() {
		return l;
	}

	public M getM() {
		return m;
	}

	public R getR() {
		return r;
	}

	public static <L, M, R> Triple<L, M, R> of(L l, M m, R r) {
		return new Triple<>(l, m, r);
	}
}
