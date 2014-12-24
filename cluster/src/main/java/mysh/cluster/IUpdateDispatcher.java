package mysh.cluster;


import java.io.IOException;

/**
 * @author Mysh
 * @since 2014/12/7 20:47
 */
public interface IUpdateDispatcher {

	/**
	 * get file by name. about name, see {@link mysh.cluster.FilesInfo#filesTsMap}
	 */
	byte[] getFile(String name) throws IOException;

	/**
	 * get <code>core</code> and <code>user</code> files info,
	 */
	FilesInfo getFilesInfo();
}
