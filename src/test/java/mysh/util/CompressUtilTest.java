
package mysh.util;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.ZipEntry;

import org.junit.Assert;
import org.junit.Test;

public class CompressUtilTest {

	@Test
	public void test() throws IOException {

		final byte[][] data = new byte[][] { new byte[0], new byte[1000], new byte[2000] };
		Random r = new Random();

		for (byte[] ba : data) {
			r.nextBytes(ba);
		}

		// compress
		ByteArrayOutputStream compressOut = new ByteArrayOutputStream();
		for (int i = 0; i < data.length; i++) {

			ByteArrayInputStream singleDataReader = new ByteArrayInputStream(data[i]);
			CompressUtil.compress("" + i, singleDataReader, data[i].length / 2,
					compressOut, 0);
			Assert.assertEquals(data[i].length / 2, singleDataReader.available());
		}

		final int compresssedDataLength = compressOut.size();

		// 压缩数据后的无关数据
		final int extraDataLength = 100_000;
		compressOut.write(new byte[extraDataLength]);

		byte[] result = compressOut.toByteArray();
		// FileIO.writeFile("c:/test.zip", result);

		// decompress
		final ByteArrayInputStream compressedDataReader = new ByteArrayInputStream(result);
		CompressUtil.deCompress(new CompressUtil.EntryPicker() {

			@Override
			public void getEntry(ZipEntry entry, InputStream in) {

				int len = 0;
				byte[] buf = new byte[10_000];
				ByteArrayOutputStream deCompOut = new ByteArrayOutputStream();

				try {
					while ((len = in.read(buf)) > -1) {
						deCompOut.write(buf, 0, len);
					}
					byte[] oriData = data[Integer.parseInt(entry.getName())];

					Assert.assertArrayEquals(
							Arrays.copyOf(oriData, oriData.length / 2),
							deCompOut.toByteArray());
				} catch (Exception e) {
					fail("decompress error. " + e.toString());
				}
			}
		}, compressedDataReader, compresssedDataLength);

		Assert.assertEquals(extraDataLength, compressedDataReader.available());

	}
}
