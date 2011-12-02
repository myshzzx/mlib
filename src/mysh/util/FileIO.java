
package mysh.util;

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
	 * 取当前目录.
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

			out = new ObjectOutputStream(new FileOutputStream(filepath));
			out.writeObject(obj);
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
