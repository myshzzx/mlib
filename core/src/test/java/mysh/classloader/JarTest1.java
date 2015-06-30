package mysh.classloader;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarFile;

/**
 * @author Mysh
 * @since 2014/12/4 22:35
 */
public class JarTest1 {
	public static class A {

	}

	@Test
	@Ignore
	public void t1() throws IOException {
		final URL url = new URL("jar:file:///L:/javafx-mx.jar!/");
		final JarURLConnection jar = (JarURLConnection) url.openConnection();
		final JarFile jarFile = jar.getJarFile();
		jarFile.stream().forEach(e -> {
			System.out.println(e.getName());
			System.out.println(e.getTime());
		});
	}

	@Test
	public void t2() throws ClassNotFoundException {
		final Class<?> c = Class.forName("mysh.classloader.JarTest1$A");
		System.out.println(c.getName());
	}
}
