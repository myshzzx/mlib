package mysh.cluster;

import mysh.util.PropConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;

/**
 * cluster configuration.
 *
 * @author Mysh
 * @since 2014/12/12 21:47
 */
public class ClusterConf implements Serializable {
	private static final Logger log = LoggerFactory.getLogger(ClusterConf.class);

	static final long serialVersionUID = -8940782299543664345L;
	private static final String filePath = "cluster.properties";

	/**
	 * cluster node id. should be unique within a cluster.<br/>
	 * if it's null, then the node will generate one.
	 */
	String id;
	private static final String _id = "id";
	/**
	 * cluster start time.
	 */
	long startTime = System.currentTimeMillis();
	private static final String _startTime = "startTime";
	/**
	 * cmd port. UDP(cmdPort) will be used in broadcast communication,
	 * while TCP(cmdPort) in services dispatching,
	 * TCP(cmdPort+1) in Master-Node service and TCP(cmdPort+2) in Worker-Node service.
	 */
	int cmdPort = 8030;
	private static final String _cmdPort = "cmdPort";
	/**
	 * rpc server pool size. usually used by client, also used by worker node
	 * when update files. can be 0.<br/>
	 * see {@link mysh.thrift.ThriftServerFactory#setServerPoolSize}
	 */
	int serverPoolSize = 0;
	private static final String _serverPoolSize = "serverPoolSize";
	/**
	 * heart beat interval time in milliseconds.
	 */
	int heartBeatTime = 10_000;
	private static final String _heartBeatTime = "heartBeatTime";
	/**
	 * initial state of the worker, which can be updated and sent to master node automatically.
	 * can be <b>null</b>.
	 */
	WorkerState initState;

	/**
	 * get config from properties file.
	 */
	public static ClusterConf readConf() {
		ClusterConf c = new ClusterConf();

		try {
			final PropConf propConf = new PropConf(filePath);
			c.id = propConf.getPropString(_id);
			c.startTime = propConf.getPropLong(_startTime, c.startTime);
			c.cmdPort = propConf.getPropInt(_cmdPort, c.cmdPort);
			c.serverPoolSize = propConf.getPropInt(_serverPoolSize, c.serverPoolSize);
			c.heartBeatTime = propConf.getPropInt(_heartBeatTime, c.heartBeatTime);
		} catch (Throwable e) {
			log.error("read cluster conf from file error.", e);
		}

		return c;
	}

	/**
	 * save config to file.
	 */
	public void save() {
		try {
			final PropConf propConf = new PropConf();
			propConf.putProp(_id, id);
			propConf.putProp(_startTime, startTime);
			propConf.putProp(_cmdPort, cmdPort);
			propConf.putProp(_serverPoolSize, serverPoolSize);
			propConf.putProp(_heartBeatTime, heartBeatTime);

			propConf.save2File(filePath);
		} catch (IOException e) {
			log.error("save cluster conf error.", e);
		}
	}
}
