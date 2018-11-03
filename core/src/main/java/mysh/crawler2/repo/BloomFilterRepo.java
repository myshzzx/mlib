package mysh.crawler2.repo;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import mysh.collect.Pair;
import mysh.crawler2.UrlContext;
import mysh.crawler2.UrlCtxHolder;
import mysh.util.Asserts;
import mysh.util.Encodings;
import mysh.util.FilesUtil;

import java.io.File;
import java.io.IOException;
import java.util.Queue;

/**
 * BloomFilterRepo
 *
 * @author mysh
 * @since 2016/8/13
 */
public class BloomFilterRepo<Ctx extends UrlContext> implements Repo<Ctx> {
	private final File file;
	private BloomFilter<String> urls;
	private int expectedInsertions;
	private double fpp;

	/**
	 * @param expectedInsertions 预期插入数.
	 * @param fpp                误报率 (0~1).
	 */
	public BloomFilterRepo(File file, int expectedInsertions, double fpp) {
		this.expectedInsertions = expectedInsertions;
		this.fpp = fpp;
		Asserts.notNull(file, "save file");
		this.file = file;
	}

	@Override
	public Queue<UrlCtxHolder<Ctx>> load() {
		if (file.exists()) {
			try {
				Pair<BloomFilter<String>, Queue<UrlCtxHolder<Ctx>>> data = FilesUtil.decompressFile(file);
				urls = data.getL();
				return data.getR();
			} catch (IOException e) {
				throw new RuntimeException("load file error.", e);
			}
		} else {
			urls = BloomFilter.create(Funnels.stringFunnel(Encodings.UTF_8), expectedInsertions, fpp);
			return null;
		}
	}

	@Override
	public void save(Queue<UrlCtxHolder<Ctx>> tasks) {
		Pair<BloomFilter<String>, Queue<UrlCtxHolder<Ctx>>> data = Pair.of(urls, tasks);
		try {
			FilesUtil.compress2File(file, data);
		} catch (IOException e) {
			throw new RuntimeException("save file error.", e);
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
