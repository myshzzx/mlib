package mysh.ui;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class CookieCatcherTest {

	public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
		String url = "http://www.baidu.com/";
		CookieCatcher c = new CookieCatcher(url, url);
		List<String> cookie1 = c.getCookie();
		System.out.println(cookie1);

		c = new CookieCatcher(url, url);
		cookie1 = c.getCookie();
		System.out.println(cookie1);

	}
}
