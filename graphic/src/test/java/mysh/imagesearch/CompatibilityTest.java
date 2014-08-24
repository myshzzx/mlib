package mysh.imagesearch;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;

/**
 * @author Mysh
 * @since 14-1-31 下午3:43
 */
public class CompatibilityTest {

	@Test
	public void readImage() throws IOException {
		URL r = this.getClass().getClassLoader().getResource("sample.jpg");
		ImageIO.read(r.openStream());
	}

	@Test
	public void networkInfo() throws SocketException {
		Enumeration<NetworkInterface> nifEnum = NetworkInterface.getNetworkInterfaces();
		while (nifEnum.hasMoreElements()) {
			NetworkInterface nif = nifEnum.nextElement();
			if (nif.isUp() && !nif.isLoopback()) {
				for (InterfaceAddress addr : nif.getInterfaceAddresses())
					if (addr.getBroadcast() != null) {
						System.out.println("network interface: " + addr);
					}
			}
		}
	}
}
