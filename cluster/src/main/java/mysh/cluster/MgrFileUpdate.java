package mysh.cluster;

import mysh.cluster.FilesMgr.FileType;
import mysh.cluster.FilesMgr.UpdateType;
import mysh.util.ExpUtil;

import java.util.List;

/**
 * When update USER or SU files, all files will be updated in all nodes, and applied immediately.<br/>
 * But if update CORE files, changes will be applied only after a restart, so only master node will
 * be updated, because if there are some problem in updates, the node may be not able to start.
 * If all nodes update CORE files immediately, then entire cluster may be down and not able to
 * recover itself.
 *
 * @author Mysh
 * @since 2014/12/12 15:57
 */
final class MgrFileUpdate extends IClusterMgr<String, String, String, String> {
	private static final long serialVersionUID = -674837947039151106L;
	private static final String[] emptyArray = new String[0];

	private FileType fileType;
	private String ns;
	private UpdateType updateType;
	private String fileName;
	private byte[] ctx;

	public MgrFileUpdate(FileType fileType, String ns, UpdateType updateType, String fileName, byte[] ctx) {
		this.fileType = fileType;
		this.ns = ns;
		this.updateType = updateType;
		this.fileName = fileName;
		this.ctx = ctx;
	}

	@Override
	public SubTasksPack<String> fork(String task, String masterNode, List<String> workerNodes) {
		updateFile(master.getFilesMgr());

		if (fileType == FileType.CORE) {
			return pack(emptyArray, emptyArray);
		} else if (fileType == FileType.USER || fileType == FileType.SU) {
			workerNodes.remove(masterNode);
			return pack(new String[workerNodes.size()], workerNodes.toArray(new String[workerNodes.size()]));
		} else
			throw new RuntimeException("unknown fileType: " + fileType);
	}

	private void updateFile(FilesMgr filesMgr) {
		try {
			if (updateType == UpdateType.UPDATE)
				filesMgr.putFile(fileType, ns, fileName, ctx);
			else if (updateType == UpdateType.DELETE)
				filesMgr.removeFile(fileType, ns, fileName);
		} catch (Throwable e) {
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
						"ns='" + ns + '\'' +
						", fileType=" + fileType +
						", updateType=" + updateType +
						", fileName='" + fileName + '\'' +
						'}';
	}
}
