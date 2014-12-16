package mysh.cluster;

import mysh.cluster.update.FilesMgr;
import mysh.cluster.update.FilesMgr.FileType;
import mysh.cluster.update.FilesMgr.UpdateType;
import mysh.util.ExpUtil;

import java.util.List;

/**
 * @author Mysh
 * @since 2014/12/12 15:57
 */
final class MgrFileUpdate extends IClusterMgr<String, String, String, String> {
	private static final long serialVersionUID = -674837947039151106L;

	private FileType fileType;
	private UpdateType updateType;
	private String fileName;
	private byte[] ctx;

	public MgrFileUpdate(FileType fileType, UpdateType updateType, String fileName, byte[] ctx) {
		this.fileType = fileType;
		this.updateType = updateType;
		this.fileName = fileName;
		this.ctx = ctx;
	}

	@Override
	public SubTasksPack<String> fork(String task, String masterNode, List<String> workerNodes) {
		updateFile(master.getFilesMgr());

		return pack(new String[workerNodes.size()], workerNodes.toArray(new String[workerNodes.size()]));
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
	public String join(String masterNode, String[] assignedNodeIds, String[] subResults) {
		return null;
	}

	@Override
	public String toString() {
		return "MgrFileUpdate{" +
						"updateType=" + updateType +
						", fileType=" + fileType +
						", fileName='" + fileName + '\'' +
						'}';
	}
}
