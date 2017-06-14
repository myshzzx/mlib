package mysh.crawler2;

import mysh.net.httpclient.HttpClientAssist;
import mysh.net.httpclient.HttpClientConfig;
import mysh.util.FilesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * http://stanleychen.tuchong.com/
 */
public class StanleyDl implements CrawlerSeed<UrlContext> {
	private static final long serialVersionUID = 498361912566529068L;
	private static final Logger log = LoggerFactory.getLogger(StanleyDl.class);
	private static final File saveFile = new File("l:/stanleyStore");
	private static final String v = "";

	transient List<UrlCtxHolder<UrlContext>> seeds = new ArrayList<>();
	Queue<UrlCtxHolder<UrlContext>> unhandledSeeds = new ConcurrentLinkedQueue<>();
	Map<String, Serializable> repo = new ConcurrentHashMap<>(8000);
	Map<String, String> titleMap = new ConcurrentHashMap<>(8000);

	public static final String SAVE_DIR = "l:/stanley/";
	public static final String REPO_FILE = "F:/temp/stanley/stanley.zip";
	public static final int MAX_CONN_PER_ROUTE = 4;
	public static final long folderSize = 800_000_000;

	public static void main(String[] args) throws Exception {
		HttpClientConfig hcc = new HttpClientConfig();

		int ratePerMin = 500;
		Crawler<UrlContext> c = new Crawler<>(new StanleyDl(), hcc, ratePerMin);
		c.start();

		while (c.getStatus() != Crawler.Status.STOPPED) {
			chkAndZipDir(c);

			// manual stop, break on this line, and watch c.stop()
			Thread.sleep(10000);
		}

		System.out.println("end");
	}

	public StanleyDl() {
		String u = "http://stanleychen.tuchong.com/?page=";
		seeds.add(new UrlCtxHolder<>(u, null));

		int n = 0;
		while (++n < 11) {
			seeds.add(new UrlCtxHolder<>(u + n, null));
		}
	}

	private static final List<String> blockDomain = Arrays.asList("blogspot.com", "wordpress.com");

	@Override
	public boolean accept(String url, UrlContext ctx) {
//		if (1 == 1)
//			return true;
		return !url.contains("albums")
						&& !url.contains("tags")
						&& !url.contains("followers")
						&& !url.contains("favorites")
						&&
						(
										url.startsWith("http://stanleychen.tuchong.com/")
														|| url.endsWith(".jpg")
						)
						&& !repo.containsKey(url)
						&& blockDomain.stream().filter(url::contains).count() == 0
						;
	}

