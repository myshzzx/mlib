
package mysh.dev.filesearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 文本搜索工具.
 *
 * @author ZhangZhx
 * @version Revision 1.0.0
 */
public class FileSearch {

	private static final int CASE_DISTANCE = 'A' - 'a';

	private static final int CHAR_BUF_LENGTH = 400000;

	private static final int THREAD_NUMBER = Runtime.getRuntime().availableProcessors();

	/**
	 * 搜索目标.
	 */
	private String fileDir = "";

	/**
	 * 搜索内容.
	 */
	private char[] searchContent = new char[0];

	/**
	 * 文件过滤表达式.
	 */
	private String fileNameFilterPattern = ".*";

	/**
	 * 文件过滤器.
	 */
	private FileFilter filter = (file) -> file.isDirectory()
					|| file.getName().matches(FileSearch.this.fileNameFilterPattern);

	/**
	 * 文件计数器锁.
	 */
	private final Object countFileLock = new Object();

	/**
	 * 文件计数器.
	 */
	private int countSearchFile = 0;

	/**
	 * 搜索标记.
	 */
	private volatile boolean isSearching = false;

	/**
	 * 文件搜索线程池.
	 */
	private ExecutorService executor = null;

	/**
	 * 信息输出锁.
	 */
	private final Object outputLock = new Object();

	/**
	 * 默认输出插件.
	 */
	private ResultOutputPlugin resultOutput = (msg) -> System.out.println(msg + System.getProperty("line.separator"));

	/**
	 * 结果输出插件.
	 *
	 * @author ZhangZhx
	 * @version Revision 1.0.0
	 */
	public static interface ResultOutputPlugin {

		/**
		 * 追加输出结果，不必是线程安全的.
		 *
		 * @param msg 追加结果
		 */
		void appendResult(String msg);
	}

	public static void main(String[] args) {

		String fileDir = "D:\\eclipse_3.6.2_SDK\\eclipse.ini";
		String searchContent = "MaxPer";
		String fileNamePattern = ".*?\\.ini";

		FileSearch searchEngine = new FileSearch();
		searchEngine.setFileDir(fileDir);
		searchEngine.setSearchContent(searchContent);
		searchEngine.setFileNameFilterPattern(fileNamePattern);
		searchEngine.startSearching();
		// new FileSearchFrame();
	}

	public void setResultOutputPlugin(ResultOutputPlugin resultOutput) {

		this.resultOutput = resultOutput;
	}

	public void setFileDir(String fileDir) {

		this.fileDir = fileDir;
	}

	public void setSearchContent(String searchContent) {

		this.searchContent = searchContent.toLowerCase().toCharArray();
	}

	public void setFileNameFilterPattern(String fileNameFilterPattern) {

		this.fileNameFilterPattern = fileNameFilterPattern;
	}

	/**
	 * 检查初始数据的有效性.
	 */
	private void dataValidate() {

		if (!(new File(this.fileDir)).exists()) {
			throw new RuntimeException("目标不存在");
		}

		if (this.searchContent.length >= FileSearch.CHAR_BUF_LENGTH) {
			throw new RuntimeException("搜索字段长度应小于 " + FileSearch.CHAR_BUF_LENGTH);
		}
	}

	/**
	 * 开始搜索.
	 */
	public synchronized void startSearching() {

		this.dataValidate();

		this.isSearching = true;

		try {
			this.resultOutput.appendResult("search start : " + new Date().toString());
			this.resultOutput.appendResult("file or dir : " + this.fileDir);
			this.resultOutput.appendResult("search for : " + new String(this.searchContent));
			this.resultOutput.appendResult("file filter : " + this.fileNameFilterPattern);
			this.resultOutput.appendResult("");
			this.countSearchFile = 0;

			long startTime = System.nanoTime();
			this.executor = Executors.newFixedThreadPool(FileSearch.THREAD_NUMBER);
			this.recursion(new File(this.fileDir));
			this.executor.shutdown();

			this.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

			this.resultOutput.appendResult("");
			this.resultOutput.appendResult("total searched file : " + this.countSearchFile);
			this.resultOutput.appendResult("total cost time(mm) : " + ((System.nanoTime() - startTime) / 1000000));
			this.resultOutput.appendResult("search end.");
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			this.isSearching = false;
			System.gc();
		}
	}

	public void stopSearching() {

		this.isSearching = false;
		synchronized (FileSearch.this.outputLock) {
			this.resultOutput.appendResult("用户终止！");
		}
	}

