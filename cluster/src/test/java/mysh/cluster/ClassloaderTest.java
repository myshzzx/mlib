package mysh.cluster;

import org.junit.Test;

/**
 * @author Mysh
 * @since 2014/11/26 9:47
 */
public class ClassloaderTest {
	private static class L extends ClassLoader{
		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
return null;
		}
	}
	@Test
	public void t1() {

	}
}
