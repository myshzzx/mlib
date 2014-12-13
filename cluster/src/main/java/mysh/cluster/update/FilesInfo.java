package mysh.cluster.update;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * cluster files thumbStamp.
 *
 * @author Mysh
 * @since 2014/12/7 21:09
 */
public final class FilesInfo implements Serializable {
	private static final long serialVersionUID = 1118515353624642892L;

	public String thumbStamp = "";
	/**
	 * core files [name,thumbStamp] map. it's <b>immutable</b>.
	 */
	public Map<String, String> coreFiles;
	/**
	 * user files [name,thumbStamp] map. it's <b>immutable</b>.
	 */
	public Map<String, String> userFiles;

	@Override
	public synchronized boolean equals(Object obj) {
		if (obj instanceof FilesInfo) {
			FilesInfo f = (FilesInfo) obj;
			return this.thumbStamp.equals(f.thumbStamp);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.thumbStamp.hashCode();
	}

	FilesInfo() {
	}

	@SuppressWarnings("unchecked")
	FilesInfo(FilesInfo fi) {
		this.thumbStamp = fi.thumbStamp;
		this.coreFiles = Collections.unmodifiableMap(new HashMap<>(fi.coreFiles));
		this.userFiles = Collections.unmodifiableMap(new HashMap<>(fi.userFiles));
	}

	@Override
	public String toString() {
		return "FilesInfo{" +
						"thumbStamp='" + thumbStamp + '\'' +
						", coreFiles=" + coreFiles +
						", userFiles=" + userFiles +
						'}';
	}
}