	@Override
	public boolean needToDistillUrls(HttpClientAssist.UrlEntity ue, UrlContext ctx) {
		try {
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	static final Pattern titleP = Pattern.compile("<title>(.+?)</title>");

	@Override
	public Stream<UrlCtxHolder<UrlContext>> afterDistillingUrls(
					HttpClientAssist.UrlEntity ue, UrlContext ctx, Stream<String> distilledUrl) {
		String title = "title";
		try {
			String html = ue.getEntityStr();
			Matcher m = titleP.matcher(html);
			if (m.find()) {
				title = m.group(1);
				int idx = title.indexOf(" - ");
				if (idx > 0) {
					title = title.substring(0, idx);
				}
			}
		} catch (IOException e) {
		}
		String t = title;
		return distilledUrl
						.peek(u -> {
							if (u.endsWith(".jpg"))
								titleMap.put(u, t);
						})
						.map(UrlCtxHolder::new);
	}

	@Override
	public boolean onGet(HttpClientAssist.UrlEntity e, UrlContext ctx) {

		repo.put(e.getCurrentURL(), v);
		repo.put(e.getReqUrl(), v);

		if (e.getStatusCode() == 200 && e.isImage()) {

			String imgName = titleMap.get(e.getReqUrl()) + ".jpg";
			imgName = imgName.replace('*', '-')
							.replace('|', '-')
							.replace('\\', '-')
							.replace(':', '-')
							.replace('"', '-')
							.replace('<', '-')
							.replace('>', '-')
							.replace('?', '-')
							.replace('/', '-');

			File f = FilesUtil.getWritableFile(new File(SAVE_DIR, imgName));
			if (!f.getAbsoluteFile().getParentFile().exists())
				f.getAbsoluteFile().getParentFile().mkdirs();
			if (f.exists() && f.length() > 0) return true;

			try (OutputStream out = new FileOutputStream(f)) {
				e.bufWriteTo(out);
			} catch (Exception ex) {
				log.error("下载失败: " + e.getCurrentURL(), ex);
				repo.remove(e.getCurrentURL());
				repo.remove(e.getReqUrl());
				return false;
			}
		}
		return true;
	}

	@Override
	public void init() throws IOException, ClassNotFoundException {
		if (saveFile.exists()) {
			try (ZipInputStream in = new ZipInputStream(new FileInputStream(saveFile))) {
				in.getNextEntry();
				ObjectInputStream oin = new ObjectInputStream(in);
				Object[] savedObj = (Object[]) oin.readObject();

				Map tRepo = (Map) savedObj[0];
				if (tRepo != null && tRepo.size() > 0)
					repo = tRepo;

				Queue<UrlCtxHolder<UrlContext>> tUnhandledSeeds = (Queue<UrlCtxHolder<UrlContext>>) savedObj[1];
				if (tUnhandledSeeds != null && tUnhandledSeeds.size() > 0) {
					seeds.clear();
					tUnhandledSeeds.stream()
									.filter(t -> this.accept(t.getUrl(), t.getCtx()))
									.forEach(t -> {
										t.setUrl(this.unEscape(t.getUrl()));
										seeds.add(t);
									});
				}

				titleMap = (Map<String, String>) savedObj[2];

				log.info("load from file: " + saveFile);
			} catch (Exception e) {
				log.error("load from file error.", e);
			}
		}

		log.info("seeds.size=" + this.seeds.size());
	}

	private String unEscape(String url) {
		return url.replace('\\', '/')
						.replace("////", "//")
						.replace("&amp;", "&")
						.replace("&lt;", "<")
						.replace("&gt;", ">")
						.replace("&quot;", "\"");
	}

	@Override
	public void onCrawlerStopped(Queue<UrlCtxHolder<UrlContext>> unhandledSeeds) {
		this.unhandledSeeds = unhandledSeeds;

		try (ZipOutputStream zo = new ZipOutputStream(new FileOutputStream(saveFile))) {
			zo.putNextEntry(new ZipEntry("store"));
			ObjectOutputStream oOut = new ObjectOutputStream(zo);
			oOut.writeObject(new Object[]{this.repo, this.unhandledSeeds, this.titleMap});
			log.info("save to file: " + saveFile);
		} catch (Exception e) {
			log.error("save unhandled seeds failed.", e);
		}
	}


	@Override
	public Stream<UrlCtxHolder<UrlContext>> getSeeds() {
		return seeds.stream();
	}

	private static void chkAndZipDir(Crawler c) {
		try {
			File dir = new File(SAVE_DIR);
			dir.mkdirs();

			if (FilesUtil.folderSize(dir) > folderSize) {
				c.pause();
				Thread.sleep(5000);

				File zipFile = FilesUtil.getWritableFile(new File(REPO_FILE));
				zipFile.getParentFile().mkdirs();
				Process process = Runtime.getRuntime().exec(
								"zip -q -r \"" + zipFile.getAbsolutePath() + "\" \"" + SAVE_DIR + "\"");
				process.waitFor();

				if (zipFile.length() > folderSize / 2) {
					FilesUtil.recurDir(dir, null, EnumSet.of(FilesUtil.HandleType.FoldersAndFiles), f -> f.delete());
					dir.delete();
				} else
					c.stop();

				c.resume();
			}
		} catch (Exception e) {
			log.error("", e);
			c.stop();
		}
	}
}


