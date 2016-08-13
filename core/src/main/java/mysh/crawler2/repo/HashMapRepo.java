package mysh.crawler2.repo;

import mysh.collect.Pair;
import mysh.crawler2.UrlContext;
import mysh.crawler2.UrlCtxHolder;
import mysh.util.FilesUtil;
import org.apache.http.util.Asserts;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HashMapRepo
 *
 * @author mysh
 * @since 2016/8/13
 */
public class HashMapRepo<Ctx extends UrlContext> implements Repo<Ctx> {
	private final File file;
	private Set<String> urls;

	public HashMapRepo(File file) {
		Asserts.notNull(file, "save file");
		this.file = file;
	}

	@Override
	public Queue<UrlCtxHolder<Ctx>> load() {
		if (file.exists()) {
			try {
				Pair<Set<String>, Queue<UrlCtxHolder<Ctx>>> data = FilesUtil.decompressFileFst(file);
				urls = data.getL();
				return data.getR();
			} catch (IOException e) {
				throw new RuntimeException("load file error.", e);
			}
		} else {
			urls = Collections.newSetFromMap(new ConcurrentHashMap<>());
			return null;
		}
	}

	@Override
	public void save(Queue<UrlCtxHolder<Ctx>> tasks) {
		Pair<Set<String>, Queue<UrlCtxHolder<Ctx>>> data = Pair.of(urls, tasks);
		try {
			FilesUtil.compress2FileFst(file, data);
		} catch (IOException e) {
			throw new RuntimeException("save file error.", e);
		}
	}

	@Override
	public void add(String url) {
		urls.add(url);
	}

	@Override
	public boolean contains(String url) {
		return urls.contains(url);
	}

	@Override
	public void remove(String url) {
		urls.remove(url);
	}
}
