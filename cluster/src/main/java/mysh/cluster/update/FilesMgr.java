package mysh.cluster.update;

import mysh.util.OSUtil;
import mysh.util.Serializer;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Mysh
 * @since 2014/12/10 19:21
 */
public class FilesMgr implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(FilesMgr.class);

	private static final String mainDir = "main";
	private static final String updateDir = "update";

	public static enum FileType {
		/**
		 * cluster's core files
		 */
		CORE("core"),
		/**
		 * user files
		 */
		USER("user");

		private String dir;

		FileType(String dir) {
			this.dir = dir;
		}

	}

	public static enum UpdateType {
		UPDATE, DELETE;
	}

	static {
		new File(mainDir, FileType.CORE.dir).mkdirs();
		new File(mainDir, FileType.USER.dir).mkdirs();
		new File(updateDir, FileType.CORE.dir).mkdirs();
	}

	public final Serializer.ClassLoaderFetcher clFetcher = () -> FilesMgr.this.cl;

	private volatile URLClassLoader cl;
	private volatile FilesInfo old;

	private final FilesInfo curr = new FilesInfo();

	public FilesMgr() throws IOException {
		curr.coreFiles = new HashMap<>();
		curr.userFiles = new HashMap<>();

		for (FileType type : Arrays.asList(FileType.CORE, FileType.USER))
			Files.walk(Paths.get(mainDir, type.dir), 1).forEach(p -> {
				if (!p.toFile().isFile()) return;
				try {
					final String fileName = p.getFileName().toString();
					final String sha = getThumbStamp(Files.readAllBytes(p));
					if (type == FileType.CORE)
						curr.coreFiles.put(fileName, sha);
					else if (type == FileType.USER)
						curr.userFiles.put(fileName, sha);
				} catch (Exception e) {
					log.error("read file error: " + p, e);
				}
			});

		this.refreshTS();

		old = new FilesInfo(curr);

		renewCl();
	}

	private void renewCl() throws IOException {
		ArrayList<URL> urls = new ArrayList<>();
		Files.walk(Paths.get(mainDir, FileType.USER.dir), 1).forEach(p -> {
			try {
				final File file = p.toFile();
				if (file.isFile() && file.getName().endsWith(".jar"))
					urls.add(new URL("jar:file:///" + file.getAbsolutePath() + "!/"));
			} catch (MalformedURLException e) {
				log.error("get file url error:" + p, e);
			}
		});
		cl = new URLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
	}

	public FilesInfo getFilesInfo() {
		return old;
	}

	private void refreshTS() {
		List<String> tsList = new ArrayList<>(curr.coreFiles.values());
		tsList.addAll(curr.userFiles.values());
		Collections.sort(tsList);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			for (String ts : tsList) {
				out.write(ts.getBytes(StandardCharsets.UTF_8));
			}
		} catch (Exception e) {
			log.error("refresh thumbStamp error.", e);
		}
		curr.thumbStamp = getThumbStamp(out.toByteArray());
	}

	private static String getThumbStamp(byte[] ctx) {
		return DigestUtils.md5Hex(ctx);
	}

	private final ReentrantReadWriteLock fileLock = new ReentrantReadWriteLock();

	private ConcurrentHashMap<String, WeakReference<byte[]>> filesCache = new ConcurrentHashMap<>();

	public byte[] getFile(FileType type, String fileName) throws IOException {
		fileLock.readLock().lock();
		try {
			final String filesCacheKey = (type.dir + '/' + fileName).intern();

			// read from cache
			WeakReference<byte[]> fileCtx = filesCache.get(filesCacheKey);
			byte[] ctx;
			if (fileCtx != null && (ctx = fileCtx.get()) != null)
				return ctx;

			// read file and update cache
			synchronized (filesCacheKey) {
				ctx = Files.readAllBytes(Paths.get(mainDir, type.dir, fileName));
				filesCache.put(filesCacheKey, new WeakReference<>(ctx));
				return ctx;
			}
		} finally {
			fileLock.readLock().unlock();
		}
	}

	/**
	 * put file operation, then update FilesInfo if update USER files.
	 */
	public void putFile(FileType type, String fileName, byte[] ctx) throws IOException {
		log.info("update file: (" + type + ") " + fileName);
		fileLock.writeLock().lock();
		try {
			if (type == FileType.CORE) {
				Files.write(Paths.get(updateDir, type.dir, fileName), ctx);
			} else if (type == FileType.USER) {
				try {
					// close class loader and write file
					cl.close();
					Files.write(Paths.get(mainDir, type.dir, fileName), ctx);

					// update files info
					curr.userFiles.put(fileName, getThumbStamp(ctx));
					refreshTS();
					old = new FilesInfo(curr);

					// put cache
					String filesCacheKey = (type.dir + '/' + fileName).intern();
					filesCache.put(filesCacheKey, new WeakReference<>(ctx));
				} finally {
					renewCl();
				}
			} else
				throw new RuntimeException("unknown fileType: " + type);
		} finally {
			fileLock.writeLock().unlock();
		}
	}

	/**
	 * remove file operation, then update FilesInfo if update USER files.
	 */
	public void removeFile(FileType type, String fileName) throws IOException {
		log.info("remove file: (" + type + ") " + fileName);
		fileLock.writeLock().lock();
		try {
			if (type == FileType.CORE) { // update core files
				// delete file in update dir
				final File updateDirFile = Paths.get(updateDir, FileType.CORE.dir, fileName).toFile();
				updateDirFile.delete();

				// write delete script
				String updateScriptFile, script;
				Charset charset;
				if (OSUtil.getOS() == OSUtil.OS.Windows) {
					updateScriptFile = "update.bat";
					charset = StandardCharsets.US_ASCII;
					script = "del /f /q \"" + mainDir + "\\" + type.dir + "\\" + fileName + "\"";
				} else {
					updateScriptFile = "update.sh";
					charset = StandardCharsets.UTF_8;
					script = "rm -f \"" + mainDir + "/" + type.dir + "/" + fileName + "\"";
				}
				script += System.lineSeparator();
				final Path usFile = Paths.get(updateScriptFile);
				Files.write(usFile, script.getBytes(charset),
								StandardOpenOption.APPEND, StandardOpenOption.CREATE);
				usFile.toFile().setExecutable(true, false);
			} else if (type == FileType.USER) { // update user files
				try {
					// close class loader and delete file
					cl.close();
					Paths.get(mainDir, type.dir, fileName).toFile().delete();

					// update files info
					curr.userFiles.remove(fileName);
					refreshTS();
					old = new FilesInfo(curr);

					// clear cache
					String filesCacheKey = (type.dir + '/' + fileName).intern();
					filesCache.remove(filesCacheKey);
				} finally {
					// start class loader
					renewCl();
				}
			} else
				throw new RuntimeException("unknown fileType: " + type);
		} finally {
			fileLock.writeLock().unlock();
		}
	}

	@Override
	public void close() throws IOException {
		cl.close();
	}

	/**
	 * get restart cmd.
	 */
	public String[] getRestartCmd() {
		final String pid = String.valueOf(OSUtil.getPid());

		if (OSUtil.getOS() == OSUtil.OS.Windows)
			return new String[]{"startCluster.bat", pid};
		else
			return new String[]{"/bin/sh", "startCluster.sh", pid};
	}
}
