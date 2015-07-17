package mysh.benchmark;

import mysh.util.Tick;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 一次操作包括一个自减,一个方法调用,一个自增
 * i7-4702m(2.8GHz): 2亿次操作, 反射耗时1150ms, 直接调用耗时7ms, public 与 private 没区别
 *
 * @author mysh
 * @since 2015/7/16
 */
@Ignore
public class ReflectionTest {

	public static final int INVOKE_TIMES = 200_000_000;

	public static class T {
		int v;

		private void f1() {
			v++;
		}

		public void f2() {
			v++;
		}
	}

	@Test
	public void direct() {
		directPrivateCall();
		directPublicCall();

		directPrivateCall();
		directPublicCall();
	}

	@Test
	public void directPrivateCall() {
		int n;
		T t = new T();

		Tick tick = Tick.tick("private");
		n = INVOKE_TIMES;
		t.v = 0;
		while (n-- > 0)
			t.f1();
		tick.nipAndPrint();

		tick.reset();
		n = INVOKE_TIMES;
		t.v = 0;
		while (n-- > 0)
			t.f1();
		tick.nipAndPrint();
	}

	@Test
	public void directPublicCall() {
		int n;
		T t = new T();

		Tick tick = Tick.tick("public");
		n = INVOKE_TIMES;
		t.v = 0;
		while (n-- > 0)
			t.f2();
		tick.nipAndPrint();

		tick.reset();
		n = INVOKE_TIMES;
		t.v = 0;
		while (n-- > 0)
			t.f2();
		tick.nipAndPrint();
	}

	@Test
	public void reflection() throws Exception {
		reflectionPublic();
		reflectionPrivate();

		reflectionPublic();
		reflectionPrivate();
	}

	@Test
	public void reflectionPrivate() throws Exception {
		Method f = T.class.getDeclaredMethod("f1");
		reflectionMethodCall(f);
	}

	@Test
	public void reflectionPublic() throws Exception {
		Method f = T.class.getDeclaredMethod("f2");
		reflectionMethodCall(f);
	}

	public void reflectionMethodCall(Method f) throws IllegalAccessException, InvocationTargetException {
		f.setAccessible(true);
		int n;
		T t = new T();

		Tick tick = Tick.tick(f.getName());
		n = INVOKE_TIMES;
		t.v = 0;
		while (n-- > 0)
			f.invoke(t);
		tick.nipAndPrint();

		tick.reset();
		n = INVOKE_TIMES;
		t.v = 0;
		while (n-- > 0)
			f.invoke(t);
		tick.nipAndPrint();
	}
}
