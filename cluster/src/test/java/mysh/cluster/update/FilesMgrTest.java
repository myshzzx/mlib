package mysh.cluster.update;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

public class FilesMgrTest {

	@Test
	public void file1() throws IOException {
		System.setSecurityManager(new SecurityManager());


		byte[] ctx;
//		ctx = Files.readAllBytes(Paths.get("l:", "a.txt"));
		System.out.println("common");
		System.out.println(AccessController.getContext());
		final ProtectionDomain[] pd = new ProtectionDomain[0];
		System.out.println(pd);
		ctx = AccessController.doPrivileged((PrivilegedAction<byte[]>) () -> {
			System.out.println(AccessController.getContext());
			return null;
		}, new AccessControlContext(pd));
	}

	@Test
	public void file2(){
		System.setSecurityManager(new SecurityManager());

		new Thread(){
			@Override
			public void run() {
				System.out.println("abc");
			}
		}.start();
	}


	public static void main(String[] args) {
		Path p1 = Paths.get("c:\\a\\b");
		Path rp = p1.relativize(Paths.get("a/b/c").toAbsolutePath());
		System.out.println(rp);
	}
}
