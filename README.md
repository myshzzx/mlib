# 那些年玩过的组件和封装过的代码
这是一个原创大杂烩。

有时，我只想用 AES 加密一串文本；有时，我只是想取一个网页的内容。。。有 apache 强大的 commons 包，但还得写大段大段的代码，灵活，但啰嗦。放弃一些灵活性封装一下，简单粗暴地两行代码搞定 90% 的任务。

在代码的世界里每击败一只小强，就会得到一个装备，所以这里算武器库了。

----
下面是一些块头稍大的组件

* [Crawler 网页爬虫](#crawler)
* [DynamicSql 动态sql拼接(流式)](#dynasql)
* [HttpClientAssist 简化版 httpclient](#hca)
* [cluster 分布式计算框架](#cluster)


## <a name='crawler'></a>网页爬虫
看到牛人的博客想一锅端了怎么破？wget？那只是对源站做了个镜像而已，而且面对下载后的一堆数字命名的文件会让人抓狂的。

假如有个爬虫允许我
```java
interface CrawlerSeed<CTX extends UrlContext> extends Serializable {
	// 准备工作
	void init();

	// 设定起点
	Stream<UrlCtxHolder<CTX>> getSeeds();

	// 决定要爬取的页面。需要自己记录已抓取的 url，可以用 ConcurrentHashMap 或 BloomFilter
	boolean accept(String url, CTX ctx);

	// 决定要提取 url 的页面
	boolean needToDistillUrls(HttpClientAssist.UrlEntity ue, CTX ctx);

	// 提取 url 后可以处理上下文
	Stream<UrlCtxHolder<CTX>> afterDistillingUrls(
	          HttpClientAssist.UrlEntity parentUe, CTX parentCtx, Stream<String> distilledUrls)

	// 处理取到的 url 资源数据
	boolean onGet(HttpClientAssist.UrlEntity ue, CTX ctx);

	// 被停止时的处理。如保存已抓取的页面信息。
	void onCrawlerStopped(Queue<UrlCtxHolder<CTX>> unhandledTasks);

	...
}
```
就好了。示例见[test目录](https://github.com/myshzzx/mlib/tree/master/core/src/test/java/mysh/crawler2)


## <a name='dynasql'></a>动态sql拼接(流式)
控制器丢过来一个表单对象，要根据它动态地拼 sql 也是个令人恼火的活，要处理各种空值引号啥的，一不小心就出错。能像写 sql 一样写 java 或许还会好点。
```java
// form = {name:"mysh", org:"sd"}
DynamicSql sql = DynamicSql.create();
sql
   // 二元表达式，如果你觉得这样更直观
   .bi("name", "!=", form.getName())

   // 或者简单用 eq（equal）
   .eq("org", form.getOrg())

   // null 或空白值的拼接语句会被跳过
   .notLike("label", form.getLabel())

   // 可以随时设置字段别名
   .setTableAlias("t")

   // 条件为 true 才拼接
   .on(form.isOK()).in("age", 10, 20, 30)

   .orderBy("no");

sql.getCondStr(); // 1=1 AND NAME != :name AND ORG = :org AND T.AGE IN (10,20,30) ORDER BY NO
sql.getParamMap(); // {name=mysh, org=sd}
```
示例见[test目录](https://github.com/myshzzx/mlib/tree/master/core/src/test/java/mysh/codegen)


## <a name='hca'></a>简化版 httpclient
爬虫项目的衍生物，httpclient component 的封装版。访问个 url 就这么简单。

proxy/header/params 啥的就不说了，详见[test目录](https://github.com/myshzzx/mlib/tree/master/core/src/test/java/mysh/net/httpclient)。
```java
HttpClientAssist hca = new HttpClientAssist(new HttpClientConfig());

// 用 try-resource 释放资源
try(HttpClientAssist.UrlEntity ue = hca.access("https://hc.apache.org/")){
	// 取页面文本
	String html = ue.getEntityStr();

	// 页面数据写入文件
	ue.bufWriteTo(fileOutputStream);

	// 响应状态码
	ue.getStatusLine().getStatusCode(); // 200
}
```


# <a name='cluster'></a>cluster 分布式计算框架


