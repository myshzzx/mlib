package mysh.cluster.update;

import mysh.cluster.update.FilesMgr.FileType;

import java.io.IOException;

/**
 * @author Mysh
 * @since 2014/12/7 20:47
 */
public interface IUpdateDispatcher {

	FilesMgr getFilesMgr();

	/**
	 * get file content. return <code>null</code> if file not exists.
	 */
	byte[] getFile(FileType type, String fileName) throws IOException;

	/**
	 * get <code>core</code> and <code>user</code> files info,
	 */
	FilesInfo getFilesInfo();
}
