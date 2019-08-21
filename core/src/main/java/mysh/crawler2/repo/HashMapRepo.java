package mysh.crawler2.repo;

import mysh.collect.Pair;
import mysh.crawler2.UrlContext;
import mysh.crawler2.UrlCtxHolder;
import mysh.sql.sqlite.SqliteKV;
import mysh.util.FilesUtil;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HashMapRepo
 *
 * @author mysh
 * @since 2016/8/13
 */
@ThreadSafe
public class HashMapRepo<CTX extends UrlContext> implements Repo<CTX> {
	public Map<String, Object> urls;
	
	private File file;
	private SqliteKV.DAO sqliteDao;
	private String sqliteItemName;
	
	public HashMapRepo(File file) {
		this.file = Objects.requireNonNull(file, "file can't be null");
	}
	
	public HashMapRepo(SqliteKV.DAO sqliteDao, String sqliteItemName) {
		this.sqliteDao = sqliteDao;
		this.sqliteItemName = sqliteItemName;
	}
	
	@Override
	public Collection<UrlCtxHolder<CTX>> load() {
		Pair<Map<String, Object>, Collection<UrlCtxHolder<CTX>>> data = null;
		if (file != null && file.exists()) {
			try {
				data = FilesUtil.decompressFile(file);
			} catch (IOException e) {
				throw new RuntimeException("load file error.", e);
			}
		} else if (sqliteDao != null) {
			data = sqliteDao.byKey(sqliteItemName);
		}
		
		if (data != null) {
			urls = data.getL();
			return data.getR();
		} else {
			urls = new ConcurrentHashMap<>();
			return null;
		}
	}
	
	@Override
	public void save(Collection<UrlCtxHolder<CTX>> tasks) {
		Pair<Map<String, Object>, Collection<UrlCtxHolder<CTX>>> data = Pair.of(urls, tasks);
		if (file != null) {
			try {
				FilesUtil.compress2File(file, data);
			} catch (IOException e) {
				throw new RuntimeException("save file error.", e);
			}
		} else if (sqliteDao != null) {
			sqliteDao.save(sqliteItemName, data);
		}
	}
	
	@Override
	public void add(String url) {
		put(url, "");
	}
	
	public void put(String url, Object content) {
		urls.put(url, content);
	}
	
	public <T> T get(String url) {
		return (T) urls.get(url);
	}
	
	@Override
	public boolean contains(String url) {
		return urls.containsKey(url);
	}
	
	@Override
	public void remove(String url) {
		urls.remove(url);
	}
}
