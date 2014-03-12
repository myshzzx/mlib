package mysh.thrift;

import org.apache.thrift.TException;

/**
 * @author: 张智贤
 */
public class Service1Impl implements TService1.Iface {
	public String getStr(String value, int number) throws TException {
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
		return value + " " + number;
	}

}
