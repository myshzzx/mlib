package jsocks.socks;

/* Copyright 2013 Shai Ayal
 *
 * This file is part of SPT.
 *
 * SPT is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPT is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SPT.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelDirectTCPIP;
import com.jcraft.jsch.Session;
import jsocks.socks.server.ServerAuthenticatorNone;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * run a socks proxy upon ssh
 */
@Slf4j
public class DynamicForwarder implements Runnable {
	
	class DynamicForward extends ProxyServer {
		Session session;
		int port;
		
		public DynamicForward(Session session, int port) {
			super(new ServerAuthenticatorNone());
			this.session = session;
			this.port = port;
		}
		
		public DynamicForward(Socket s, Session session, int port) {
			super(new ServerAuthenticatorNone(), s);
			this.session = session;
			this.port = port;
		}
		
		@Override
		public void start(int port, int backlog, InetAddress localIP) {
			try {
				ss = new ServerSocket(port, backlog, localIP);
				// noinspection InfiniteLoopStatement
				while (true) {
					Socket s = ss.accept();
					DynamicForward df = new DynamicForward(s, session, port);
					(new Thread(df)).start();
				}
			} catch (Exception e) {
				log.error("proxyServer-handle-exp,port={},backlog={}", port, backlog, e);
			}
		}
		
		@Override
		protected void onConnect(ProxyMessage msg) throws IOException {
			ProxyMessage response = null;
			int iSock5Cmd = CProxy.SOCKS_FAILURE; // defaulting to failure
			int iSock4Msg = Socks4Message.REPLY_NO_CONNECT;
			InetAddress sIp = null;
			int iPort = 0;
			Channel channel = null;
			
			try {
				channel = session.openChannel("direct-tcpip");
				channel.setInputStream(in);
				channel.setOutputStream(out);
				((ChannelDirectTCPIP) channel).setHost(msg.host);
				((ChannelDirectTCPIP) channel).setPort(msg.port);
				channel.connect();
				
				iSock5Cmd = CProxy.SOCKS_SUCCESS;
				iSock4Msg = Socks4Message.REPLY_OK;
				sIp = msg.ip;
				iPort = msg.port;
				
			} catch (Exception sE) {
				log("Failed connecting to remote socket. Exception: " + sE.getLocalizedMessage());
				
				// TBD Pick proper socks error for corresponding socket error,
				// below is too generic
				iSock5Cmd = CProxy.SOCKS_CONNECTION_REFUSED;
				iSock4Msg = Socks4Message.REPLY_NO_CONNECT;
			}
			
			if (msg instanceof Socks5Message) {
				response = new Socks5Message(iSock5Cmd, sIp, iPort);
			} else {
				response = new Socks4Message(iSock4Msg, sIp, iPort);
			}
			
			response.write(out);
			try {
				if (channel != null) {
					while (channel.isConnected()) {
						Thread.sleep(100);
					}
				}
			} catch (InterruptedException ignored) {
			}
		}
		
		void start() throws IOException {
			start(port);
		}
	}
	
	DynamicForward df = null;
	Thread ServerThread = null;
	Exception e = null;
	
	public DynamicForwarder(int port, Session session) {
		df = new DynamicForward(session, port);
		// DynamicForward.setLog(System.out);
		ServerThread = new Thread(this, "DynamicForwader");
		ServerThread.start();
	}
	
	public void run() {
		try {
			df.start();
		} catch (IOException e) {
			this.e = e;
		}
	}
	
	public void stop() {
		df.stop();
		if (!ServerThread.isInterrupted()) {
			ServerThread.interrupt();
		}
	}
}
