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
//		hcc.setUserAgent(HttpClientConfig.UA_BAIDU);
		hcc.setMaxConnPerRoute(MAX_CONN_PER_ROUTE);
//		hcc.setUseProxy(true);
		hcc.setProxyHost("127.0.0.1");
		hcc.setProxyPort(8058);
//		hcc.addHeader(new BasicHeader("Cookie", "cna=xK9HC+lDbjgCAd5PAMnmGDjc; mpp=; tlut=; l=; v=0; swfstore=205783; miid=4744219378233682223; lzstat_uv=29340845793270010233|3492151@1396907; lzstat_ss=769689425_0_1413928155_3492151|2603056175_0_1413928155_1396907; linezing_session=drO8ZtuEzA6JNzUEP2ZzWd9q_1413901792473cCHQ_1; uc3=nk2=CyKw3U3YEQ%3D%3D&id2=VyT0ay3c3OU%3D&vt3=F8dATSQF8mLV2FFyafQ%3D&lg2=WqG3DMC9VAQiUQ%3D%3D; existShop=MTQxMzk4NTA4NQ%3D%3D; lgc=hongzzx; tracknick=hongzzx; sg=x47; cookie2=15956220fa438b8be54d6dbe6ad5cc96; cookie1=BYS7%2B%2BW1Djg%2F%2FRE1tUce2Gx2LnUvmXlm8W5MGWMCaWw%3D; unb=48705304; t=1aa815451a36ca826e1b316fd37b2b76; _cc_=VT5L2FSpdA%3D%3D; tg=0; _l_g_=Ug%3D%3D; _nk_=hongzzx; cookie17=VyT0ay3c3OU%3D; mt=ci=4_1; _tb_token_=fb77eb6ee4535; isg=E680DAC2976940202E3D4873144FC8D9; uc1=lltime=1413973842&cookie14=UoW28FQE5LanJQ%3D%3D&existShop=false&cookie16=VT5L2FSpNgq6fDudInPRgavC%2BQ%3D%3D&cookie21=UIHiLt3xThN%2B&tag=1&cookie15=V32FPkk%2Fw0dUvg%3D%3D; x=e%3D1%26p%3D*%26s%3D0%26c%3D0%26f%3D0%26g%3D0%26t%3D0%26__ll%3D-1%26_ato%3D0; whl=-1%260%260%260"));

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

		if (e.getStatusLine().getStatusCode() == 200 && e.isImage()) {

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

			File f = FilesUtil.getWritableFile(new File(SAVE_DIR, imgName).getAbsolutePath());
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
			new File(SAVE_DIR).mkdirs();

			if (FilesUtil.folderSize(SAVE_DIR) > folderSize) {
				c.pause();
				Thread.sleep(5000);

				File zipFile = FilesUtil.getWritableFile(REPO_FILE);
				zipFile.getParentFile().mkdirs();
				Process process = Runtime.getRuntime().exec(
								"zip -q -r \"" + zipFile.getAbsolutePath() + "\" \"" + SAVE_DIR + "\"");
				process.waitFor();

				if (zipFile.length() > folderSize / 2) {
					File dir = new File(SAVE_DIR);
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


