package mysh.cluster.rpc;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Mysh
 * @since 2014/11/19 17:33
 */
public class IFaceHolder<I> implements Closeable {
	private I client;
	private Closeable c;

	public IFaceHolder(I client, Closeable c) {
		this.client = client;
		this.c = c;
	}

	public I getClient() {
		return client;
	}

	/**
	 * close of thrift transport will never throws exceptions.
	 */
	@Override
	public void close() throws IOException {
		if (c != null) c.close();
	}
}
