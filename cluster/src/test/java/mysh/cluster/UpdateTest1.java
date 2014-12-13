package mysh.cluster;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Mysh
 * @since 2014/12/13 14:06
 */
public class UpdateTest1 {
	@Test
	public void t1() throws Exception {
		ClusterClient c = new ClusterClient(8030);

		byte[] ctx = Files.readAllBytes(Paths.get("l:", "a.jar"));
//		c.runTask(new FileUpdate(FilesMgr.UpdateType.UPDATE, FilesMgr.FileType.USER, "a.jar", ctx),
//						"", 0, 0);

		c.runTask(new CU1(), null, 0, 0);
	}

}
