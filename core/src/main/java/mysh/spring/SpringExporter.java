package mysh.spring;

import com.alibaba.fastjson.JSON;
import mysh.util.Exps;
import mysh.util.Serializer;
import mysh.util.Strings;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 * @since 2017/08/04
 */
// @Component
public class SpringExporter implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(SpringExporter.class);

    public static final int SERVER_PORT = 62345;
    public static Serializer SERIALIZER = Serializer.FST;

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

    public Result invoke(Invoke iv) throws NoSuchMethodException {
        Object bean = Strings.isNotBlank(iv.beanName) ? ctx.getBean(iv.beanName) : ctx.getBean(iv.type);
        Method method = findMethod(iv.methodClass, iv.methodName, iv.methodParamsTypes);

        Result r = new Result();
        try {
            Object value = method.invoke(bean, iv.args);
            r.setResult(value);
        } catch (Throwable t) {
            r.t = t;
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
                    OutputStream out = null;
                    InputStream in = null;
                    if (socket == null || socket.isClosed()) {
                        socket = new Socket(host, port);
                        tt.set(socket);
                        out = socket.getOutputStream();
                        in = socket.getInputStream();
                    }
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

    public static <T> T proxyHttp(String url, String lineSepHeaders, Class<T> type) {
        return proxyHttp(url, lineSepHeaders, type, null);
    }

    public static <T> T proxyHttp(String url, String lineSepHeaders, Class<T> type, @Nullable String beanName) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(type);
        OkHttpClient client = new OkHttpClient.Builder().build();
        enhancer.setCallback(
                (InvocationHandler) (o, method, args) -> {
                    Invoke iv = new Invoke(type, beanName,
                            method.getDeclaringClass(), method.getName(), method.getParameterTypes(),
                            args);

                    Request.Builder rb = new Request.Builder().url(url);
                    rb.addHeader("invoke", Base64.getEncoder().encodeToString(SERIALIZER.serialize(iv)));
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
                    try (Response rsp = client.newCall(rb.build()).execute()) {
                        String result = rsp.body().string();
                        if (Strings.isBlank(result)) {
                            throw new RuntimeException("check server log for exp info");
                        } else {
                            Result r = SERIALIZER.deserialize(Base64.getDecoder().decode(result));
                            return r.getResult();
                        }
                    }
                });
        return (T) enhancer.create();
    }

    public String serveHttp(HttpServletRequest req) {
        try {
            String ivStr = req.getHeader("invoke");
            SpringExporter.Invoke iv = SpringExporter.SERIALIZER.deserialize(Base64.getDecoder().decode(ivStr));
            log.info("serveHttp-invoke-iv,iv={},tid={}", iv, Thread.currentThread().getId());
            SpringExporter.Result r = this.invoke(iv);
            return Base64.getEncoder().encodeToString(SpringExporter.SERIALIZER.serialize(r));
        } catch (Exception e) {
            log.error("serveHttp-invoke-err", e);
            return "";
        }
    }
}
