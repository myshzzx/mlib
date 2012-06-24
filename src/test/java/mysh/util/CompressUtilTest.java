
package mysh.util;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.ZipEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CompressUtilTest {

	private byte[][] datas;

	@Before
	public void prepare() {

		this.datas = new byte[][] { new byte[0], new byte[1000], new byte[2000] };

		Random r = new Random();

		for (byte[] ba : this.datas) {
			r.nextBytes(ba);
		}
	}

	@Test
	public void commonTest() throws IOException {

		// compress
		ByteArrayOutputStream compressOut = new ByteArrayOutputStream();
		for (int i = 0; i < this.datas.length; i++) {

			ByteArrayInputStream singleDataReader = new ByteArrayInputStream(this.datas[i]);
			CompressUtil.compress("" + i, singleDataReader, this.datas[i].length / 2,
					compressOut, 0);
			Assert.assertEquals(this.datas[i].length / 2, singleDataReader.available());
		}

		// CompressUtil.finishCompress(compressOut);
//		final int compresssedDataLength = compressOut.size();

		// 压缩数据后的无关数据
		final int extraDataLength = 100_000;
		compressOut.write(new byte[extraDataLength]);

		byte[] result = compressOut.toByteArray();
		// FileIO.writeFile("c:/test.zip", result);

		// decompress
		final ByteArrayInputStream compressedDataReader = new ByteArrayInputStream(result);

		// define picker
		CompressUtil.EntryPicker picker = new CompressUtil.EntryPicker() {

			@Override
			public void getEntry(ZipEntry entry, InputStream in) {

				int len = 0;
				byte[] buf = new byte[10_000];
				ByteArrayOutputStream deCompOut = new ByteArrayOutputStream();

				try {
					while ((len = in.read(buf)) > -1) {
						deCompOut.write(buf, 0, len);
					}
					byte[] oriData = CompressUtilTest.this.datas[Integer.parseInt(entry.getName())];

					Assert.assertArrayEquals(
							Arrays.copyOf(oriData, oriData.length / 2),
							deCompOut.toByteArray());
				} catch (Exception e) {
					fail("decompress error. " + e.toString());
				}
			}
		};

		// save state for reset
//		compressedDataReader.mark(Integer.MAX_VALUE);

		// 无限制解压范围测试
		CompressUtil.deCompress(picker, compressedDataReader);
		Assert.assertTrue(extraDataLength != compressedDataReader.available());

		// 限制解压范围测试
//		compressedDataReader.reset();
//		CompressUtil.deCompress(picker, compressedDataReader, compresssedDataLength);
//		Assert.assertTrue(extraDataLength == compressedDataReader.available());

	}

	@Test
	public void fileTest() throws Exception {

		long maxReadLen = Long.MAX_VALUE;
		OutputStream out = new FileOutputStream("e:/test/test.zip");

		CompressUtil.compress("test/a/0", new ByteArrayInputStream(this.datas[0]),
				maxReadLen, out, 0);
		CompressUtil.compress("test/1", new ByteArrayInputStream(this.datas[1]), maxReadLen,
				out, 0);
		CompressUtil.compress("2.5", new ByteArrayInputStream(this.datas[2]),
				this.datas[2].length / 2, out, 0);
		CompressUtil.compress("2", new ByteArrayInputStream(this.datas[2]), maxReadLen, out,
				0);
		CompressUtil.compress("test/a/2", new ByteArrayInputStream(this.datas[2]),
				maxReadLen, out, 0);
		CompressUtil.compress("test/a/1.5", new ByteArrayInputStream(this.datas[1]),
				this.datas[1].length / 2, out, 0);
		CompressUtil.compress("test/a/1", new ByteArrayInputStream(this.datas[1]),
				maxReadLen, out, 0);

	}
}
