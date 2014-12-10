package mysh.cluster.update;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @author Mysh
 * @since 2014/12/7 21:09
 */
public final class FilesInfo implements Serializable {
	private static final long serialVersionUID = 1118515353624642892L;

	public String thumbStamp;
	/**
	 * core files [name,thumbStamp] map.
	 */
	public HashMap<String, String> coreFiles;
	/**
	 * user files [name,thumbStamp] map.
	 */
	public HashMap<String, String> userFiles;

	@Override
	public synchronized boolean equals(Object obj) {
		if (obj instanceof FilesInfo) {
			FilesInfo f = (FilesInfo) obj;
			return this.thumbStamp.equals(f.thumbStamp);
		}
		return false;
	}

	FilesInfo() {
	}

	@SuppressWarnings("unchecked")
	FilesInfo(FilesInfo fi) {
		this.thumbStamp = fi.thumbStamp;
		this.coreFiles = (HashMap<String, String>) fi.coreFiles.clone();
		this.userFiles = (HashMap<String, String>) fi.userFiles.clone();
	}
}
