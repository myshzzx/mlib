
package mysh.dev.filesearch;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class FileIterator {

	private File root;

	private FileFilter filter;

	private FileHandler handler;

	private ExecutorService executor = Executors.newFixedThreadPool(4);

	/**
	 * 注意：此实例将在多线程环境中执行，必需保证实现类的线程安全性
	 */
	public static interface FileHandler {

		void handle(File file);
	}

	public static void main(String[] args) {

		FileIterator iterator = new FileIterator(new File("F:\\JEE-Project\\vp5Crack\\src"),
						null, file -> {

			if (file.getPath().endsWith(".jad")) {
				file.renameTo(new File(file.getPath().substring(0,
								file.getPath().length() - 3)
								+ "java"));
			}
		}
		);
		iterator.doIterate();
	}

	FileIterator(File fileOrDir, FileFilter filter, FileHandler handler) {

		this.root = fileOrDir;
		this.filter = filter;
		this.handler = handler;
	}

	public void doIterate() {

		this.iterateDir(this.root);
		this.executor.shutdown();
	}

	private void iterateDir(final File fileOrDir) {

		if (fileOrDir.isFile()) {
			this.executor.submit(new Runnable() {

				@Override
				public void run() {

					FileIterator.this.handler.handle(fileOrDir);
				}
			});
		} else if (fileOrDir.isDirectory()) {
			for (File child : this.filter == null ? fileOrDir.listFiles() : fileOrDir.listFiles(this.filter)) {
				this.iterateDir(child);
			}
		}
	}
}
