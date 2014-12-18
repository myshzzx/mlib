package mysh.cluster;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Mysh
 * @since 2014/12/17 23:22
 */
public class Cu2X {
	@Override
	public String toString() {
		try {
			return new String(Files.readAllBytes(Paths.get("l:", "a.txt")));
		} catch (IOException e) {
			return null;
		}
	}
}
