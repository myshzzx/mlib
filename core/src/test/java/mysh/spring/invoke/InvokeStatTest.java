package mysh.spring.invoke;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author zhangzhixian<hzzhangzhixian @ corp.netease.com>
 * @since 2017/2/11
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@Component("InvokeStatTest")
public class InvokeStatTest {
	
	@Autowired
	@Qualifier("InvokeStatTest")
	InvokeStatTest self;
	
	@Test
	public void f() throws InterruptedException {
		int n = 10;
		while (n-- > 0) {
			self.testRun(1, 2);
			Thread.sleep(500);
		}
	}
	
	@InvokeStat(value = "test1", recParams = true)
	public void testRun(int p, int q) {
		int a = 0, n = 1000000000;
		while (n-- > 0)
			a = a + 1;
		System.out.println(a);
	}
}
