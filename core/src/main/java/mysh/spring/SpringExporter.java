package mysh.spring;

import com.alibaba.fastjson.JSON;
import com.google.common.io.ByteStreams;
import mysh.util.Compresses;
import mysh.util.Exps;
import mysh.util.Serializer;
import mysh.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

/**
 * spring 导出类, 方便本地测试.
 * 调 SpringExporter.proxySock 创建代理实例.
 *
 * @since 2017/08/04
 */
@Component
public class SpringExporter implements ApplicationContextAware {
	private static final Logger log = LoggerFactory.getLogger(SpringExporter.class);
	
	public static final int SERVER_PORT = 32345;
	public static Serializer SERIALIZER = Serializer.BUILD_IN;
	
	private static ApplicationContext ctx;
	private static ServerSocket server;
	
	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		SpringExporter.ctx = ctx;
		initSock();
	}
	
	private Method findMethod(Class methodClass, String methodName, Class<?>[] methodParamsTypes) throws NoSuchMethodException {
		Method method = methodClass.getDeclaredMethod(methodName, methodParamsTypes);
		method.setAccessible(true);
		return method;
	}
	
	public static class Invoke implements Serializable {
		private static final long serialVersionUID = 4147956720977499166L;
		private Class type;
		private String beanName;
		private String methodName;
		private Class<?> methodClass;
		private Class<?>[] methodParamsTypes;
		private Object[] args;
		
		public <T> Invoke(Class<T> type, String beanName,
		                  Class<?> methodClass, String methodName, Class<?>[] methodParamsTypes, Object[] args) {
			this.type = type;
			this.beanName = beanName;
			this.methodClass = methodClass;
			this.methodName = methodName;
			this.methodParamsTypes = methodParamsTypes;
			this.args = args;
		}
		
		@Override
		public String toString() {
			return "Invoke{" +
					"type=" + type +
					", beanName='" + beanName + '\'' +
					", methodName='" + methodName + '\'' +
					", methodClass=" + methodClass +
					", methodParamsTypes=" + Arrays.toString(methodParamsTypes) +
					", args=" + Arrays.toString(args) +
					'}';
		}
	}
	
	public static class Result implements Serializable {
		private static final long serialVersionUID = 6866690856377532311L;
		private Object value;
		private Class type;
		private boolean isJson = false;
		private String t;
		
		void setResult(Object result) {
			try {
				value = SERIALIZER.serialize(result);
			} catch (Exception e) {
				setJsonResult(result);
			}
		}
		
		void setJsonResult(Object result) {
			isJson = true;
			type = result.getClass();
			value = JSON.toJSONString(result, true);
		}
		
		public <T> T getResult() throws Throwable {
			if (t != null) {
				throw new RuntimeException(t);
			}
			if (isJson) {
				return (T) JSON.parseObject((String) value, type);
			} else {
				return SERIALIZER.deserialize((byte[]) value);
			}
		}
	}
	
	@SuppressWarnings("all")
	private void initSock() {
		new Thread(() -> {
			try {
				server = new ServerSocket(SERVER_PORT);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			while (true) {
				try {
					Socket socket = server.accept();
					socket.setSoTimeout(5 * 60_000);
					new Thread(() -> {
						try {
							InputStream in = socket.getInputStream();
							OutputStream out = socket.getOutputStream();
							
							while (!socket.isClosed()) {
								Invoke iv = SERIALIZER.deserialize(in);
								Result r = invoke(iv);
								SERIALIZER.serialize(r, out);
							}
						} catch (Throwable e) {
							if (Exps.isCausedBy(e, IOException.class) == null) {
								log.error("SpringExporter-invoke-error", e);
							}
						} finally {
							try {
								socket.close();
							} catch (IOException e) {
							}
						}
					}).start();
				} catch (Exception e) {
					log.error("SpringExporter-accept-error", e);
				}
			}
		}).start();
	}
	
	public Result invoke(Invoke iv) {
		Result r = new Result();
		try {
			Object bean = Strings.isNotBlank(iv.beanName) ? ctx.getBean(iv.beanName) : ctx.getBean(iv.type);
			Method method = findMethod(iv.methodClass, iv.methodName, iv.methodParamsTypes);
			Object value = method.invoke(bean, iv.args);
			r.setResult(value);
		} catch (Throwable t) {
			r.t = t.toString();
			log.error("SpringExporter-invoke-error,invoke={}", JSON.toJSONString(iv), t);
		}
		return r;
	}
	
	public static <T> T proxySock(Class<T> type) {
		return proxySock("127.0.0.1", SERVER_PORT, type, null);
	}
	
	public static <T> T proxySock(String host, int port, Class<T> type) {
		return proxySock(host, port, type, null);
	}
	
	public static <T> T proxySock(String host, int port, Class<T> type, @Nullable String beanName) {
		ThreadLocal<Socket> tt = new ThreadLocal<>();
		
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(type);
		enhancer.setCallback(
				(InvocationHandler) (o, method, args) -> {
					Socket socket = tt.get();
					if (socket == null || socket.isClosed()) {
						socket = new Socket(host, port);
						tt.set(socket);
					}
					OutputStream out = socket.getOutputStream();
					InputStream in = socket.getInputStream();
					try {
						SERIALIZER.serialize(
								new Invoke(type, beanName,
										method.getDeclaringClass(), method.getName(), method.getParameterTypes(),
										args), out);
						Result r = SERIALIZER.deserialize(in);
						return r.getResult();
					} catch (Exception e) {
						socket.close();
						throw e;
					}
				});
		return (T) enhancer.create();
	}
	
	/**
	 * @param useHeaderOrBody 交互数据可以放在 req/rsp 的 header 或 body.
	 *                        注意使用 header 时可能有大小限制.
	 *                        数据放在 header 时, 请求/结果 header 为 invoke/result
	 */
	public static <T> T proxyHttp(String url, @Nullable Map<String, String> reqHeaders,
	                              Class<T> type, @Nullable String beanName,
	                              boolean useHeaderOrBody) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(type);
		enhancer.setCallback(
				(InvocationHandler) (o, method, args) -> {
					URLConnection conn = null;
					try {
						URL u = new URL(url);
						conn = u.openConnection();
						if (reqHeaders != null) {
							reqHeaders.forEach(conn::addRequestProperty);
						}
						String invokeStr = objToBase64(
								new Invoke(type, beanName,
										method.getDeclaringClass(), method.getName(), method.getParameterTypes(),
										args));
						if (useHeaderOrBody) {
							// put invokeStr to request header
							conn.addRequestProperty("invoke", invokeStr);
							conn.connect();
							Result r = base64ToObj(conn.getHeaderField("result"));
							return r.getResult();
						} else {
							// put invokeStr to request body
							conn.setDoOutput(true);
							conn.setRequestProperty("Content-Type", "application/octet-stream");
							conn.connect();
							try (OutputStream os = conn.getOutputStream()) {
								os.write(invokeStr.getBytes(StandardCharsets.UTF_8));
							}
							try (InputStream in = conn.getInputStream()) {
								Result r = base64ToObj(in);
								return r.getResult();
							}
						}
					} finally {
						if (conn instanceof HttpURLConnection) {
							((HttpURLConnection) conn).disconnect();
						}
					}
				});
		return (T) enhancer.create();
	}
	
	public String handleHttpReq(String invokeStr) {
		SpringExporter.Result r = invoke(SpringExporter.base64ToObj(invokeStr));
		return SpringExporter.objToBase64(r);
	}
	
	private static String objToBase64(Object obj) {
		return Base64.getEncoder().encodeToString(Compresses.compressZip(SERIALIZER.serialize(obj), 9));
	}
	
	private static <T> T base64ToObj(String str) {
		return SERIALIZER.deserialize(Compresses.decompressZip(Base64.getDecoder().decode(str.trim())));
	}
	
	private static <T> T base64ToObj(InputStream is) throws IOException {
		byte[] buf = ByteStreams.toByteArray(is);
		String b64 = new String(buf).trim();
		return SERIALIZER.deserialize(Compresses.decompressZip(Base64.getDecoder().decode(b64)));
	}
}
