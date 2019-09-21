[![Latest release](https://img.shields.io/github/release/myshzzx/mlib.svg)](https://github.com/myshzzx/mlib/releases/latest)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.myshzzx/mlib/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.myshzzx/mlib/)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

```xml
<dependency>
    <groupId>com.github.myshzzx</groupId>
    <artifactId>mlib-core</artifactId>
</dependency>
```

这是一个原创大杂烩。

有时，我只想用 AES 加密一串文本；有时，我只是想取一个网页的内容。。。有 apache 强大的 commons 包，但还得写大段大段的代码，灵活，但啰嗦。放弃一些灵活性封装一下，简单粗暴地两行代码搞定 90% 的任务。

在代码的世界里每击败一只小强，就会得到一个装备，所以这里算武器库了。

下面是一些块头稍大的组件

* [Crawler 网页爬虫](#crawler)
* [DynamicSql 动态sql拼接(流式)](#dynasql)
* [HttpClientAssist 简化版 httpclient](#hca)
* [cluster 分布式计算框架](#cluster)

----

## <a name='crawler'></a>Crawler 网页爬虫
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

有个要注意的地方, 就是默认的 url 分类器会自动调速, 没有流控的小站很容易被爬崩, 小伙伴们要手下留情。
关于调速机制，见 ```mysh.crawler2.Crawler.UrlClassifierAdjuster#analyze```

## <a name='dynasql'></a>DynamicSql 动态sql拼接(流式)
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


## <a name='hca'></a>HttpClientAssist 简化版 httpclient
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
	ue.getStatusCode(); // 200
}
```


# <a name='cluster'></a>cluster 分布式计算框架
闲置机器发挥余热的时候到了。
### 特点
* 面向计算密集的应用
* 无中心，自组织，无单点依赖，通过内网广播进行节点发现
* 无外系统依赖，一个节点即可组成集群，并支持动态横向扩展
* 支持独立部署和嵌入式部署

### 运行机制
* 集群内有两种角色：Master 和 Worker
* Master 负责任务调度和文件同步，Worker 负责任务执行
* 所有节点都是 Worker，某个节点身兼 Master
* Master 由所有节点公认的规则确定（启动时间最早且 IP 地址字串的字典序最靠前）
* Master 和 Worker 保持心跳连接，用于存活检测和状态更新
* 除了节点发现，其他通信采用 RPC
* RPC 基于 Thrift 和 fast-serialization

### 自组织
* 按照规则，某台机器 “天生” 就是 Master (按 启动时间+IP 字串的字典序, 序号最小的为 Master)
* 节点通过广播包问答的方式发现其他节点及确认 Master，广播包内包含自身信息
* 节点启动时进入 “Master 确认流程”，广播询问 “谁是 Master”
	1. 超时没有收到回应，将自己设定为 Master，并广播 “我是 Master”
	2. 超时内收到回应，对方宣称自己是 Master，对比信息发现：
		+ 自己更有资格当 Master，将自己设定为 Master，并广播 “我是 Master”
		+ 对方更有资格当 Master，什么也不做
* Master 发现 Worker 心跳丢失时，将 Worker 未提交的任务重新发给其他 Worker 执行
* Worker 发现 Master 心跳丢失时，重新进入 “Master 确认流程”

### 任务调度
* Master 维护 Worker 的状态，并由 Worker 的心跳更新状态
* Master 根据 Worker 的状态计算它的负载，维护一个负载值的优先队列，以此进行任务调度
* Master 将任务分解为子任务交由 Worker 执行，Worker 执行完成后将结果提交 Master，Master 合并结果并返回给用户
* Master 的任务超时或 Worker 的子任务失败将导致任务取消，Worker 的子任务是否超时或失败由用户代码控制

### 部署
* 独立
```shell
cd cluster
mvn package
cd target/cluster-dist
./startCluster.sh
```
* 嵌入式
```java
new ClusterNode(new ClusterConf());
// ClusterNode 只启动 daemon 线程，需要保留非 daemon 线程来阻止 vm 退出
new CountDownLatch(1).await();
```

### 客户端连接
```java
// 连接到集群
ClusterClient c = new ClusterClient(cmdPort);
```

### 集群任务
#### 管理任务
```java
// 取消任务
mysh.cluster.ClusterClient.mgrCancelTask

// 获取 Worker 节点状态
mysh.cluster.ClusterClient.mgrGetWorkerStates

// 重启节点
mysh.cluster.ClusterClient.mgrShutdownRestart

// 更新所有节点配置
mysh.cluster.ClusterClient.mgrUpdateConf

// 上传(更新)文件到集群
mysh.cluster.ClusterClient.mgrUpdateFile
```
#### 用户任务
* 用户将应用打成 jar 包上传到集群上某个 namespace 后，即可执行分布式任务。
* 不同 namespace 使用各自的类加载器，相互隔离
```java
// 执行任务
mysh.cluster.ClusterClient#runTask

// 任务描述：对二维数组中所有元素求和
new IClusterUser<float[][], float[][], Float, Float>() {
	// Master 任务分解
	public SubTasksPack<float[][]> fork(float[][] task, String masterNode, List<String> workerNodes) {
		log.info("begin to fork sumUser task.==");

		float[][][] r = split(task, workerNodes.size());

		log.info("fork sumUser task end.==");
		return pack(r, null);
	}

	// 子任务结果类型
	public Class<Float> getSubResultType() {
		return Float.class;
	}

	// Worker 执行子任务
	public Float procSubTask(float[][] subTask, int timeout) throws InterruptedException {
		log.info("begin to process sumUser subTask.--");
		Thread.sleep(5000);
		float sum = 0;
		for (float[] s : subTask) {
			for (float f : s) {
				sum += f;
			}
		}
		log.info("process sumUser subTask end.--");
		return sum;
	}

	// Master 子任务结果合并
	public Float join(String masterNode, String[] nodes, Float[] subResult) {
		return Arrays.stream(subResult)
						.reduce((a, b) -> a + b).get();
	}
};
```

### 权限
* 集群上的用户程序权限受 jvm 安全沙箱限制，权限取决于文件上传时的类型（su：superuser，拥有节点主机所有权限；user：普通用户，拥有受限权限）。
详见[配置文件](https://github.com/myshzzx/mlib/blob/master/cluster/dist/main/core/permission.txt)
* 受限的用户程序可以通过`mysh.cluster.IClusterUser` 的 `protected` 方法访问系统资源

### 更多示例
详见[test目录](https://github.com/myshzzx/mlib/tree/master/cluster/src/test/java/mysh/cluster)

