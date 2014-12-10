package mysh.cluster.update;

import mysh.cluster.update.FilesMgr.FileType;

/**
 * @author Mysh
 * @since 2014/12/7 20:47
 */
public interface IUpdateDispatcher {

	/**
	 * get file content. return <code>null</code> if file not exists.
	 */
	byte[] getFile(FileType fileType, String fileName);

	/**
	 * get <code>core</code> and <code>user</code> files info,
	 */
	FilesInfo getFilesInfo();
}
