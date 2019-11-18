package mysh.net;

import jcifs.NameServiceClient;
import jcifs.context.SingletonContext;
import jcifs.netbios.NameServiceClientImpl;
import org.junit.Ignore;
import org.junit.Test;

import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * @since 2019-11-18
 */
@Ignore
public class SMBHelperTest {
	@Test
	public void getHostAddressByName() throws UnknownHostException {
		System.out.println(SMBHelper.getHostAddressByName("zzx-mac"));
		System.out.println(SMBHelper.getHostAddressByName("localhost"));
		System.out.println(SMBHelper.getHostAddressByName("z.cn"));
		System.out.println(SMBHelper.getHostAddressByName("19.12.42.128"));
		System.out.println(SMBHelper.getHostAddressByName("abcdefg"));
	}
	
	@Test
	public void getAllHostAddr() throws UnknownHostException {
		NameServiceClient nsc = new NameServiceClientImpl(SingletonContext.getInstance());
		System.out.println(Arrays.toString(nsc.getAllByName("zzx-mac", true)));
		System.out.println(Arrays.toString(nsc.getAllByName("zzx-mac", false)));
	}
}
