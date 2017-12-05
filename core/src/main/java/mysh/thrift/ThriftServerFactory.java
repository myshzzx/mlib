package mysh.thrift;

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServerEventHandler;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TServerSocket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * a TLS-supported and non-blocking thrift-server maker.<br/>
 * DO NOT support async mode, because async mode focus on thread-reuse(for large scale c/s),
 * but this implementation focus on connection-reuse(for small scale and heavy communication).
 */
public class ThriftServerFactory {

    // client never timeout, for long connection
    private final int clientTimeout = 0;
    private String serverHost;
    private int serverPort;
    private TProcessor processor;
    private int serverPoolSize;
    private int nonTLSServerMaxFrameSize = 16384000;

    private boolean useTLS;
    private String selfKeyStore;
    private String selfKeyStorePw;
    private boolean isRequireClientAuth = true;
    private String trustKeyStore;
    private String trustKeyStorePw;

    private TServerEventHandler serverEventHandler;

    private static final AtomicInteger serverTPI = new AtomicInteger(1);

    /**
     * @return build but not start server.
     * @throws Exception
     */
    @SuppressWarnings("ConstantConditions")
    public TServer build() throws Exception {
        final TServer server;

        int poolSize = Runtime.getRuntime().availableProcessors() * 2;
        poolSize = Math.max(poolSize, this.serverPoolSize);
        AtomicInteger tpi = new AtomicInteger(1);
        ThreadPoolExecutor tPool = new ThreadPoolExecutor(poolSize, poolSize, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r, "tServer-" + serverTPI.getAndIncrement() + "-" + tpi.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                });
        tPool.allowCoreThreadTimeOut(true);

        if (useTLS) { // TLS server
            TSSLTransportFactory.TSSLTransportParameters transportParams = new TSSLTransportFactory.TSSLTransportParameters();
            transportParams.setKeyStore(
                    ThriftServerFactory.class.getClassLoader().getResource(this.selfKeyStore).getPath(),
                    this.selfKeyStorePw);
            transportParams.requireClientAuth(this.isRequireClientAuth);
            if (this.isRequireClientAuth)
                transportParams.setTrustStore(
                        ThriftServerFactory.class.getClassLoader().getResource(this.trustKeyStore).getPath(),
                        this.trustKeyStorePw);
            TServerSocket serverTransport = TSSLTransportFactory.getServerSocket(this.serverPort, this.clientTimeout,
                    InetAddress.getByName(this.serverHost), transportParams);

            server = new TThreadPoolServer(
                    new TThreadPoolServer.Args(serverTransport)
                            .processor(this.processor)
                            .protocolFactory(new TCompactProtocol.Factory())
                            .executorService(tPool));
        } else { // Non security Server
            TNonblockingServerSocket serverTransport = new TNonblockingServerSocket(
                    new InetSocketAddress(this.serverHost, this.serverPort), this.clientTimeout);

            server = new THsHaServer(
                    new THsHaServer.Args(serverTransport)
                            .processor(this.processor)
                            .protocolFactory(new TCompactProtocol.Factory())
                            .transportFactory(new TFramedTransport.Factory(nonTLSServerMaxFrameSize))
                            .executorService(tPool));
        }

        if (this.serverEventHandler != null) {
            server.setServerEventHandler(this.serverEventHandler);
        }

        return server;
    }

    /**
     * 服务主机地址。
     */
    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    /**
     * 服务端口。
     */
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * 服务处理器。
     */
    public void setProcessor(TProcessor processor) {
        this.processor = processor;
    }

    /**
     * server handler thread pool size.<br/>
     * this value should be larger than possible client connection number,
     * but not to large, in case of heavy cost on thread context switch.
     * <p>
     * WARNING: number of client connections will not exceed this pool size,
     * because client connection will hold a thread till client-task completes,
     * if there's no more thread to handle subTask-complete invoking,
     * the client-task will never end (or timeout if the parameter is not zero).
     */
    public void setServerPoolSize(int serverPoolSize) {
        this.serverPoolSize = serverPoolSize;
    }

    /**
     * if not use TLS, the server is a non-blocking server, which use TFramedTransport,
     * each invoke will be encapsulated to a frame,
     * and there is a "max frame size" limit (default 16384000), if the frame size exceed, invoking will fail.
     */
    public ThriftServerFactory setNonTLSServerMaxFrameSize(int nonTLSServerMaxFrameSize) {
        if (nonTLSServerMaxFrameSize > 0)
            this.nonTLSServerMaxFrameSize = nonTLSServerMaxFrameSize;
        return this;
    }

    /**
     * 是否启用 TLS. 安全相关配置的开关.
     */
    public void setUseTLS(boolean useTLS) {
        this.useTLS = useTLS;
    }

    /**
     * 服务端证书库。
     */
    public void setSelfKeyStore(String selfKeyStore) {
        this.selfKeyStore = selfKeyStore;
    }

    /**
     * 服务端证书库密码。
     */
    public void setSelfKeyStorePw(String selfKeyStorePw) {
        this.selfKeyStorePw = selfKeyStorePw;
    }

    /**
     * 是否需要客户端认证。
     */
    public void setRequireClientAuth(boolean requireClientAuth) {
        isRequireClientAuth = requireClientAuth;
    }

    /**
     * 信任证书库。
     */
    public void setTrustKeyStore(String trustKeyStore) {
        this.trustKeyStore = trustKeyStore;
    }

    /**
     * 信任证书库密码。
     */
    public void setTrustKeyStorePw(String trustKeyStorePw) {
        this.trustKeyStorePw = trustKeyStorePw;
    }

    /**
     * 服务端事件监听器。
     */
    public void setServerEventHandler(TServerEventHandler serverEventHandler) {
        this.serverEventHandler = serverEventHandler;
    }

}
