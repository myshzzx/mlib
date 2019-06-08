package mysh.cluster;

import java.util.Arrays;
import java.util.List;

/**
 * @author Mysh
 * @since 2014/12/17 23:18
 */
public class Cu2 extends IClusterUser<String, String, String, String> {

    @Override
    public SubTasksPack<String> fork(String task, String masterNode, List<String> workerNodes) {
        return pack(Arrays.asList(task.split(" ")), null);
    }

    @Override
    public String procSubTask(String subTask, int timeout) throws InterruptedException {
        try {
            System.out.println(Thread.currentThread().getContextClassLoader());
            final Object cu2x = Class.forName("mysh.cluster.Cu2X").getConstructor().newInstance();
            System.out.println(cu2x.getClass().getClassLoader());
            return subTask + cu2x;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String join(String masterNode, List<String> assignedNodeIds, List<String> subResults) {
        return String.join(",", subResults);
    }
}
