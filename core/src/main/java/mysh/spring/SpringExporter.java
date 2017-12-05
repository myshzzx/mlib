package mysh.spring;

import com.alibaba.fastjson.JSON;
import mysh.util.Serializer;
import org.springframework.beans.BeansException;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * spring 导出类, 方便本地测试.
 * 调 SpringExporter.proxySock 创建代理实例.
 */
//@Component
public class SpringExporter implements ApplicationContextAware {

	public static final int SERVER_PORT = 12345;
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

	private static class Invoke implements Serializable {
		private static final long serialVersionUID = 4147956720977499166L;
		private Class type;
		String methodName;
		private Class<?> methodClass;
		private Class<?>[] methodParamsTypes;
		Object[] args;

		public <T> Invoke(Class<T> type, Class<?> methodClass, String methodName, Class<?>[] methodParamsTypes, Object[] args) {
			this.type = type;
			this.methodClass = methodClass;
			this.methodName = methodName;
			this.methodParamsTypes = methodParamsTypes;
			this.args = args;
		}
	}

	private static class Result implements Serializable {
		private static final long serialVersionUID = 6866690856377532311L;
		Object value;
		Class type;
		boolean isJson = false;
		Throwable t;

		void setResult(Object result) {

			if (result == null || result instanceof Serializable) {
				try {
					value = Serializer.buildIn.serialize((Serializable) result);
				} catch (Exception e) {
					setJsonResult(result);
				}
			} else {
				setJsonResult(result);
			}
		}

		void setJsonResult(Object result) {
			isJson = true;
			type = result.getClass();
			value = JSON.toJSONString(result, true);
		}

		<T> T getResult() throws Throwable {
			if (t != null) {
				throw t;
			}
			if (isJson) {
				return (T) JSON.parseObject((String) value, type);
			} else {
				return Serializer.buildIn.deserialize((byte[]) value);
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
					new Thread(() -> {
						ObjectOutputStream out = null;
						try {
							ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
							out = new ObjectOutputStream(socket.getOutputStream());

							Invoke iv = (Invoke) in.readObject();

							Object bean = ctx.getBean(iv.type);
							Method method = findMethod(iv.methodClass, iv.methodName, iv.methodParamsTypes);

							Result r = new Result();
							try {
								Object value = method.invoke(bean, iv.args);
								r.setResult(value);
							} catch (Throwable t) {
								r.t = t;
								t.printStackTrace();
							}
							out.writeObject(r);
							out.close();
						} catch (Throwable e) {
							e.printStackTrace();
						} finally {
							try {
								socket.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}).start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public static <T> T proxySock(Class<T> type) {
		return proxySock("127.0.0.1", SERVER_PORT, type);
	}

	public static <T> T proxySock(String host, int port, Class<T> type) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(type);
		enhancer.setCallback(
				(InvocationHandler) (o, method, args) -> {
					Socket socket = new Socket(host, port);
					ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
					out.writeObject(
							new Invoke(type,
									method.getDeclaringClass(), method.getName(), method.getParameterTypes(),
									args));
					out.flush();
					ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
					Result r = (Result) in.readObject();
					return r.getResult();
				});
		return (T) enhancer.create();
	}
}




