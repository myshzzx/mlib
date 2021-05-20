package mysh.crawler2.app;

import com.google.common.net.HttpHeaders;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import mysh.codegen.CodeUtil;
import mysh.collect.Colls;
import mysh.crawler2.Crawler;
import mysh.crawler2.CrawlerSeed;
import mysh.crawler2.UrlContext;
import mysh.crawler2.UrlCtxHolder;
import mysh.net.httpclient.HttpClientAssist;
import mysh.net.httpclient.HttpClientConfig;
import mysh.os.HotKeysGlobal;
import mysh.sql.sqlite.SqliteDB;
import mysh.tulskiy.keymaster.common.HotKeyListener;
import mysh.util.Strings;
import mysh.util.Try;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 爬单站, 页面数据存储db, 可启动本地web服务重现爬取的单站镜像
 *
 * @author mysh
 * @since 2021-05-15
 */
@Slf4j
public abstract class SiteCrawler<CTX extends UrlContext> implements CrawlerSeed<CTX>, Closeable {
	private static final long serialVersionUID = -537294199713566818L;
	
	@Data
	@Accessors(chain = true)
	protected static class SiteConfig implements Serializable {
		private static final long serialVersionUID = 8100792219369925962L;
		
		private String name, siteRoot, dbFile;
		private boolean dbUseLock = false;
		private int dbMmapSize = 134217728, crawlRatePerMin = 60, crawlerThreadPoolSize = 5;
		
		@Nullable
		private transient HttpClientConfig hcc;
		@Nullable
		private transient ProxySelector proxySelector;
	}
	
	@Data
	@Accessors(chain = true)
	protected static class PageInfo implements Serializable {
		private static final long serialVersionUID = 8100792219369925962L;
		
		private Map<String, String> header;
		private byte[] content;
	}
	
	protected final SiteConfig config;
	private final SqliteDB db;
	protected final SqliteDB.KvDAO pageDAO;
	protected final SqliteDB.KvDAO configDAO;
	
	protected SiteCrawler() {
		config = getConfig();
		db = new SqliteDB(Paths.get(config.dbFile), config.dbUseLock, config.dbMmapSize);
		String table = CodeUtil.camel2underline(config.name).toLowerCase();
		pageDAO = db.genKvDAO(table + "_pages", true, false);
		configDAO = db.genKvDAO(table + "_config", true, false);
	}
	
	@Override
	public void close() throws IOException {
		if (db != null)
			db.close();
	}
	
	protected abstract SiteConfig getConfig();
	
	@Override
	public Stream<UrlCtxHolder<CTX>> getSeeds() {
		SqliteDB.Item unhandledTasksItem = configDAO.itemByKey("unhandledTasks");
		if (unhandledTasksItem != null && dbItemValid(unhandledTasksItem)) {
			Collection<UrlCtxHolder<CTX>> tasks = unhandledTasksItem.getValue();
			if (Colls.isNotEmpty(tasks))
				return tasks.stream();
		}
		return Stream.of(UrlCtxHolder.of(config.getSiteRoot()));
		
	}
	
	/**
	 * 将 url 或 uri 转为真实存储的 key uri, 可重写以去除不需要的参数, 如时间戳
	 */
	protected String getReqUri(String reqUrl) {
		String uri = reqUrl;
		if (reqUrl.startsWith(config.getSiteRoot()))
			uri = reqUrl.substring(config.getSiteRoot().length());
		if (uri.length() == 0 || uri.charAt(0) != '/')
			uri = '/' + uri;
		
		int sharpIdx = uri.indexOf('#');
		return sharpIdx > 0 ? uri.substring(0, sharpIdx) : uri;
	}
	
	/**
	 * 检查存储的 item 是否有效, 无效则视为不存在
	 */
	protected boolean dbItemValid(@Nonnull SqliteDB.Item item) {
		return true;
	}
	
	@Override
	public boolean onGet(HttpClientAssist.UrlEntity ue, CTX urlContext) {
		try {
			if (ue.getStatusCode() == 200) {
				String uri = getReqUri(ue.getReqUrl());
				pageDAO.save(uri, new PageInfo()
						.setHeader(Colls.ofHashMap(
								HttpHeaders.CONTENT_TYPE, ue.getRspHeader(HttpHeaders.CONTENT_TYPE)
						))
						.setContent(ue.getEntityBuf())
				);
				return true;
			} else
				return ue.getStatusCode() == 404;
		} catch (Throwable t) {
			log.error("get page fail: {}", ue.getReqUrl(), t);
			return false;
		}
	}
	
	@Override
	public void onCrawlerStopped(Collection<UrlCtxHolder<CTX>> unhandledTasks) {
		configDAO.save("unhandledTasks", unhandledTasks);
	}
	
