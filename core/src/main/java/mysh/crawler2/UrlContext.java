package mysh.crawler2;

import java.io.Serializable;

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
