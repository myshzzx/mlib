package jsocks.socks;

import jsocks.socks.server.IdentAuthenticator;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

public class SOCKS {
	
	static public void usage() {
		System.out.println(
				"Usage: java SOCKS [inifile1 inifile2 ...]\n" +
						"If no inifile is given, uses socks.properties.\n"
		);
	}
	
	static public void main(String[] args) {
		
		String[] file_names;
		int port = 1080;
		String logFile = null;
		String host = null;
		
		IdentAuthenticator auth = new IdentAuthenticator();
		OutputStream log = null;
		InetAddress localIP = null;
		
		if (args.length == 0) {
			file_names = new String[1];
			file_names[0] = "socks.properties";
		} else {
			file_names = args;
		}
		
		
		Properties pr = null;
		inform("Loading properties...");
		for (int i = 0; i < file_names.length; ++i) {
			
			inform("Reading file: " + file_names[i]);
			
			pr = loadProperties(file_names[i]);
			if (pr == null) {
				if (args.length == 0) {
					inform("Using default property values.");
					pr = load_defaults();
				} else {
					continue;
				}
			}
			
			if (!addAuth(auth, pr)) {
				System.err.println("Error in file " + file_names[i] + ".");
				pr = null;
				continue;
			}
			//First file should contain all global settings,
			// like port and host and log.
			if (i == 0) {
				String port_s = (String) pr.get("port");
				if (port_s != null) {
					try {
						port = Integer.parseInt(port_s);
					} catch (NumberFormatException nfe) {
						System.err.println("Can't parse port: " + port_s);
						pr = null;
						break;
					}
				}
				
				serverInit(pr);
				logFile = (String) pr.get("log");
				host = (String) pr.get("host");
			}
			
			//inform("Props:"+pr);
		}
		
		if (pr == null) {
			System.err.println("Failed to open/parse properties file.\n");
			usage();
			return;
		}
		
		
		if (logFile != null) {
			if (logFile.equals("-")) {
				log = System.out;
			} else {
				try {
					log = new FileOutputStream(logFile);
				} catch (IOException ioe) {
					System.err.println("Can't open log file " + logFile);
					return;
				}
			}
		}
		
		if (host != null) {
			try {
				localIP = InetAddress.getByName(host);
			} catch (UnknownHostException uhe) {
				System.err.println("Can't resolve local ip: " + host);
				return;
			}
		}
		
		inform("Using Ident Authentication scheme:\n" + auth + "\n");
		ProxyServer server = new ProxyServer(auth);
		inform("JSocks Proxy Server started. Listening on port: " + port);
		
		server.start(port, 5, localIP);
	}
	
	static Properties loadProperties(String file_name) {
		
		Properties pr = new Properties();
		
		try {
			InputStream fin = new FileInputStream(file_name);
			pr.load(fin);
			fin.close();
		} catch (IOException ioe) {
			System.err.println("loadProperties(" + file_name + ") failed: " + ioe.getLocalizedMessage());
			pr = null;
		}
		return pr;
	}
	
	static boolean addAuth(IdentAuthenticator ident, Properties pr) {
		
		InetRange irange;
		
		String range = (String) pr.get("range");
		if (range == null) return false;
		irange = parseInetRange(range);
		
		
		String users = (String) pr.get("users");
		
		if (users == null) {
			ident.add(irange, null);
			return true;
		}
		
		Hashtable uhash = new Hashtable();
		
		StringTokenizer st = new StringTokenizer(users, ";");
		while (st.hasMoreTokens())
			uhash.put(st.nextToken(), "");
		
		ident.add(irange, uhash);
		return true;
	}
	
	/**
	 * Does server initialisation.
	 */
	static void serverInit(Properties props) {
		int val;
		val = readInt(props, "iddleTimeout");
		if (val >= 0) {
			ProxyServer.setIddleTimeout(val);
			inform("Setting iddle timeout to " + val + " ms.");
		}
		val = readInt(props, "acceptTimeout");
		if (val >= 0) {
			ProxyServer.setAcceptTimeout(val);
			inform("Setting accept timeout to " + val + " ms.");
		}
		val = readInt(props, "udpTimeout");
		if (val >= 0) {
			ProxyServer.setUDPTimeout(val);
			inform("Setting udp timeout to " + val + " ms.");
		}
		
		val = readInt(props, "datagramSize");
		if (val >= 0) {
			ProxyServer.setDatagramSize(val);
			inform("Setting datagram size to " + val + " bytes.");
		}
		
		proxyInit(props);
		
	}
	
	/**
	 * Initialises proxy, if any specified.
	 */
	static void proxyInit(Properties props) {
		String proxy_list;
		CProxy proxy = null;
		StringTokenizer st;
		
		proxy_list = (String) props.get("proxy");
		if (proxy_list == null) return;
		
		st = new StringTokenizer(proxy_list, ";");
		while (st.hasMoreTokens()) {
			String proxy_entry = st.nextToken();
			
			CProxy p = CProxy.parseProxy(proxy_entry);
			
			if (p == null)
				exit("Can't parse proxy entry:" + proxy_entry);
			
			
			inform("Adding CProxy:" + p);
			
			if (proxy != null)
				p.setChainProxy(proxy);
			
			proxy = p;
			
		}
		if (proxy == null) return;  //Empty list
		
		String direct_hosts = (String) props.get("directHosts");
		if (direct_hosts != null) {
			InetRange ir = parseInetRange(direct_hosts);
			inform("Setting direct hosts:" + ir);
			proxy.setDirect(ir);
		}
		
		
		ProxyServer.setProxy(proxy);
	}
	
	/**
	 * Inits range from the string of semicolon separated ranges.
	 */
	static InetRange parseInetRange(String source) {
		InetRange irange = new InetRange();
		
		StringTokenizer st = new StringTokenizer(source, ";");
		while (st.hasMoreTokens())
			irange.add(st.nextToken());
		
		return irange;
	}
	
	/**
	 * Integer representaion of the property named name, or -1 if one
	 * is not found.
	 */
	static int readInt(Properties props, String name) {
		int result = -1;
		String val = (String) props.get(name);
		if (val == null) return -1;
		StringTokenizer st = new StringTokenizer(val);
		if (!st.hasMoreElements()) return -1;
		try {
			result = Integer.parseInt(st.nextToken());
		} catch (NumberFormatException nfe) {
			inform("Bad value for " + name + ":" + val);
		}
		return result;
	}
	
	//Display functions
	///////////////////
	
	static void inform(String s) {
		System.out.println(s);
	}
	
	static void exit(String msg) {
		System.err.println("Error:" + msg);
		System.err.println("Aborting operation");
		System.exit(0);
	}
	
	private static Properties load_defaults() {
		Properties sRet = new Properties();
		
		sRet.setProperty("port", "1080");
		
		sRet.setProperty("range", ".");
		
		sRet.setProperty("iddleTimeout", "600000");
		sRet.setProperty("acceptTimeout", "60000");
		sRet.setProperty("udpTimeout", "600000");
		
		sRet.setProperty("log", "-");
		
		return sRet;
	}
}
