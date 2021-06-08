package mysh.crawler2;

import java.io.Serializable;

/**
 * WARNING: subtypes should rewrite {@link Object#equals} and {@link Object#hashCode}
 * to prevent same tasks from been submit to crawler task. {@link lombok.Data} can be simply used.
 */
public interface UrlContext extends Serializable, Comparable<Object> {
	
	/**
	 * to change crawler executor handling order, rewrite this method.
	 * handle by the {@link UrlContext} sorting order.
	 */
	@Override
	default int compareTo(Object o) {
		return 0;
	}
}
