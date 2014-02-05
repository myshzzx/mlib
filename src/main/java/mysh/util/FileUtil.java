
package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * 文件工具类.
 *
 * @author ZhangZhx
 */
public class FileUtil {
	private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

	/**
	 * 取当前目录(结尾不带分隔符).
	 */
	public static String getCurrentDirPath() {

		return System.getProperty("user.dir");
	}

	/**
	 * 取相对于当前目录的完整路径.
	 *
	 * @param filename 文件名.
	 */
	public static String getAbstractPath(String filename) {
		return System.getProperty("user.dir") + System.getProperty("file.separator") + filename;
	}

	/**
	 * 取文件输入流.<br/>
	 * 失败时返回 null.
	 *
	 * @param filepath 文件路径.
	 */
	public static FileInputStream getFileInputStream(String filepath) {

		try {
			return new FileInputStream(filepath);
		} catch (FileNotFoundException e) {
			log.error("打开文件失败: " + filepath, e);
			return null;
		}
	}

	/**
	 * 取文件输出流. (文件不存在时自动创建)<br/>
	 * 失败时返回 null.
	 *
	 * @param filepath 文件路径.
	 */
	public static FileOutputStream getFileOutputStream(String filepath) {

		File file = new File(filepath);

		try {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}

			return new FileOutputStream(file);
		} catch (Exception e) {
			log.error("准备写入文件失败: " + filepath, e);
			return null;
		}
	}

	/**
	 * 从文件取得数据.<br/>
	 * 失败则返回 null.
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
		}
	}

	/**
	 * 从文件取得数据(使用内存缓存, 可大幅提升反序列化速度).<br/>
	 * 失败则返回 null.
	 *
	 * @param filepath  文件路径.
	 * @param maxLength 文件最大长度, 超过此长度将失败.
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
		}
	}

	/**
	 * 从缓存反序列化数据. 失败则抛异常.
	 *
	 * @param buf 缓存.
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
	 * 将文件数据读到缓存.<br/>
	 * 不会返回 null. 失败抛异常.
	 *
	 * @param filepath 文件路径.
	 * @param maxLengh 文件大小限制.
	 */
	public static byte[] readFileToByteArray(String filepath, int maxLengh) throws Exception {

		File file = new File(filepath);
		if (!file.isFile() || !file.exists() || !file.canRead()) {
			throw new IllegalArgumentException(filepath + " 不存在或不可读, 加载文件失败.");
		}

		if (file.length() > maxLengh) {
			throw new IllegalArgumentException("要读取的文件大小 (" + file.length() + ") 超出指定大小: " + maxLengh);
		}

		try (FileInputStream in = new FileInputStream(filepath)) {
			byte[] buf = new byte[(int) file.length()];
			in.read(buf);
			return buf;
		}
	}

	/**
	 * 将数据写入到文件.
	 *
	 * @param filepath 文件路径.
	 * @param obj      要写入的对象.
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
			}
		}
	}

	/**
	 * 将数据写入文件.
	 *
	 * @param filepath 文件路径.
	 * @param data     要写入数据.
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

	/**
	 * 取文件扩展名(小写).
	 *
	 * @param filepath 文件路径(可以是相对路径).
	 * @return 文件扩展名. 不包含点.
	 */
	public static String getFileExtension(String filepath) {

		String filename = new File(filepath).getName().trim().toLowerCase();
		if (filename.length() == 0)
			return "";
		if (filename.charAt(filename.length() - 1) == '.')
			filename = filename.substring(0, filename.length() - 1);

		int lastPointIndex = filename.lastIndexOf('.');
		if (lastPointIndex < 0)
			return "";
		else
			return filename.substring(lastPointIndex + 1, filename.length());

	}

	/**
	 * 取不含扩展名的文件名.
	 *
	 * @param filepath 文件路径(可以是相对路径).
	 */
	public static String getFileNameWithoutExtention(String filepath) {

		String filename = new File(filepath).getName().trim();
		if (filename.length() == 0)
			return "";
		if (filename.charAt(filename.length() - 1) == '.')
			filename = filename.substring(0, filename.length() - 1);

		int lastPointIndex = filename.lastIndexOf('.');
		if (lastPointIndex < 0)
			return filename;
		else if (lastPointIndex == 0)
			return "";
		else
			return filename.substring(0, lastPointIndex);
	}

	/**
	 * 取给定文件路径可写的File, 若文件已存在, 则在文件名后加 "(数字)", 以保证可写入.<br/>
	 * 如若给定的路径 c:/a.txt, 目录下 a.txt 和 a (1).txt 已存在, 则返回 c:/a (2).txt 的可写文件.<br/>
	 * 注意: 可写文件的状态是瞬时的, 此方法不能锁定此文件, 它随时可能被其他程序锁定.
	 *
	 * @param filePath 文件路径(可以是相对路径).
	 * @return 带绝对路径的 File.
	 */
	public static File getWritableFile(String filePath) {

		if (new File(filePath).getAbsoluteFile().exists()) {
			String dir = new File(filePath).getAbsoluteFile().getParent() + File.separatorChar;
			String fName = FileUtil.getFileNameWithoutExtention(filePath);
			String fExt = FileUtil.getFileExtension(filePath);
			int i = 0;
			while (new File(filePath = (dir + fName + " (" + (++i) + ")" + (fExt.length() > 0 ? ("." + fExt)
							: ""))).exists())
				;
		}

		return new File(filePath).getAbsoluteFile();
	}

	/**
	 * 文件处理任务.
	 */
	public static interface FileTask {
		void handle(File f);
	}

	/**
	 * 递归处理(含子目录)目录下的所有文件.<br/>
	 * 若处理器抛异常, 处理将中断.
	 *
	 * @param dirRoot 目录.
	 * @param filter  文件过滤器. (为 null 表示不过滤文件)
	 * @param handler 文件处理器.
	 */
	public static void recurDir(File dirRoot, FileFilter filter, FileTask handler) {
		if (dirRoot == null || !dirRoot.isDirectory() || handler == null)
			throw new IllegalArgumentException();

		List<File> dirs = new LinkedList<>();
		dirs.add(dirRoot);

		File dir;
		while (!dirs.isEmpty()) {
			dir = dirs.remove(0);
			for (File child : dir.listFiles()) {
				if (child.isDirectory()) dirs.add(child);
				else if (filter == null || filter.accept(child)) {
					handler.handle(child);
				}
			}
		}
	}
}
