package mysh.dev.tcpportscanner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanTask implements Runnable {

	private InetAddress host;
	private int startPort;
	private int endPort;
	private int timeout;
	private int concurrentThread;
	private int temp;

	private ExecutorService threadPool;
	private StringBuilder state;
	private Set<Observer> observers;

	public ScanTask() {
		this.startPort = 1;
		this.endPort = 65535;
		this.timeout = 1000;
		this.concurrentThread = 100;

		this.state = new StringBuilder();
		this.observers = new HashSet<Observer>();
	}

	public int getConcurrentThread() {
		return concurrentThread;
	}

	public ScanTask setConcurrentThread(int concurrentThread) {
		if (concurrentThread < 1 || concurrentThread > 5000)
			throw new RuntimeException("param error : concurrentThread");
		this.concurrentThread = concurrentThread;
		return this;
	}

	public String getHost() {
		return host.toString();
	}

	public ScanTask setHost(String host) throws UnknownHostException {
		this.host = InetAddress.getByName(host);
		return this;
	}

	public int getStartPort() {
		return startPort;
	}

	public ScanTask setStartPort(int startPort) {
		if (startPort > 65535 || startPort < 1 || startPort > this.endPort)
			throw new RuntimeException("param error : startPort");
		this.startPort = startPort;
		return this;
	}

	public int getEndPort() {
		return endPort;
	}

	public ScanTask setEndPort(int endPort) {
		if (endPort > 65535 || endPort < 1 || endPort < this.startPort)
			throw new RuntimeException("param error : endPort");
		this.endPort = endPort;
		return this;
	}

	public int getTimeout() {
		return timeout;
	}

	public ScanTask setTimeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	public void addObserver(Observer ob) {
		this.observers.add(ob);
	}

	public void removeObserver(Observer ob) {
		this.observers.remove(ob);
	}

	public void removeAllObserver(){
		this.observers.clear();
	}
	
	private void sendState(String state) {
		this.state.append(state + "\r\n");
		for (Observer ob : this.observers)
			ob.update(this.state.toString());
	}

	@Override
	public String toString() {
		return new String("scan task :  [ " + ScanTask.this.host + "  "
				+ ScanTask.this.startPort + "-" + ScanTask.this.endPort + " ]");
	}

	@Override
	public void run() {
		if (this.host == null)
			throw new RuntimeException("unknown host");

		Date startTime = new Date();
		sendState(ScanTask.this + "  start ...");
		this.threadPool = Executors.newFixedThreadPool(this.concurrentThread);
		for (this.temp = this.startPort; this.temp <= this.endPort; this.temp++) {
			this.threadPool.execute(new Runnable() {
				private int port = ScanTask.this.temp;

				@Override
				public void run() {
					Socket sock;
					try {
						// sock = new
						// Socket(ScanTask.this.host,
						// port);
						sock = new Socket();
						sock.connect(new InetSocketAddress(ScanTask.this.host,
								port), ScanTask.this.timeout);
					} catch (IOException e) {
						return;
					}
					if (sock != null)
						try {
							sock.close();
						} catch (IOException e) {
						}
					sendState(ScanTask.this.host + " : " + port);
				}
			});
		}

		this.threadPool.shutdown();
		while (!this.threadPool.isTerminated())
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		sendState(ScanTask.this + "  finished !  total spend time : "
				+ (new Date().getTime() - startTime.getTime()) / 1000 + " s");
		removeAllObserver();
		System.gc();
	}

	public void stop() {
		if (this.threadPool == null)
			return;
		sendState(ScanTask.this + "  terminated by user !");
		this.threadPool.shutdownNow();
		removeAllObserver();
	}
}

// public static void main(String[] args) throws InterruptedException {
// ScanTask st = new ScanTask().setHost("www.baidu.com").setConcurrentThread(50)
// .setEndPort(70);
// new Thread(st).start();
// // Thread.sleep(3000);
// // st.stop();
//
// }

