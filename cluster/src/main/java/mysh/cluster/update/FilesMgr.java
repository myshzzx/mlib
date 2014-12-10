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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

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

		private String dirName;

		FileType(String dirName) {
			this.dirName = dirName;
		}

		public String getDir() {
			return this.dirName;
		}
	}

	public static enum UpdateType {
		UPDATE, DELETE
	}

	public final Serializer.ClassLoaderFetcher clFetcher = () -> FilesMgr.this.cl;
	private volatile URLClassLoader cl;

	private volatile FilesInfo old;
	private final FilesInfo curr = new FilesInfo();

	public FilesMgr() throws IOException {
		curr.coreFiles = new HashMap<>();
		curr.userFiles = new HashMap<>();

		for (FileType type : Arrays.asList(FileType.CORE, FileType.USER))
			Files.walk(Paths.get(mainDir, type.getDir()), 1).forEach(p -> {
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
		Files.walk(Paths.get(mainDir, FileType.USER.getDir()), 1).forEach(p -> {
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

	public synchronized void putFile(FileType type, String fileName, byte[] ctx) throws IOException {
		if (type == FileType.CORE)
			Files.write(Paths.get(updateDir, type.getDir(), fileName), ctx);
		else {
			try {
				cl.close();
				Files.write(Paths.get(mainDir, type.getDir(), fileName), ctx);
			} finally {
				renewCl();
			}
		}

		(type == FileType.CORE ? curr.coreFiles : curr.userFiles).put(fileName, getThumbStamp(ctx));
		refreshTS();
		old = new FilesInfo(curr);
	}

	public synchronized void removeFile(FileType type, String fileName) throws IOException {
		if (type == FileType.CORE) {
			String updateScriptFile, script;
			if (OSUtil.getOS() == OSUtil.OS.Windows) {
				updateScriptFile = "update.bat";
				script = "del /f /q \"" + mainDir + "\\" + type.getDir() + "\\" + fileName + "\"";
			} else {
				updateScriptFile = "update.sh";
				script = "rm -f \"" + mainDir + "/" + type.getDir() + "/" + fileName + "\"";
			}
			script += System.lineSeparator();
			Files.write(Paths.get(updateScriptFile), script.getBytes(StandardCharsets.US_ASCII),
							StandardOpenOption.APPEND);
		} else {
			try {
				cl.close();
				Paths.get(mainDir, type.getDir(), fileName).toFile().delete();
			} finally {
				renewCl();
			}
		}

		(type == FileType.CORE ? curr.coreFiles : curr.userFiles).remove(fileName);
		refreshTS();
		old = new FilesInfo(curr);
	}

	@Override
	public void close() throws IOException {
		cl.close();
	}

	public static void main(String[] args) throws IOException {
		String updateScriptFile = "update.bat";
		String fileName = "abc.jar";
		String script = "del /f /q \"" + mainDir + "\\" + FileType.CORE.getDir() + "\\" + fileName + "\"";
		script += System.lineSeparator();
		Files.write(Paths.get(updateScriptFile), script.getBytes(StandardCharsets.US_ASCII),
						StandardOpenOption.APPEND, StandardOpenOption.CREATE);
	}
}
