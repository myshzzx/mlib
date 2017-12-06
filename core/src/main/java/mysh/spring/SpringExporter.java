package mysh.spring;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import mysh.util.Serializer;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;

/**
 * spring 导出类, 方便本地测试.
 * 调 SpringExporter.proxySock 创建代理实例.
 *
 * @author 凯泓(zhixian.zzx@alibaba-inc.com)
 * @since 2017/08/04
 */
// @Component
public class SpringExporter implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(SpringExporter.class);

    public static final int SERVER_PORT = 12345;
    public static Serializer SERIALIZER = Serializer.buildIn;

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
        private Throwable t;

        void setResult(Object result) {

            if (result == null || result instanceof Serializable) {
                try {
                    value = SERIALIZER.serialize((Serializable) result);
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

        public <T> T getResult() throws Throwable {
            if (t != null) {
                throw t;
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
                            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

                            while (!socket.isClosed()) {
                                Invoke iv = (Invoke) in.readObject();
                                Result r = invoke(iv);
                                out.writeObject(r);
                                out.flush();
                            }
                        } catch (Throwable e) {
                            if (!(e instanceof IOException)) {
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

    public Result invoke(Invoke iv) throws NoSuchMethodException {
        Object bean = iv.beanName != null ? ctx.getBean(iv.beanName) : ctx.getBean(iv.type);
        Method method = findMethod(iv.methodClass, iv.methodName, iv.methodParamsTypes);

        Result r = new Result();
        try {
            Object value = method.invoke(bean, iv.args);
            r.setResult(value);
        } catch (Throwable t) {
            r.t = t;
            log.error("{}-invoke-error,invoke={}", SpringExporter.class.getName(), JSON.toJSONString(iv), t);
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
        ThreadLocal<Triple<Socket, ObjectInputStream, ObjectOutputStream>> tt =
                ThreadLocal.withInitial(() -> Triple.of(null, null, null));

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(type);
        enhancer.setCallback(
                (InvocationHandler) (o, method, args) -> {
                    Triple<Socket, ObjectInputStream, ObjectOutputStream> tsoo = tt.get();
                    Socket socket = tsoo.getLeft();
                    ObjectOutputStream out = tsoo.getRight();
                    ObjectInputStream in = tsoo.getMiddle();
                    if (socket == null || socket.isClosed()) {
                        socket = new Socket(host, port);
                        out = new ObjectOutputStream(socket.getOutputStream());
                        in = new ObjectInputStream(socket.getInputStream());
                        tt.set(Triple.of(socket, in, out));
                    }
                    out.writeObject(
                            new Invoke(type, beanName,
                                    method.getDeclaringClass(), method.getName(), method.getParameterTypes(),
                                    args));
                    out.flush();
                    Result r = (Result) in.readObject();
                    return r.getResult();
                });
        return (T) enhancer.create();
    }

    public static <T> T proxyHttp(String url, String lineSepHeaders, Class<T> type) {
        return proxyHttp(url, lineSepHeaders, type, null);
    }

    private static CloseableHttpClient hc = HttpClients.createDefault();

    public static <T> T proxyHttp(String url, String lineSepHeaders, Class<T> type, @Nullable String beanName) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(type);
        enhancer.setCallback(
                (InvocationHandler) (o, method, args) -> {
                    Invoke iv = new Invoke(type, beanName,
                            method.getDeclaringClass(), method.getName(), method.getParameterTypes(),
                            args);

                    HttpUriRequest req = new HttpGet(url);
                    req.setHeader("invoke", Base64.getEncoder().encodeToString(SERIALIZER.serialize(iv)));
                    if (lineSepHeaders != null) {
                        String[] lines = lineSepHeaders.split("[\\r\\n]+");
                        for (String line : lines) {
                            line = line.trim();
                            if (line.length() > 0) {
                                String[] hs = line.split(":", 2);
                                req.setHeader(hs[0], hs[1]);
                            }
                        }
                    }
                    try (CloseableHttpResponse rsp = hc.execute(req)) {
                        JSONObject rj = JSON.parseObject(rsp.getEntity().getContent(), JSONObject.class);
                        Result r = SERIALIZER.deserialize(Base64.getDecoder().decode(rj.getString("response")));
                        return r.getResult();
                    }
                });
        return (T) enhancer.create();
    }
}

