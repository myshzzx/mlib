package mysh.cluster;

import mysh.collect.Colls;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

/**
 * @author Mysh
 * @since 2014/12/21 16:52
 */
public class Cu4Perm extends IClusterUser<String, String, String, String> {

    @Override
    public SubTasksPack<String> fork(String task, String masterNode, List<String> workerNodes) {
        test();
        return pack(Colls.fillNull(1), null);
    }

    @Override
    public String procSubTask(String subTask, int timeout) throws InterruptedException {
        test();
        return null;
    }

    private void test() {
        Stream.of("a.txt", "abc/a.txt", "../../a.txt").forEach(path -> {
            File file = fileGet(path);
            try {
                Files.write(file.toPath(), "mysh".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        try {
            Thread t = new Thread();
            t.start();
            System.out.println("uncontrolled thread started");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            threadFactory().newThread(() -> {
                System.out.println("user thread.");
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException e) {
                    System.out.println("user thread interrupted");
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String join(String masterNode, List<String> assignedNodeIds, List<String> subResults) {
        return null;
    }
}
