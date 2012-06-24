
package mysh.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

/**
 * 数据压缩工具.
 * 
 * @author Allen
 * 
 */
public class CompressUtil {

	private static final Logger log = Logger.getLogger(CompressUtil.class);

	/**
	 * 默认缓冲区大小. (10M)
	 */
	public static final int DEFAULT_BUF_SIZE = 10_000_000;

	private CompressUtil() {

	}

	/**
	 * 压缩数据. (完成后不会关闭IO流)<br/>
	 * 此方法会阻塞直到完成(输入流关闭 或 达到允许输入阀值).
	 * 
	 * @param entry
	 *               压缩实体名.
	 * @param in
	 *               数据输入流.
	 * @param maxReadLen
	 *               允许从输入流读取的最大字节数.
	 * @param out
	 *               数据输出流.
	 * @param bufSize
	 *               缓冲区大小. (有效范围: [100KB, 100MB], 默认: 10MB)
	 * @return 操作结果.
	 */
	public static boolean compress(String entry, InputStream in, long maxReadLen,
			OutputStream out, int bufSize) {

		byte[] buf = CompressUtil.getProperBufSize(bufSize);
		CheckedOutputStream cos = new CheckedOutputStream(out, new CRC32());
		ZipOutputStream zos = new ZipOutputStream(cos);
		try {
			ZipEntry ze = new ZipEntry(entry);
			zos.putNextEntry(ze);

			int len = 0;
			while (maxReadLen > 0
					&& (len = in.read(buf, 0, maxReadLen > buf.length ? buf.length
							: (int) maxReadLen)) > -1) {
				zos.write(buf, 0, len);
				maxReadLen -= len;
			}

			zos.closeEntry();
			zos.flush();

			// log.info("压缩成功: " + entry);
			return true;
		} catch (Exception e) {
			log.error("压缩失败: " + entry, e);
			return false;
		}

	}

	/**
	 * 压缩实体拾取器.
	 * 
	 * @author Allen
	 * 
	 */
	public static interface EntryPicker {

		/**
		 * 取到实体. (操作完成后不要关闭输入流.)
		 * 
		 * @param entry
		 *               压缩实体.
		 * @param in
		 *               输入流.
		 */
		public void getEntry(ZipEntry entry, InputStream in);
	}

	/**
	 * 解压缩数据. (完成后不会关闭IO流)<br/>
	 * 此方法会阻塞直到完成(输入流关闭).
	 * 
	 * @param picker
	 *               压缩实体拾取器.
	 * @param in
	 *               数据输入流.
	 * @return 操作结果.
	 */
	public static boolean deCompress(EntryPicker picker, final InputStream in) {

		// CheckedInputStream cis = new CheckedInputStream(new InputStream() {
		//
		// private long availableLength = maxReadLength;
		//
		// @Override
		// public int read() throws IOException {
		//
		// return (this.availableLength-- > 0) ? in.read() : -1;
		// }
		// }, new CRC32());

		CheckedInputStream cis = new CheckedInputStream(in, new CRC32());

		ZipInputStream zis = new ZipInputStream(cis) {

			@Override
			public void close() throws IOException {

				throw new IOException("输入流不允许关闭.");
			}
		};

		try {
			ZipEntry entry = null;
			while ((entry = zis.getNextEntry()) != null) {
				picker.getEntry(entry, zis);
				zis.closeEntry();
			}

			// log.info("解压缩成功.");
			return true;
		} catch (Exception e) {
			log.error("解压缩失败.", e);
			return false;
		}
	}

	/**
	 * 取合适的缓冲区. (有效范围: [100KB, 100MB], 默认: 10MB)
	 * 
	 * @param bufSize
	 *               缓冲区大小.
	 * @return
	 */
	private static byte[] getProperBufSize(int bufSize) {

		if (bufSize > 100_000_000 || bufSize < 100_000) {
			return new byte[DEFAULT_BUF_SIZE];
		}
		return new byte[bufSize];
	}

}
