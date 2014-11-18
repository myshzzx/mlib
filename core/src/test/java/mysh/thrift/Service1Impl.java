package mysh.thrift;

import org.apache.thrift.TException;

/**
 * @author: 张智贤
 */
public class Service1Impl implements TService1.Iface {
	public String getStr(String value, int number) throws TException {
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
		}
		return value + " " + number;
	}

}
