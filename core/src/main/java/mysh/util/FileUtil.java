
package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
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
	 * 文件处理任务.
	 */
	public static interface FileTask {
		void handle(File f);
	}

	/**
	 * 从文件取得数据.<br/>
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getObjectFromFile(String filepath) throws IOException, ClassNotFoundException {

		if (!new File(filepath).exists()) {
			log.error(filepath + " 不存在, 加载文件失败.");
			return null;
		}

		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filepath))) {
			T o = (T) in.readObject();
			log.debug("load object from file: " + filepath);
			return o;
		}
	}

	/**
	 * 从文件取得数据(使用内存缓存, 可大幅提升反序列化速度).<br/>
	 * 失败则返回 null.
	 *
	 * @param filepath 文件路径.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getObjectFromFileWithBuf(String filepath) throws IOException, ClassNotFoundException {
		byte[] buf = Files.readAllBytes(Paths.get(filepath));
		T obj = SerializeUtil.unSerialize(buf);
		log.debug("load object from file: " + filepath);
		return obj;
	}

	/**
	 * 将数据写入到文件.
	 *
	 * @param filepath 文件路径.
	 * @param obj      要写入的对象.
	 */
	public static void writeObjectToFile(String filepath, Object obj) throws IOException {
		File file = ensureWritable(filepath);
		try (ObjectOutput out = new ObjectOutputStream(new FileOutputStream(file))) {
			out.writeObject(obj);
			log.debug("written file: " + file.getPath());
		}
	}

	/**
	 * make sure filepath writable. if not, return null.
	 */
	public static File ensureWritable(String filepath) throws IOException {
		File file = new File(filepath).getAbsoluteFile();
		file.getParentFile().mkdirs();
		if (!file.exists())
			file.createNewFile();
		return file;
	}

	/**
	 * 将数据写入文件.
	 *
	 * @param filepath 文件路径.
	 * @param data     要写入数据.
	 */
	public static void writeFile(String filepath, byte[] data) throws IOException {
		File file = ensureWritable(filepath);
		try (FileOutputStream out = new FileOutputStream(file)) {
			out.write(data);
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

		while (!dirs.isEmpty()) {
			File[] children = dirs.remove(0).listFiles();

			if (children != null) {
				for (File child : children) {
					if (child.isDirectory()) dirs.add(child);
					else if (filter == null || filter.accept(child)) {
						handler.handle(child);
					}
				}
			}
		}
	}

	public static long folderSize(String path) throws IOException {
		File dir = new File(path).getAbsoluteFile();
		if (!dir.isDirectory())
			throw new IllegalArgumentException("not directory: " + path);

		return Files.walk(Paths.get(dir.getAbsolutePath())).mapToLong(p -> {
			try {
				return Files.size(p);
			} catch (IOException e) {
				log.debug("get file size error: " + p, e);
				return 0;
			}
		}).sum();
	}
}
