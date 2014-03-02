package mysh.imagesearch;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Mysh
 * @since 13-12-23 下午4:58
 */
public class FileUtil {
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
	 * @param filter  文件过滤器. 为 null 表示不过滤.
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
