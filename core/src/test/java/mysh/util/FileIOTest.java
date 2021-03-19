
package mysh.util;

import mysh.net.httpclient.HttpClientConfig;
import org.junit.jupiter.api.Disabled;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;

public class FileIOTest {

	// @Before
	public void prepare() throws FileNotFoundException, IOException {

		File f = new File("test1");

		List<HttpClientConfig> list = new LinkedList<>();
		for (int i = 0; i < 400_000; i++) {
			list.add(new HttpClientConfig());
		}

		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f));
		out.writeObject(list);
	}

	@Disabled
	public void memoryMapTest() throws IOException {

		File f = new File("test1");

		FileChannel fc = FileChannel.open(f.toPath(), StandardOpenOption.READ);
		fc.map(MapMode.READ_ONLY, 0, f.length());
		ByteBuffer buf = ByteBuffer.allocate(4_000_000);
		fc.read(buf, 0);
	}
}
