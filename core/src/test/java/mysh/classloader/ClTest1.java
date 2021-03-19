package mysh.classloader;

import mysh.util.Serializer;
import org.junit.jupiter.api.Test;

import java.util.Base64;

/**
 * @author Mysh
 * @since 2014/12/2 16:15
 */
public class ClTest1 {

	/**
	 * a Runnable class T which prints aaa
	 */
	static byte[] a = Base64.getDecoder().decode(
					"yv66vgAAADQAJQoABgAVCQAWABcIABgKABkAGgcAGwcAHAcAHQcAHgEAEHNlcmlhbFZlcnNpb25VSUQBAAFKAQANQ29uc3RhbnRWYWx1ZQXy7oCx6xcr6wEABjxpbml0PgEAAygpVgEABENvZGUBAA9MaW5lTnVtYmVyVGFibGUBAANydW4BAApTb3VyY2VGaWxlAQAGVC5qYXZhDAAOAA8HAB8MACAAIQEAA2FhYQcAIgwAIwAkAQABVAEAEGphdmEvbGFuZy9PYmplY3QBABJqYXZhL2xhbmcvUnVubmFibGUBABRqYXZhL2lvL1NlcmlhbGl6YWJsZQEAEGphdmEvbGFuZy9TeXN0ZW0BAANvdXQBABVMamF2YS9pby9QcmludFN0cmVhbTsBABNqYXZhL2lvL1ByaW50U3RyZWFtAQAHcHJpbnRsbgEAFShMamF2YS9sYW5nL1N0cmluZzspVgAhAAUABgACAAcACAABABoACQAKAAEACwAAAAIADAACAAEADgAPAAEAEAAAAB0AAQABAAAABSq3AAGxAAAAAQARAAAABgABAAAAAwABABIADwABABAAAAAlAAIAAQAAAAmyAAISA7YABLEAAAABABEAAAAKAAIAAAAIAAgACQABABMAAAACABQ=");
	/**
	 * a Runnable class T witch prints bbb
	 */
	static byte[] b = Base64.getDecoder().decode(
					"yv66vgAAADQAJQoABgAVCQAWABcIABgKABkAGgcAGwcAHAcAHQcAHgEAEHNlcmlhbFZlcnNpb25VSUQBAAFKAQANQ29uc3RhbnRWYWx1ZQXy7oCx6xcr6wEABjxpbml0PgEAAygpVgEABENvZGUBAA9MaW5lTnVtYmVyVGFibGUBAANydW4BAApTb3VyY2VGaWxlAQAGVC5qYXZhDAAOAA8HAB8MACAAIQEAA2JiYgcAIgwAIwAkAQABVAEAEGphdmEvbGFuZy9PYmplY3QBABJqYXZhL2xhbmcvUnVubmFibGUBABRqYXZhL2lvL1NlcmlhbGl6YWJsZQEAEGphdmEvbGFuZy9TeXN0ZW0BAANvdXQBABVMamF2YS9pby9QcmludFN0cmVhbTsBABNqYXZhL2lvL1ByaW50U3RyZWFtAQAHcHJpbnRsbgEAFShMamF2YS9sYW5nL1N0cmluZzspVgAhAAUABgACAAcACAABABoACQAKAAEACwAAAAIADAACAAEADgAPAAEAEAAAAB0AAQABAAAABSq3AAGxAAAAAQARAAAABgABAAAAAwABABIADwABABAAAAAlAAIAAQAAAAmyAAISA7YABLEAAAABABEAAAAKAAIAAAAIAAgACQABABMAAAACABQ=");

	static byte[] s = Base64.getDecoder().decode("rO0ABXNyAAFU8u6AsesXK+sCAAB4cA==");

	@Test
	public void t1() throws ClassNotFoundException {
		final ClassLoader tLoader = Thread.currentThread().getContextClassLoader();
		final MyCl cl1 = new MyCl(tLoader, a);
		final MyCl cl2 = new MyCl(tLoader, b);

		Runnable r = Serializer.BUILD_IN.deserialize(s, cl1);
		r.run();
		r = Serializer.BUILD_IN.deserialize(s, cl2);
		r.run();
	}

	private static class MyCl extends ClassLoader {

		private ClassLoader parent;
		private byte[] buf;

		public MyCl(ClassLoader parent, byte[] buf) {
			super(parent);
			this.parent = parent;
			this.buf = buf;
		}

		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			synchronized (getClassLoadingLock(name)) {
				Class<?> c = findLoadedClass(name);
				if (name.equals("T"))
					c = defineClass(name, buf, 0, buf.length);
				if (c == null)
					c = parent.loadClass(name);

				if (resolve) {
					resolveClass(c);
				}
				return c;
			}
		}

	}
}
