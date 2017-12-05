package mysh.cluster;

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
     * files under main folder. it's <b>immutable</b>.<br/>
     * [name, thumbStamp] map. name looks like "user/ns1/lib.jar".
     */
    public Map<String, String> filesTsMap;

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
		this.filesTsMap = Collections.unmodifiableMap(new HashMap<>(fi.filesTsMap));
	}

    @Override
    public String toString() {
        return "FilesInfo{" +
                "thumbStamp='" + thumbStamp + '\'' +
                ", filesTsMap=" + filesTsMap +
                '}';
    }
}
