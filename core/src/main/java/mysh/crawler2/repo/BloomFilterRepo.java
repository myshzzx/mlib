package mysh.crawler2.repo;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import mysh.collect.Pair;
import mysh.crawler2.UrlContext;
import mysh.crawler2.UrlCtxHolder;
import mysh.sql.sqlite.SqliteDB;
import mysh.util.Encodings;
import mysh.util.FilesUtil;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

/**
 * BloomFilterRepo
 *
 * @author mysh
 * @since 2016/8/13
 */
@ThreadSafe
public class BloomFilterRepo<CTX extends UrlContext> implements Repo<CTX> {
	private BloomFilter<String> urls;
	private int expectedInsertions;
	private double fpp;
	
	private File file;
	private SqliteDB.KvDAO sqliteDao;
	private String sqliteItemName;
	
	/**
	 * @param expectedInsertions 预期插入数.
	 * @param fpp                误报率 (0~1).
	 */
	public BloomFilterRepo(File file, int expectedInsertions, double fpp) {
		this.expectedInsertions = expectedInsertions;
		this.fpp = fpp;
		this.file = Objects.requireNonNull(file, "save file is null");
	}
	
	/**
	 * @param expectedInsertions 预期插入数.
	 * @param fpp                误报率 (0~1).
	 */
	public BloomFilterRepo(SqliteDB.KvDAO sqliteDao, String sqliteItemName, int expectedInsertions, double fpp) {
		this.expectedInsertions = expectedInsertions;
		this.fpp = fpp;
		this.sqliteDao = Objects.requireNonNull(sqliteDao, "dao is null");
		this.sqliteItemName = Objects.requireNonNull(sqliteItemName, "sqliteItemName is null");
	}
	
	@Override
	public Collection<UrlCtxHolder<CTX>> load() {
		Pair<BloomFilter<String>, Collection<UrlCtxHolder<CTX>>> data = null;
		
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
			urls = BloomFilter.create(Funnels.stringFunnel(Encodings.UTF_8), expectedInsertions, fpp);
			return null;
		}
	}
	
	@Override
	public void save(Collection<UrlCtxHolder<CTX>> tasks) {
		Pair<BloomFilter<String>, Collection<UrlCtxHolder<CTX>>> data = Pair.of(urls, tasks);
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
		urls.put(url);
	}
	
	@Override
	public boolean contains(String url) {
		return urls.mightContain(url);
	}
	
	@Override
	public void remove(String url) {
		throw new UnsupportedOperationException("bloom filter can't remove element");
	}
}