	/**
	 * 搜索文件. 搜索文件用到数组缓存，缓存管理策略如下： 假设搜索字串长度为10，缓存区长度为1000， 开始时，文件数据从缓存索引 10 位置开始写入数据， 写满后搜索索引位置
	 * [0, 999] 的数据， 若未完成搜索，复制缓存区 [990, 999] 的数据到 [0, 9]， 继续读文件，仍从 10 索引位置开始写入，继续操作直到找到结果或文件结束
	 *
	 * @param file 搜索文件
	 * @return 搜索结果 char 索引位置，搜索无结果则返回 -1
	 */
	private int searchFile(File file) {

		if (!this.isSearching) {
			return -1;
		}

		this.countSearchFile();
		char[] charBuf = new char[FileSearch.CHAR_BUF_LENGTH];

		if (!file.isFile()) {
			throw new RuntimeException("it's not a file");
		}

		try (FileReader fileReader = new FileReader(file);
		     BufferedReader reader = new BufferedReader(fileReader)) {
			// 读取到缓存的数据长度
			int length = -1;
			// 搜索数据长度
			int searchContentLength = this.searchContent.length;
			// 最大的缓存读取长度
			int maxReaderBufLength = charBuf.length - this.searchContent.length;
			// 当前缓存的起始索引在整个数据流中的位置
			int currentBufPos = 0 - searchContentLength;
			// 搜索结果索引号
			int searchCharResultIndex = -1;
			while ((length = reader.read(charBuf, searchContentLength, maxReaderBufLength)) != -1) {

				if ((searchCharResultIndex = this.searchCharIgnoreCase(charBuf, 0,
								searchContentLength + length, this.searchContent)) != -1) {
					reader.close();
					return currentBufPos + searchCharResultIndex;
				}

				if (length == maxReaderBufLength) {
					System.arraycopy(charBuf, charBuf.length - searchContentLength,
									charBuf, 0, searchContentLength);
				}

				currentBufPos += length;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1;
	}

	/**
	 * 搜索文件计数.
	 */
	private void countSearchFile() {

		synchronized (this.countFileLock) {
			this.countSearchFile++;
		}
	}

	/**
	 * 搜索字节数组.
	 *
	 * @param source     目标数组
	 * @param offset     目标数组起始搜索位置
	 * @param length     目标数组搜索长度
	 * @param searchChar 搜索数组
	 * @return 搜索结果索引位置，失败则返回 -1
	 */
	private int searchCharIgnoreCase(char[] source, int offset, int length, char[] searchChar) {

		if (!this.isSearching) {
			return -1;
		}

		if (length < searchChar.length || source.length < offset + length
						|| source.length <= offset || offset < 0 || length < 1
						|| searchChar.length < 1) {
			throw new RuntimeException("are you kidding?");
		}

		int topIndex = offset + length - searchChar.length + 1;
		int searchIndex;
		SOURCE_INDEX:
		for (int sourceIndex = offset; sourceIndex < topIndex; sourceIndex++) {
			if (this.compareIngoreCase(source[sourceIndex], searchChar[0])) {
				searchIndex = 0;
				while (++searchIndex < searchChar.length) {
					if (!this.compareIngoreCase(source[sourceIndex + searchIndex],
									searchChar[searchIndex])) {
						continue SOURCE_INDEX;
					}
				}
				return sourceIndex;
			}
		}

		return -1;
	}

	/**
	 * 比较两个字符是否相同（忽略大小写）.
	 *
	 * @param source    要比较的的一个字符
	 * @param lowerCase 要比较的另一个字符（小写字符）
	 * @return 是否相同
	 */
	private boolean compareIngoreCase(char source, char lowerCase) {

		return source == lowerCase || (source >= 'A' && source <= 'Z' && source
						- FileSearch.CASE_DISTANCE == lowerCase);
	}

	/**
	 * 递归处理文件夹.
	 *
	 * @param dirOrFile 文件夹
	 */
	private void recursion(final File dirOrFile) {

		if (!this.isSearching) {
			return;
		}

		if (dirOrFile.isFile()) {
			this.executor.submit(new Runnable() {

				@Override
				public void run() {

					int resultIndex = FileSearch.this.searchFile(dirOrFile);
					if (resultIndex != -1) {
						synchronized (FileSearch.this.outputLock) {
							FileSearch.this.resultOutput.appendResult(dirOrFile.toString());
						}
					}
				}
			});
		} else if (dirOrFile.isDirectory()) {
			File[] children = dirOrFile.listFiles(this.filter);
			if (children != null) {
				for (File tempFile : children) {
					this.recursion(tempFile);
				}
			}
		}
	}
}
