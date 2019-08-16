package mysh.crawler2.repo;

import mysh.crawler2.UrlContext;
import mysh.crawler2.UrlCtxHolder;

import java.util.Collection;

/**
 * Repo
 *
 * @author mysh
 * @since 2016/8/13
 */
public interface Repo<CTX extends UrlContext> {

	/**
	 * load saved info. return saved tasks.
	 */
	Collection<UrlCtxHolder<CTX>> load();

	/**
	 * save repo info and tasks.
	 */
	void save(Collection<UrlCtxHolder<CTX>> tasks);

	/**
	 * put url to repo.
	 */
	void add(String url);

	/**
	 * check whether repo contains url.
	 */
	boolean contains(String url);

	/**
	 * remove url from repo.
	 */
	void remove(String url);

}
