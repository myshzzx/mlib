package mysh.thrift;

import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.ServerContext;
import org.apache.thrift.server.TServerEventHandler;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;

/**
 * User: Allen
 * Time: 13-6-9 上午10:43
 */
public class ServerEventHandlerImpl implements TServerEventHandler {
	private static final Logger log = LoggerFactory.getLogger(ServerEventHandlerImpl.class);

	@Override
	public void preServe() {
		log.info("preServe");
	}

	@Override
	public ServerContext createContext(TProtocol input, TProtocol output) {
		String clientName = null;
		if (input.getTransport() instanceof TSocket) { // tls
			try {
				clientName = ((SSLSocket) ((TSocket) input.getTransport()).getSocket())
								.getSession().getPeerCertificateChain()[0].getSubjectDN().getName();
			} catch (SSLPeerUnverifiedException e) {
				log.error(e.getMessage(), e);
			}
			log.info("createContext: " + clientName);
		}
		return null;
	}

	@Override
	public void deleteContext(ServerContext serverContext, TProtocol input, TProtocol output) {
		log.info("deleteContext");
	}

	@Override
	public void processContext(ServerContext serverContext, TTransport inputTransport, TTransport outputTransport) {
		log.info("processContext: ");
	}

	private static class SSLServerContext implements ServerContext {
//		private HandshakeListener handshakeListener;
//
//		private SSLServerContext(HandshakeListener handshakeListener) {
//			this.handshakeListener = handshakeListener;
//		}
	}

//	private static class HandshakeListener implements HandshakeCompletedListener {
//		private String clientName;
//
//		@Override
//		public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
//			try {
//				this.clientName = handshakeCompletedEvent.getPeerCertificateChain()[0].getSubjectDN().getName();
//			} catch (SSLPeerUnverifiedException e) {
//				log.error(e.getMessage(), e);
//			}
//		}
//	}
}
