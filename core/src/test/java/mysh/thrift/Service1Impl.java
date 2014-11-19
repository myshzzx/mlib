package mysh.thrift;

import org.apache.thrift.TException;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author: 张智贤
 */
public class Service1Impl implements TService1.Iface {

	@Override
	public String getStr(String value, ByteBuffer number) throws TException {
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
		}
		return value + " " + Arrays.toString(number.array());
	}
}
