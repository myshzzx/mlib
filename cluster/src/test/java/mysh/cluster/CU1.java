package mysh.cluster;

import java.util.List;

/**
 * @author Mysh
 * @since 2014/12/13 14:17
 */
public class CU1 extends IClusterMgr<String, String, String, String> {
	@Override
	public SubTasksPack<String> fork(String task, String masterNode, List<String> workerNodes) {
		System.out.println("=========== fork 2 =============");
		return pack(workerNodes, workerNodes);
	}

	@Override
	public String procSubTask(String subTask, int timeout) throws InterruptedException {
		System.out.println("----------- sub task  2------------");
		System.out.println(worker.getFilesMgr().getFilesInfo());
		return null;
	}

	@Override
	public String join(String masterNode, List<String> assignedNodeIds, List<String> subResults) {
		return null;
	}
}
