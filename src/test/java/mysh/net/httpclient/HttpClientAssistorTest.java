package mysh.net.httpclient;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.Test;

/**
 * @author Mysh
 * @since 13-10-6 上午11:47
 */
public class HttpClientAssistorTest {
	@Ignore
	public void httpsTest() throws  Exception {
		String url = "https://raw.github.com/myshzzx/misc/master/lecai.update";
		HttpClientAssistor hca = new HttpClientAssistor(HttpClientConfig.getDefault());
		Page page = hca.getPage(url);
		System.out.println(page.getContent());
	}

	@Test
	public void downloadTest(){

	}


}
