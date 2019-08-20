package mysh.crawler2.repo;

import mysh.crawler2.UrlContext;
import mysh.crawler2.UrlCtxHolder;
import mysh.sql.sqlite.SqliteKV;

import java.nio.file.Path;
import java.util.Collection;

/**
 * @since 2019-08-20
 */
public class SqliteKVRepo<CTX extends UrlContext> implements Repo<CTX> {
	private SqliteKV kv;
	private SqliteKV.DAO dao;
	
	public SqliteKVRepo(Path dbFile, String name) {
		kv = new SqliteKV(dbFile);
		dao = kv.genDAO(name, false, false);
	}
	
	public SqliteKVRepo(SqliteKV kv, String name) {
		this.kv = kv;
		dao = kv.genDAO(name, false, false);
	}
	
	@Override
	public Collection<UrlCtxHolder<CTX>> load() {
		return dao.byKey("\u0000");
	}
	
	@Override
	public void add(String url) {
		put(url, "");
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
		return dao.byKey(url) != null;
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
