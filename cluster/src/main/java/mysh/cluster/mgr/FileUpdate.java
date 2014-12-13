package mysh.cluster.mgr;

import mysh.cluster.IClusterMgr;
import mysh.cluster.update.FilesMgr;
import mysh.cluster.update.FilesMgr.UpdateType;
import mysh.util.ExpUtil;

import java.util.Set;

/**
 * @author Mysh
 * @since 2014/12/12 15:57
 */
public final class FileUpdate extends IClusterMgr<String, String, String, String> {
	private static final long serialVersionUID = -674837947039151106L;

	private UpdateType updateType;
	private FilesMgr.FileType fileType;
	private String fileName;
	private byte[] ctx;

	public FileUpdate(UpdateType updateType, FilesMgr.FileType fileType, String fileName, byte[] ctx) {
		this.updateType = updateType;
		this.fileType = fileType;
		this.fileName = fileName;
		this.ctx = ctx;
	}

	@Override
	public SubTasksPack<String> fork(String task, Set<String> workerNodes) {
		updateFile(master.getFilesMgr());

		String[] ids = new String[workerNodes.size()];
		int n = 0;
		for (String id : workerNodes) {
			ids[n++] = id;
		}
		return pack(new String[workerNodes.size()], ids);
	}

	private void updateFile(FilesMgr filesMgr) {
		try {
			if (updateType == UpdateType.UPDATE)
				filesMgr.putFile(fileType, fileName, ctx);
			else if (updateType == UpdateType.DELETE)
				filesMgr.removeFile(fileType, fileName);
		} catch (Exception e) {
			throw ExpUtil.unchecked(e);
		}
	}

	@Override
	public Class<String> getSubResultType() {
		return String.class;
	}

	@Override
	public String procSubTask(String subTask, int timeout) throws InterruptedException {
		updateFile(worker.getFilesMgr());
		return "";
	}

	@Override
	public String join(String[] subResults, String[] assignedNodeIds) {
		return "";
	}
}
