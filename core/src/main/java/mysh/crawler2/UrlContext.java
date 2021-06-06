package mysh.crawler2;

import java.io.Serializable;

public class UrlContext implements Serializable, Comparable<Object> {
	
	private static final long serialVersionUID = 1819236568182342681L;
	
	/**
	 * to change crawler executor handling order, rewrite this method.
	 * handle by the {@link UrlContext} sorting order.
	 */
	@Override
	public int compareTo(Object o) {
		return 0;
	}
}
