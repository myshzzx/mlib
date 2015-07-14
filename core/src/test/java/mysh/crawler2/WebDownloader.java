package mysh.crawler2;

import mysh.net.httpclient.HttpClientAssist;
import mysh.net.httpclient.HttpClientConfig;
import mysh.util.FilesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

/**
 * @author Mysh
 * @since 2014/9/25 15:37
 */
public class WebDownloader implements CrawlerSeed<UrlContext> {
	private static final long serialVersionUID = 498361912566529068L;
	private static final Logger log = LoggerFactory.getLogger(WebDownloader.class);
	private static final File saveFile = new File("l:/wdStore");
	private static final String v = "";

	transient List<UrlCtxHolder<UrlContext>> seeds = new ArrayList<>();

	Queue<UrlCtxHolder<UrlContext>> unhandledSeeds = new ConcurrentLinkedQueue<>();
	Map<String, Serializable> repo = new ConcurrentHashMap<>(8000);

	private static final String ROOT = "http://www.geekonomics10000.com/";

	public static void main(String[] args) throws Exception {
		HttpClientConfig hcc = new HttpClientConfig();
//		hcc.setUserAgent(HttpClientConfig.UA_BAIDU);
		hcc.setMaxConnPerRoute(3);
//		hcc.setUseProxy(true);
		hcc.setProxyHost("127.0.0.1");
		hcc.setProxyPort(8087);

		Crawler<UrlContext> c = new Crawler<>(new WebDownloader(), hcc);
		c.start();

		while (c.getStatus() != Crawler.Status.STOPPED) {
			// offer an opportunity to run c.stop()
			Thread.sleep(5000);
		}

		System.out.println("end");
	}

	public WebDownloader() {
		String u = ROOT;
		seeds.add(new UrlCtxHolder<>(u, null));
	}

	public static final int WAIT_TIME = 5000;

	@Override
	public boolean accept(String url, UrlContext ctx) {
		return !repo.containsKey(url)
						&& url.startsWith(ROOT)
						;
	}

	@Override
	public boolean onGet(HttpClientAssist.UrlEntity e, UrlContext ctx) {
		if (e.getContentLength() < 512) return false;

		repo.put(e.getCurrentURL(), v);
		repo.put(e.getReqUrl(), v);

		try {
			String fUri = e.getCurrentURL().substring(ROOT.length());
			int endIdx = fUri.lastIndexOf('?');
			if (endIdx > 0) {
				fUri = fUri.substring(0, endIdx);
			}
			File f = new File("l:/a/" + fUri);
			f.getParentFile().mkdirs();
			if (f.exists() && f.length() > 0) return true;

			try (OutputStream out = new FileOutputStream(f)) {
				e.bufWriteTo(out);
			} catch (Exception ex) {
				log.error("下载失败: " + e.getCurrentURL(), ex);
				repo.remove(e.getCurrentURL());
				repo.remove(e.getReqUrl());
				return false;
			}
			return true;
		} finally {
			try {
				Thread.sleep(WAIT_TIME);
			} catch (InterruptedException e1) {
			}
		}
	}

	@Override
	public void init() throws IOException, ClassNotFoundException {
		if (saveFile.exists()) {
			Map[] savedObj = FilesUtil.getObjectFromFile(saveFile.getAbsolutePath());

			Map tRepo = savedObj[0];
			if (tRepo != null && tRepo.size() > 0)
				repo = tRepo;

			Queue<UrlCtxHolder<UrlContext>> tUnhandledSeeds = (Queue<UrlCtxHolder<UrlContext>>) savedObj[1];
			if (tUnhandledSeeds != null && tUnhandledSeeds.size() > 0) {
				seeds.clear();
				seeds.addAll(tUnhandledSeeds);
			}
		}

		log.info("seeds.size=" + this.seeds.size());
	}

	@Override
	public void onCrawlerStopped(Queue<UrlCtxHolder<UrlContext>> unhandledSeeds) {
		this.unhandledSeeds = unhandledSeeds;
		try {
			FilesUtil.writeObjectToFile(saveFile.getAbsolutePath(),
					new Object[]{this.repo, this.unhandledSeeds});
		} catch (IOException e) {
			log.error("save unhandled seeds failed.", e);
		}
	}

	@Override
	public Stream<UrlCtxHolder<UrlContext>> getSeeds() {
		return seeds.stream();
	}

}


