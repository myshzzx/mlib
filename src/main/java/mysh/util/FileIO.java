
package mysh.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.apache.log4j.Logger;

/**
 * 文件工具类.
 * 
 * @author ZhangZhx
 * 
 */
public class FileIO {

	private static final Logger log = Logger.getLogger(FileIO.class);

	/**
	 * 取当前目录(结尾不带分隔符).
	 * 
	 * @return
	 */
	public static String getCurrentDirPath() {

		return System.getProperty("user.dir");
	}

	/**
	 * 从文件取得数据.<br/>
	 * 失败则返回 null.
	 * 
	 * @param 文件路径
	 *               .
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getObjectFromFile(String filepath) {

		if (!new File(filepath).exists()) {
			log.error(filepath + " 不存在, 加载文件失败.");
			return null;
		}

		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(new FileInputStream(filepath));
			T o = (T) in.readObject();
			log.info("从文件加载数据成功: " + filepath);
			return o;
		} catch (Exception e) {
			log.error("从文件加载数据失败: " + filepath, e);
			return null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
				}
			}
			System.gc();
		}
	}

	/**
	 * 从文件取得数据(使用内存缓存, 可大幅提升反序列化速度).<br/>
	 * 失败则返回 null.
	 * 
	 * @param filepath
	 *               文件路径.
	 * @param maxLength
	 *               文件最大长度, 超过此长度将失败.
	 * @return
	 */
	public static <T> T getObjectFromFileWithBuf(String filepath, int maxLength) {

		byte[] buf;
		try {
			buf = readFileToByteArray(filepath, maxLength);
			T obj = getObjectFromByteArray(buf);
			log.info("从文件加载数据成功: " + filepath);
			return obj;
		} catch (Exception e) {
			log.error("从文件加载数据失败: " + filepath, e);
			return null;
		} finally {
			buf = null;
			System.gc();
		}
	}

	/**
	 * 从缓存反序列化数据. 失败则抛异常.
	 * 
	 * @param buf
	 *               缓存.
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getObjectFromByteArray(byte[] buf) throws Exception {

		if (buf == null) {
			throw new NullPointerException();
		}

		try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buf))) {
			return (T) in.readObject();
		} catch (Exception e) {
			log.error("从缓存反序列化数据失败: ", e);
			throw e;
		}
	}

	/**
	 * 将文件数据读到缓存. 失败抛异常.
	 * 
	 * @param filepath
	 *               文件路径.
	 * @return
	 * @throws Exception
	 */
	public static byte[] readFileToByteArray(String filepath, int maxLengh) throws Exception {

		File file = new File(filepath);
		if (!file.exists() || !file.canRead()) {
			throw new IllegalArgumentException(filepath + " 不存在或不可读, 加载文件失败.");
		}

		if (file.length() > maxLengh) {
			throw new IllegalArgumentException("要读取的文件大小 (" + file.length() + ") 超出指定大小: "
					+ maxLengh);
		}

		try (FileInputStream in = new FileInputStream(filepath)) {
			byte[] buf = new byte[(int) file.length()];
			in.read(buf);
			return buf;
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * 将数据写入到文件.
	 * 
	 * @param filepath
	 *               文件路径.
	 * @param obj
	 *               要写入的对象.
	 * @return 写入成功则返回 true;
	 */
	public static boolean writeObjectToFile(String filepath, Object obj) {

		ObjectOutput out = null;
		try {
			File file = new File(filepath);
			file.getParentFile().mkdirs();
			if (!file.exists())
				file.createNewFile();

			out = new ObjectOutputStream(new FileOutputStream(file));
			out.writeObject(obj);
			log.info("写文件成功: " + file.getPath());
			return true;
		} catch (Exception e) {
			log.error("写文件失败: " + filepath, e);
			return false;
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (Exception e) {
				}
				System.gc();
			}
		}
	}

	/**
	 * 将数据写入文件.
	 * 
	 * @param filepath
	 *               文件路径.
	 * @param data
	 *               要写入数据.
	 * @return
	 */
	public static boolean writeFile(String filepath, byte[] data) {

		FileOutputStream out = null;
		try {
			File file = new File(filepath);
			file.getParentFile().mkdirs();
			if (!file.exists())
				file.createNewFile();
			out = new FileOutputStream(file);
			out.write(data);
			log.info("写文件成功: " + file.getPath());
			return true;
		} catch (Exception e) {
			log.error("写文件失败: " + filepath, e);
			return false;
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (Exception e) {
				}
				System.gc();
			}
		}
	}
}
