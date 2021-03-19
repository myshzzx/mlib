package mysh.spring.invoke;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author zhangzhixian<hzzhangzhixian @ corp.netease.com>
 * @since 2017/2/17
 */
@Disabled
@ExtendWith(SpringExtension.class)
@Component("PerformanceInspectorTest")
public class PerformanceInspectorTest {
	
	@Autowired
	@Qualifier("PerformanceInspectorTest")
	PerformanceInspectorTest self;
	
	@Test
	public void test() {
		self.f();
	}
	
	public void f() {
		self.a();
		self.b();
		self.c();
	}
	
	public void a() {
		self.b();
	}
	
	public void b() {
		self.c();
	}
	
	public void c() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
