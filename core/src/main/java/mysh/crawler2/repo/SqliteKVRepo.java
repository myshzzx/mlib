package mysh.crawler2.repo;

import mysh.crawler2.UrlContext;
import mysh.crawler2.UrlCtxHolder;
import mysh.sql.sqlite.SqliteDB;

import java.util.Collection;

/**
 * save all urls in sqlite kv store.
 *
 * @since 2019-08-20
 */
public class SqliteKVRepo<CTX extends UrlContext> implements Repo<CTX> {
	private SqliteDB.KvDAO dao;
	
	public SqliteKVRepo(SqliteDB.KvDAO dao) {
		this.dao = dao;
	}
	
	@Override
	public Collection<UrlCtxHolder<CTX>> load() {
		return dao.byKey("\u0000");
	}
	
	@Override
	public void add(String url) {
		put(url, null);
	}
	
	@Override
	public void put(String url, Object content) {
		dao.save(url, content);
	}
	
	@Override
	public <T> T get(String url) {
		return dao.byKey(url);
	}
	
	@Override
	public boolean contains(String url) {
		return dao.containsKey(url);
	}
	
	@Override
	public void remove(String url) {
		dao.remove(url);
	}
	
	@Override
	public void save(Collection<UrlCtxHolder<CTX>> tasks) {
		dao.save("\u0000", tasks);
	}
}
