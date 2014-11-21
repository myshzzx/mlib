package mysh.thrift;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author Mysh
 * @since 2014/11/21 13:18
 */
public class Service2Impl implements TService1.AsyncIface {
	@Override
	public void getStr(String value, ByteBuffer number, AsyncMethodCallback resultHandler) throws TException {
		try {
			Thread.sleep(5000);
		} catch (Exception e) {
		}
		resultHandler.onComplete(value + " " + Arrays.toString(number.array()));
	}
}
