
package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.*;

/**
 * 数据压缩工具.
 *
 * @author Allen
 */
public class Compresses {

	/**
	 * 压缩实体拾取器.
	 *
	 * @author Allen
	 */
	public static interface EntryPicker {

		/**
		 * 取到实体. (操作完成后不要关闭输入流.)
		 *
		 * @param entry 压缩实体.
		 * @param in    输入流.
		 */
		void getEntry(ZipEntry entry, InputStream in);
	}

	private static final Logger log = LoggerFactory.getLogger(Compresses.class);

	/**
	 * 默认缓冲区大小. (10M)
	 */
	public static final int DEFAULT_BUF_SIZE = 10_000_000;

	private Compresses() {
	}

	/**
	 * 取数据输出流, 将数据输出流输出的数据压缩输出.<br/>
	 * 此方法会启动一个压缩线程来完成压缩任务, 任务会一直运行, 直到数据输出流关闭.<br/>
	 * 数据输出流输出上限为 {@link Long#MAX_VALUE}, 超过上限则无法输出, 并结束压缩.<br/>
	 * 警告: 压缩输出流 必须在压缩任务结束后才能关闭, 通过 result 里的任务引用来确认压缩结束.<br/>
	 * <pre>
	 *   demo:
	 *   dataOut.close();
	 *   result.get().get();
	 *   compressedDataOut.close();
	 * </pre>
	 *
	 * @param entry             压缩实体名.
	 * @param compressedDataOut 压缩输出流.
	 * @param bufSize           缓冲区大小. (有效范围: [100KB, 100MB], 默认: 10MB)
	 * @param result            压缩任务引用.
	 * @return 数据输出流.
	 */
	public static OutputStream getCompressOutputStream(
					String entry, OutputStream compressedDataOut, int bufSize,
					AtomicReference<RunnableFuture<Boolean>> result) throws IOException {
		if (entry == null || compressedDataOut == null || result == null)
			throw new IllegalArgumentException();

		PipedOutputStream dataOut = new PipedOutputStream();
		PipedInputStream dataIn = new PipedInputStream(dataOut, Compresses.DEFAULT_BUF_SIZE);

		RunnableFuture<Boolean> task = new FutureTask<>(
						() -> compress(entry, dataIn, Long.MAX_VALUE, compressedDataOut, bufSize));
		result.set(task);
		new Thread(task).start();
		return dataOut;
	}

	/**
	 * 压缩数据. (完成后不会关闭IO流)<br/>
	 * 此方法会阻塞直到完成(输入流关闭 或 达到允许输入阀值).
	 *
	 * @param entry      压缩实体名.
	 * @param in         数据输入流.
	 * @param maxReadLen 允许从输入流读取的最大字节数.
	 * @param out        数据输出流.
	 * @param bufSize    缓冲区大小. (有效范围: [100KB, 100MB], 默认: 10MB)
	 * @return 操作结果.
	 */
	public static boolean compress(String entry, InputStream in, long maxReadLen,
	                               OutputStream out, int bufSize) {

		byte[] buf = Compresses.getProperBufSize(bufSize);
		CheckedOutputStream cos = new CheckedOutputStream(out, new CRC32());
		ZipOutputStream zos = new ZipOutputStream(cos);
		try {
			ZipEntry ze = new ZipEntry(entry);
			zos.putNextEntry(ze);

			int len;
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
	 * 解压缩数据. (完成后不会关闭IO流)<br/>
	 * 此方法会阻塞直到完成(输入流关闭或输入结束).
	 *
	 * @param picker 压缩实体拾取器.
	 * @param in     数据输入流(不要关闭).
	 * @return 操作结果.
	 */
	public static boolean deCompress(EntryPicker picker, final InputStream in) {

		CheckedInputStream cis = new CheckedInputStream(in, new CRC32());

		ZipInputStream zis = new ZipInputStream(cis) {
			@Override
			public void close() throws IOException {
				throw new IOException("输入流不允许关闭.");
			}
		};

		try {
			ZipEntry entry;
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
	 * @param bufSize 缓冲区大小.
	 */
	private static byte[] getProperBufSize(int bufSize) {

		if (bufSize > 100_000_000 || bufSize < 100_000) {
			return new byte[DEFAULT_BUF_SIZE];
		}
		return new byte[bufSize];
	}

}
