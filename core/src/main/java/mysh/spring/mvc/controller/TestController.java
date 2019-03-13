package mysh.spring.mvc.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Charsets;
import mysh.collect.Colls;
import mysh.spring.SpringExporter;
import mysh.util.Serializer;
import mysh.util.Strings;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 后门.
 * <pre>
 * 1. 远程调用bean: {@link #invoke} (配合 {@link SpringExporter#proxyHttp})
 * 2. 执行任意代码: {@link #compileAndExecute} (配合 {@link #compileAndExecuteHelper})
 *      2.1 终止执行线程: {@link #stopThread} (配合 {@link #isKeepRunning}, 代码最好能处理中断异常)
 *      2.2 全局执行标记: {@link #stopTest} {@link #resumeTest}
 * </pre>
 *
 * @since 2017/11/03
 */
public class TestController {
	private static final Logger log = LoggerFactory.getLogger(TestController.class);
	public static Serializer SERIALIZER = Serializer.BUILD_IN;

	@Autowired
	private SpringExporter springExporter;

	public static volatile boolean testFlag = true;

	// @ResourceMapping(value = "stopTest")
	public void stopTest() throws IOException {
		testFlag = false;
	}

	// @ResourceMapping(value = "resumeTest")
	public void resumeTest() throws IOException {
		testFlag = true;
	}

	private static Set<Thread> interruptFlags = Collections.newSetFromMap(new ConcurrentHashMap<>());

	/**
	 * check whether current thread should keep running
	 */
	public static boolean isKeepRunning() {
		// clear interrupt flag
		return !Thread.interrupted() && !interruptFlags.remove(Thread.currentThread()) && testFlag;
	}

	/**
	 * interrupt specified thread.
	 * if the thread ignore the interrupt flag (not invoke {@link #isKeepRunning()}) in 10s, it will be stopped.
	 * 注意处理中断异常.
	 */
	// @ResourceMapping(value = "stopThread")
	public void stopThread(/*@RequestParam(name = "tid")*/ long tid) throws IOException, InterruptedException {
		Thread t = Thread.getAllStackTraces().entrySet().stream()
				.filter(e -> e.getKey().getId() == tid)
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(null);
		if (t == null) {
			return;
		}

		t.interrupt();
		interruptFlags.add(t);
		log.info("thread-to-be-interrupted,tid={},name={}", t.getId(), t.getName());
		Thread.sleep(10_000);
		if (interruptFlags.remove(t) && t.isAlive()) {
			// interrupt flag is ignored within 10 seconds
			log.info("thread-to-be-stopped,tid={},name={}", t.getId(), t.getName());
			t.stop();
		}
	}

	private static Pattern packageExp = Pattern.compile("package ([\\w\\.]+?);");
	private static Pattern classExp = Pattern.compile("public class (\\w+)");

	/**
	 * 编译类并执行指定方法, 返回结果.
	 * 包名类名任意, 提交的包名类名通过正则方式获取, 放在前面的注释和字符串变量要注意.
	 * slf4j 日志无法正常输出, 可用控制台输出, 在 tomcat_stdout.log 日志里查看
	 *
	 * @param code 一个带 executeMethod 无参实例方法的类, 方法返回值将返回给前端
	 */
	// @ResourceMapping(value = "compileAndExecute")
	public Map compileAndExecute(HttpServletRequest request,
	                             @RequestParam(name = "code") String code,
	                             @RequestParam(name = "executeMethod", defaultValue = "execute") String executeMethod
	) throws IOException {
		long tid = Thread.currentThread().getId();
		// if (System.currentTimeMillis() > BackDoorSwitch.TIME_LIMIT) {
		//     return null;
		// }
		// Operator operator = UserUtils.fromUic(request);
		// log.info("compileAndExecute-exec:{},code={},tid={}", operator, code, tid);

		try {
			Class<?> c = Class.forName("com.taobao.eagleeye.RuntimeJavaCompiler");
			Method m = c.getDeclaredMethod("compile", String.class, String.class, ClassLoader.class);
			m.setAccessible(true);
			String name = getClassName(code);
			ClassLoader cl = (ClassLoader) m.invoke(null, name, code, getClass().getClassLoader());

			Class<?> cc = cl.loadClass(name);
			Method execute = cc.getDeclaredMethod(executeMethod);
			execute.setAccessible(true);
			Object co = cc.newInstance();

			if (execute.getReturnType() == Void.class) {
				AtomicLong ntid = new AtomicLong();
				CountDownLatch cdl = new CountDownLatch(1);
				new Thread(() -> {
					try {
						ntid.set(Thread.currentThread().getId());
						cdl.countDown();
						log.info("compileAndExecute-in-new-thread,newTid=" + ntid.get());
						execute.invoke(co);
					} catch (Exception e) {
						log.error("compileAndExecute-err", e);
					}
				}, "compileAndExecute-" + tid).start();
				cdl.await();
				return Colls.ofHashMap("newTid", ntid.get());
			} else {
				Object result = execute.invoke(co);
				return Colls.ofHashMap("result", result);
			}
		} catch (Exception e) {
			log.error("compileAndExecute-err", e);
			return Colls.ofHashMap("result", e.toString());
		}
	}

	private String getClassName(String code) {
		Matcher pm = packageExp.matcher(code);
		Matcher cm = classExp.matcher(code);
		if (pm.find() && cm.find()) {
			return pm.group(1) + "." + cm.group(1);
		} else {
			throw new RuntimeException("className-cannot-be-parsed:" + code);
		}
	}

	/**
	 * <ul>
	 * <li>提交一个java类到服务端编译执行, 由调用方指定要执行的方法.</li>
	 * <li>文件名随意, 文件内容最好是ide格式化好的, 里面要用的bean需要用SpringContextUtil.getBean 的方式取,
	 * 要注意的是提交的类名不要已经存在于服务器上, 否则编译后找类会找到服务器上的那个</li>
	 * <li>日志打不出来, 想看日志请 tailf tomcat_stdout.log</li>
	 * <li>方法必须是无参实例方法. 无返回值时, 接口会立刻返回当前执行线程的id, 可用于执行控制;
	 * 有返回值时此调用会等待返回值, 但如果执行时间太长http超时就等不到结果了, 但程序会执行到结束.</li>
	 * <li>有外部控制入口, 详见TestRpc代码. 代码中需要使用TestRpc#isKeepRunning来配合
	 * <ul>
	 * <li>test/stopTest.json 全局停止标记</li>
	 * <li>test/resumeTest.json 全局恢复标记</li>
	 * <li>test/stopThread.json?tid=123 中断执行线程</li>
	 * </ul></li>
	 * </ul>
	 */
	public static JSONObject compileAndExecuteHelper(
			String url, String lineSepHeaders, String file, String executeMethod) throws IOException {
		if (Strings.isBlank(url) || Strings.isBlank(file) || Strings.isBlank(executeMethod)) {
			throw new IllegalArgumentException();
		}
		OkHttpClient client = new OkHttpClient.Builder().build();
		Request.Builder rb = new Request.Builder().url(url);
		if (lineSepHeaders != null) {
			String[] lines = lineSepHeaders.split("[\\r\\n]+");
			for (String line : lines) {
				line = line.trim();
				if (line.length() > 0) {
					String[] hs = line.split(":", 2);
					rb.addHeader(hs[0], hs[1]);
				}
			}
		}
		String code = new String(Files.readAllBytes(Paths.get(file)), Charsets.UTF_8);
		rb.post(new FormBody.Builder()
				.add("code", code)
				.add("executeMethod", executeMethod)
				.build());
		Call call = client.newCall(rb.build());
		try (Response rsp = call.execute()) {
			return JSON.parseObject(rsp.body().string(), JSONObject.class);
		}
	}

	/**
	 * 调用bean方法
	 */
	// @ResourceMapping(value = "invoke")
	public Object invoke(HttpServletRequest request) {
		// Operator op = UserUtils.fromUic(request);
		// if (System.currentTimeMillis() > BackDoorSwitch.TIME_LIMIT) {
		//     return null;
		// }
		try {
			String ivStr = request.getHeader("invoke");
			SpringExporter.Invoke iv = SERIALIZER.deserialize(Base64.getDecoder().decode(ivStr));
			// log.info("TestRpc-invoke-iv,op={},iv={}", op, iv);
			SpringExporter.Result r = springExporter.invoke(iv);
			return Base64.getEncoder().encodeToString(SERIALIZER.serialize(r));
		} catch (Exception e) {
			log.error("TestRpc-invoke-err", e);
			return "";
		}
	}
}
