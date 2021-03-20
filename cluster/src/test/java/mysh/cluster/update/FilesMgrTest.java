package mysh.cluster.update;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

public class FilesMgrTest {

	@Test
	@Disabled
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
	@Disabled
	public void file2(){
		System.setSecurityManager(new SecurityManager());

		new Thread(){
			@Override
			public void run() {
				System.out.println("abc");
			}
		}.start();
	}


}