	/**
	 * 启动爬虫并等待结束
	 *
	 * @return
	 */
	public Crawler<CTX> startCrawler() throws Exception {
		Crawler<CTX> crawler = new Crawler<>(this,
				config.hcc, config.proxySelector, config.crawlRatePerMin, config.crawlerThreadPoolSize)
				.start();
		log.warn("crawler started, {}, config={}", getClass(), config);
		return crawler;
	}
	
	/**
	 * 启动爬虫并等待结束. 可设置全局快捷键来停止, 放空则不设置
	 *
	 * @param globalStopHotkey e.g. <b>alt shift S</b>
	 */
	public void startCrawlerAndWait(@Nullable String globalStopHotkey) throws Exception {
		Crawler<CTX> crawler = startCrawler();
		HotKeyListener stopAction = e -> crawler.stop();
		if (Strings.isNotBlank(globalStopHotkey)) {
			HotKeysGlobal.registerKeyListener(globalStopHotkey, stopAction);
			log.warn("press {} to stop crawler", globalStopHotkey);
		}
		try {
			crawler.waitForStop();
		} finally {
			if (Strings.isNotBlank(globalStopHotkey)) {
				HotKeysGlobal.unregisterKeyListener(globalStopHotkey, stopAction);
			}
		}
	}
	
	/**
	 * 启动站点本地web服务, 本地未存储的页面将被重爬存储
	 */
	public Closeable startWebServer(int port) throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
		
		ThreadPoolExecutor httpExec = new ThreadPoolExecutor(
				Runtime.getRuntime().availableProcessors(),
				64, 1, TimeUnit.MINUTES,
				new SynchronousQueue<>(), r -> {
			Thread t = new Thread(r, getClass().getSimpleName() + "-http-server");
			t.setDaemon(true);
			return t;
		});
		httpExec.allowCoreThreadTimeOut(true);
		
		HttpClientAssist hca = new HttpClientAssist(config.hcc, config.proxySelector);
		HttpHandler httpHandler = exchange -> {
			URI uri = exchange.getRequestURI();
			String uriStr = uri.toString();
			String reqUri = getReqUri(uriStr);
			SqliteDB.Item item = pageDAO.itemByKey(reqUri);
			
			// 将 item 返回请求端
			Try.ExpRunnable<IOException> sendPageInfo = () -> {
				PageInfo pageInfo = item.getValue();
				
				Headers rspHeaders = exchange.getResponseHeaders();
				for (Map.Entry<String, String> rspHeader : pageInfo.header.entrySet()) {
					rspHeaders.set(rspHeader.getKey(), rspHeader.getValue());
				}
				exchange.sendResponseHeaders(200, 0);
				try (InputStream in = exchange.getRequestBody();
				     OutputStream out = exchange.getResponseBody()) {
					out.write(pageInfo.content);
				}
			};
			
			// 从远程源加载页面
			Headers requestHeaders = exchange.getRequestHeaders();
			Try.ExpCallable<HttpClientAssist.UrlEntity, Throwable> fetchPage = () -> {
				Map<String, String> headers = new HashMap<>();
				for (Map.Entry<String, List<String>> reqHeader : requestHeaders.entrySet()) {
					headers.put(reqHeader.getKey(), reqHeader.getValue().get(0));
				}
				headers.remove(HttpHeaders.HOST);
				return hca.access(config.siteRoot + uriStr, headers);
			};
			
			if (item != null) {
				// 有数据直接返回
				sendPageInfo.run();
				if (!dbItemValid(item)) {
					// 发现失效的数据触发一次异步加载
					httpExec.execute(() -> {
						try (HttpClientAssist.UrlEntity ue = fetchPage.call()) {
							if (ue.getStatusCode() == 200)
								onGet(ue, null);
						} catch (Throwable t) {
							log.error("load page async fail: {}", uriStr, t);
						}
					});
				}
			} else {
				// 无数据开启同步加载
				try (HttpClientAssist.UrlEntity ue = fetchPage.call()) {
					if (ue.getStatusCode() == 200)
						onGet(ue, null);
					exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, ue.getRspHeader(HttpHeaders.CONTENT_TYPE));
					exchange.sendResponseHeaders(ue.getStatusCode(), 0);
					try (InputStream in = exchange.getRequestBody();
					     OutputStream out = exchange.getResponseBody()) {
						out.write(ue.getEntityBuf());
					}
				} catch (Throwable t) {
					log.error("load page failed, {}, exp={}", uriStr, t.toString());
					exchange.sendResponseHeaders(500, 0);
					try (InputStream in = exchange.getRequestBody();
					     OutputStream out = exchange.getResponseBody()) {
						t.printStackTrace(new PrintWriter(out));
					}
				}
			}
		};
		
		server.createContext("/", httpHandler);
		server.setExecutor(httpExec);
		server.start();
		
		log.warn("SiteCrawler-WebServer started in http://localhost:{}/, {}, config={}", port, getClass(), config);
		return () -> {
			hca.close();
			server.stop(0);
		};
	}
}
