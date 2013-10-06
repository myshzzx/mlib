package mysh.net.httpclient;

import jdk.nashorn.internal.ir.annotations.Ignore;

import java.io.IOException;

/**
 * @author Mysh
 * @since 13-10-6 上午11:47
 */
public class HttpClientAssistorTest {
	@Ignore
	public void httpsTest() throws InterruptedException, GetPageException, IOException {
		String url = "https://raw.github.com/myshzzx/misc/master/lecai.update";
		HttpClientAssistor hca = new HttpClientAssistor(HttpClientConfig.getDefault());
		Page page = hca.getPage(url);
		System.out.println(page.getContent());
	}
}
