package mysh.crawler2;

import mysh.net.httpclient.HttpClientAssist;
import mysh.net.httpclient.HttpClientConfig;
import mysh.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Mysh
 * @since 2014/9/25 15:37
 */
public class ImageDownloader implements CrawlerSeed {
	private static final long serialVersionUID = 498361912566529068L;
	private static final Logger log = LoggerFactory.getLogger(ImageDownloader.class);
	private static final File saveFile = new File("l:/idStore");
	private static final String v = "";

	transient List<String> seeds = new ArrayList<>();
	Queue<String> unhandledSeeds = new ConcurrentLinkedQueue<>();
	Map<String, Serializable> repo = new ConcurrentHashMap<>(8000);

	public static void main(String[] args) throws InterruptedException {
		HttpClientConfig hcc = new HttpClientConfig();
		hcc.setMaxConnPerRoute(4);
//		hcc.setUseProxy(true);
		hcc.setProxyHost("127.0.0.1");
		hcc.setProxyPort(8058);

		Crawler c = new Crawler(new ImageDownloader(), hcc);
		c.start();

		while (c.getStatus() == Crawler.Status.RUNNING) {
			// offer an opportunity to run c.stop()
			Thread.sleep(5000);
		}

		System.out.println("end");
	}

	public ImageDownloader() {
		String u = "http://forum.bodybuilding.com/showthread.php?t=143308563";
		seeds.add(u);
		int i = 1;
		while (i++ < 105)
			seeds.add(u + "&page=" + i);
	}

	private static final List<String> blockDomain = Arrays.asList("blogspot.com", "wordpress.com");

	@Override
	public boolean accept(String url) {
		return !repo.containsKey(url)
						&& blockDomain.stream().filter(url::contains).count() == 0
						&& !url.contains("amp;")
						&& (
						url.endsWith("jpg")
										|| url.endsWith("gif")
										|| url.endsWith("jpeg")
										|| url.endsWith("png")
										|| url.startsWith("http://forum.bodybuilding.com/showthread.php?t=143308563")
		);
	}

	private static final int ACCESS_WAIT = 500;
	private static final Random r = new Random();

	@Override
	public boolean onGet(HttpClientAssist.UrlEntity e) {
		repo.put(e.getCurrentURL(), v);
		repo.put(e.getReqUrl(), v);

		try {
			if (e.isImage() && e.getContentLength() > 15_000) {
				File f = new File("l:/a", new File(e.getCurrentURL()).getName());
				if (f.exists() && f.length() > 0) return true;

				try (OutputStream out = new FileOutputStream(f)) {
					e.bufWriteTo(out);
				} catch (Exception ex) {
					log.error("下载失败: " + e.getCurrentURL(), ex);
					return false;
				}
			}
			return true;
		} finally {
			try {
				Thread.sleep(300L + r.nextInt(ACCESS_WAIT));
			} catch (InterruptedException e1) {
			}
		}
	}

	@Override
	public void init() {
		if (saveFile.exists()) {
			Object[] savedObj = FileUtil.getObjectFromFile(saveFile.getAbsolutePath());

			Map tRepo = (Map) savedObj[0];
			if (tRepo != null && tRepo.size() > 0)
				repo = tRepo;

			Queue<String> tUnhandledSeeds = (Queue<String>) savedObj[1];
			if (tUnhandledSeeds != null && tUnhandledSeeds.size() > 0) {
				seeds.clear();
				seeds.addAll(tUnhandledSeeds);
			}
		}
	}

	@Override
	public void onCrawlerStopped(Queue<String> unhandledSeeds) {
		this.unhandledSeeds = unhandledSeeds;
		FileUtil.writeObjectToFile(saveFile.getAbsolutePath(),
						new Object[]{this.repo, this.unhandledSeeds});
	}

	@Override
	public List<String> getSeeds() {
		return seeds;
	}

	@Override
	public int requestThreadSize() {
		return 30;
	}
}


