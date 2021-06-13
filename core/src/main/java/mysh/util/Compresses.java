
package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import java.io.*;
import java.util.Objects;
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
	public interface EntryPicker {
		
		/**
		 * 取到实体. (操作完成后不要关闭输入流.)
		 *
		 * @param entry 压缩实体.
		 * @param in    输入流.
		 */
		void getEntry(ZipEntry entry, InputStream in);
	}
	
	private static final Logger log = LoggerFactory.getLogger(Compresses.class);
	
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
	 * @param bufSize           缓冲区大小. (有效范围: [100KB, 10MB], 默认: 10MB)
	 * @param result            压缩任务引用.
	 * @return 数据输出流.
	 */
	public static OutputStream getCompressOutputStream(
			String entry, OutputStream compressedDataOut, int bufSize,
			AtomicReference<RunnableFuture<Object>> result) throws IOException {
		if (entry == null || compressedDataOut == null || result == null)
			throw new IllegalArgumentException();
		
		PipedOutputStream dataOut = new PipedOutputStream();
		PipedInputStream dataIn = new PipedInputStream(dataOut, getProperBufSize(bufSize));
		
		RunnableFuture<Object> task = new FutureTask<>(
				() -> {
					try {
						compress(entry, dataIn, Long.MAX_VALUE, compressedDataOut, bufSize);
						return true;
					} catch (Exception e) {
						return e;
					}
				});
		result.set(task);
		new Thread(task).start();
		return dataOut;
	}
	
	/**
	 * 压缩数据. (完成后不会关闭IO流)<br/>
	 * 此方法会阻塞直到完成(输入流关闭 或 达到允许输入阈值).
	 *
	 * @param entry      压缩实体名.
	 * @param in         数据输入流.
	 * @param maxReadLen 允许从输入流读取的最大字节数.
	 * @param out        数据输出流.
	 * @param bufSize    缓冲区大小. (有效范围: [100KB, 10MB], 默认: 100KB)
	 */
	public static void compress(String entry, InputStream in, long maxReadLen,
	                            OutputStream out, int bufSize) {
		if (maxReadLen == 0) return;
		if (maxReadLen < 0) throw new IllegalArgumentException("maxReadLen mustn't be negative");
		
		try (
				CheckedOutputStream cos = new CheckedOutputStream(out, new CRC32()) {
					@Override
					public void close() throws IOException {
						flush();
					}
				};
				ZipOutputStream zos = new ZipOutputStream(cos)
		) {
			zos.setLevel(9);
			ZipEntry ze = new ZipEntry(entry);
			zos.putNextEntry(ze);
			
			int len;
			byte[] buf = new byte[Compresses.getProperBufSize(bufSize)];
			while (maxReadLen > 0
					&& (len = in.read(buf, 0, maxReadLen > buf.length ? buf.length
					: (int) maxReadLen)) > -1) {
				zos.write(buf, 0, len);
				maxReadLen -= len;
			}
			
			zos.closeEntry();
			zos.flush();
			
			// log.info("压缩成功: " + entry);
		} catch (Exception e) {
			log.error("压缩失败: " + entry, e);
			throw Exps.unchecked(e);
		}
	}
	
	/**
	 * 解压缩数据. (完成后不会关闭IO流)<br/>
	 * 此方法会阻塞直到完成(输入流关闭或输入结束).
	 *
	 * @param picker 压缩实体拾取器.
	 * @param in     数据输入流(不关闭).
	 * @return 操作结果.
	 */
	public static void deCompress(EntryPicker picker, final InputStream in) {
		try (CheckedInputStream cis = new CheckedInputStream(in, new CRC32()) {
			@Override
			public void close() {
				// do not close in
			}
		};
		     ZipInputStream zis = new ZipInputStream(cis)
		) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				picker.getEntry(entry, zis);
				zis.closeEntry();
			}
		} catch (Exception e) {
			throw Exps.unchecked(e);
		}
	}
	
	/**
	 * 取合适的缓冲区. (有效范围: [100KB, 10MB])
	 *
	 * @param bufSize 缓冲区大小.
	 */
	private static int getProperBufSize(int bufSize) {
		return Range.within(100_000, 10_000_000, bufSize);
	}
	
	/**
	 * 压缩数据(zip/deflate 格式).
	 * 256字节以下建议不压缩.
	 */
	public static byte[] compressZip(byte[] data) {
		return compressZip(data, 9);
	}
	
	/**
	 * 压缩数据(zip/deflate 格式).
	 * 256字节以下建议不压缩.
	 *
	 * @param compressionLevel 0-9 the higher level, the harder compress
	 */
	public static byte[] compressZip(byte[] data, int compressionLevel) {
		try (
				ByteArrayOutputStream bo = new ByteArrayOutputStream(Range.within(32, 32 + data.length, data.length >> 1));
				ZipOutputStream zos = new ZipOutputStream(new CheckedOutputStream(bo, new CRC32()))
		) {
			Tick tick = Tick.tick();
			
			zos.setLevel(Range.within(0, 9, compressionLevel));
			ZipEntry ze = new ZipEntry("z");
			zos.putNextEntry(ze);
			zos.write(data);
			zos.closeEntry();
			zos.close();
			
			byte[] a = bo.toByteArray();
			if (tick.nip() > 1000)
				log.debug("compressZip cost {} ms, cl={}, before/after={}/{}", tick.lastNip(), compressionLevel, data.length, a.length);
			return a;
		} catch (Exception e) {
			throw Exps.unchecked(e);
		}
	}
	
	/**
	 * 解压缩数据(zip/deflate 格式).
	 */
	public static byte[] decompressZip(byte[] data) {
		if (!isZip(data))
			throw new IllegalArgumentException("NOT zip data");
		
		Tick tick = Tick.tick();
		try (ZipInputStream zis = new ZipInputStream(new CheckedInputStream(new ByteArrayInputStream(data), new CRC32()))) {
			ZipEntry entry = zis.getNextEntry();
			
			ByteArrayOutputStream bo = new ByteArrayOutputStream(Range.within(32, 32 + data.length * 2, data.length * 2));
			int len;
			byte[] buf = new byte[Range.within(32, 1 << 18, data.length)];
			while ((len = zis.read(buf)) > 0)
				bo.write(buf, 0, len);
			zis.closeEntry();
			
			byte[] a = bo.toByteArray();
			if (tick.nip() > 1000)
				log.debug("decompressZip cost {} ms, before/after={}/{}", tick.lastNip(), data.length, a.length);
			return a;
		} catch (Exception e) {
			throw Exps.unchecked(e);
		}
	}
	
	/**
	 * 压缩数据(xz/LZMA2格式).
	 * 自动选择压缩级别(按文本类型优化).
	 * 256字节以下建议不压缩.
	 */
	public static byte[] compressXz(byte[] data) {
		int level = (int) (Math.log(1.0 * data.length / (1 << 20)));
		level = Range.within(0, 5, level);
		return compressXz(data, level);
	}
	
	/**
	 * 压缩数据(xz/LZMA2格式).
	 * 256字节以下建议不压缩.
	 * for text, xz(3) may better than zip(9) in time and ratio.
	 *
	 * @param compressionLevel 0即可, 最高建议5
	 *                         <p>
	 *                         0-3 are fast presets with medium compression.
	 *                         4-6 are fairly slow presets with high compression.
	 *                         <p>
	 *                         7-9 are like 6 but use bigger dictionaries
	 *                         and have higher compressor and decompressor memory requirements.
	 *                         Unless the uncompressed size of the file exceeds 8&nbsp;MiB,
	 *                         16&nbsp;MiB, or 32&nbsp;MiB, it is waste of memory to use
	 *                         7, 8, or 9, respectively.
	 */
	public static byte[] compressXz(byte[] data, int compressionLevel) {
		Objects.requireNonNull(data, "compress data is null");
		
		Tick tick = Tick.tick();
		ByteArrayOutputStream ro = new ByteArrayOutputStream(Range.within(32, 32 + data.length, data.length >> 1));
		try (XZOutputStream out = new XZOutputStream(ro, new LZMA2Options(Range.within(0, 9, compressionLevel)))) {
			out.write(data);
		} catch (Exception e) {
			throw Exps.unchecked(e);
		}
		byte[] a = ro.toByteArray();
		if (tick.nip() > 1000)
			log.debug("compressXz cost {} ms, cl={}, before/after={}/{}", tick.lastNip(), compressionLevel, data.length, a.length);
		return a;
	}
	
	/**
	 * 解压缩数据(xz/LZMA2格式).
	 */
	public static byte[] decompressXz(byte[] data) {
		if (!isXz(data))
			throw new IllegalArgumentException("NOT xz data");
		
		Tick tick = Tick.tick();
		ByteArrayOutputStream ro = new ByteArrayOutputStream(Range.within(32, 32 + data.length * 2, data.length * 2));
		try (XZInputStream in = new XZInputStream(new ByteArrayInputStream(data))) {
			byte[] buf = new byte[Range.within(32, 1 << 18, data.length)];
			int len;
			while ((len = in.read(buf)) > 0) {
				ro.write(buf, 0, len);
			}
			
			byte[] a = ro.toByteArray();
			if (tick.nip() > 1000)
				log.debug("decompressXz cost {} ms, before/after={}/{}", tick.lastNip(), data.length, a.length);
			return a;
		} catch (Exception e) {
			throw Exps.unchecked(e);
		}
	}
	
	/**
	 * https://en.wikipedia.org/wiki/List_of_file_signatures
	 */
	private static final byte[] xzSignature = new byte[]{(byte) 0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00};
	private static final byte[] zipSignature = new byte[]{0x50, 0x4B, 0x03, 0x04};
	
	public static boolean isXz(byte[] d) {
		return hasSignature(d, xzSignature);
	}
	
	public static boolean isZip(byte[] d) {
		return hasSignature(d, zipSignature);
	}
	
	private static boolean hasSignature(byte[] d, byte[] signature) {
		if (d == null || d.length <= signature.length)
			return false;
		
		for (int i = 0; i < signature.length; i++) {
			if (signature[i] != d[i])
				return false;
		}
		return true;
	}
}
