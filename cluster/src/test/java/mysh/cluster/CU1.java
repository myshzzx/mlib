package mysh.cluster;

import java.util.Set;

/**
 * @author Mysh
 * @since 2014/12/13 14:17
 */
public class CU1 extends IClusterMgr<String, String, String, String> {
	@Override
	public SubTasksPack<String> fork(String task, Set<String> workerNodes) {
		final String[] ss = new String[workerNodes.size()];
		int n = 0;
		for (String workerNode : workerNodes) {
			ss[n++] = workerNode;
		}
		System.out.println("=========== fork 2 =============");
		return pack(ss, ss);
	}

	@Override
	public Class<String> getSubResultType() {
		return String.class;
	}

	@Override
	public String procSubTask(String subTask, int timeout) throws InterruptedException {
		System.out.println("----------- sub task  2------------");
		System.out.println(worker.getFilesMgr().getFilesInfo());
		return null;
	}

	@Override
	public String join(String[] subResults, String[] assignedNodeIds) {
		return null;
	}
}
