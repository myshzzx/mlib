package mysh.crawler2.repo;

import mysh.collect.Pair;
import mysh.crawler2.UrlContext;
import mysh.crawler2.UrlCtxHolder;
import mysh.util.Asserts;
import mysh.util.FilesUtil;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HashMapRepo
 *
 * @author mysh
 * @since 2016/8/13
 */
@ThreadSafe
public class HashMapRepo<Ctx extends UrlContext> implements Repo<Ctx> {
	private final File file;
	private Map<String, Object> urls;
	
	public HashMapRepo(File file) {
		Asserts.notNull(file, "save file");
		this.file = file;
	}
	
	@Override
	public Collection<UrlCtxHolder<Ctx>> load() {
		if (file.exists()) {
			try {
				Pair<Map<String, Object>, Collection<UrlCtxHolder<Ctx>>> data = FilesUtil.decompressFile(file);
				urls = data.getL();
				return data.getR();
			} catch (IOException e) {
				throw new RuntimeException("load file error.", e);
			}
		} else {
			urls = new ConcurrentHashMap<>();
			return null;
		}
	}
	
	@Override
	public void save(Collection<UrlCtxHolder<Ctx>> tasks) {
		Pair<Map<String, Object>, Collection<UrlCtxHolder<Ctx>>> data = Pair.of(urls, tasks);
		try {
			FilesUtil.compress2File(file, data);
		} catch (IOException e) {
			throw new RuntimeException("save file error.", e);
		}
	}
	
	@Override
	public void add(String url) {
		urls.put(url, "");
	}
	
	public void put(String url, String content) {
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
