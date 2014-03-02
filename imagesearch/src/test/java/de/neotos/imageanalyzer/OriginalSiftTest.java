package de.neotos.imageanalyzer;

import org.junit.Ignore;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * @author Mysh
 * @since 13-12-23 下午1:39
 */
@Ignore
public class OriginalSiftTest {

	public static void main(String[] args) {
		ImageAnalyzerImplementation analyzer = new ImageAnalyzerImplementation();
		analyzer.useCache(false);
		analyzer.bindImages(new File("l:/pics"));

		Scanner s = new Scanner(System.in);
		String file;
		while ((file = s.next()) != null) {
			List<ImageAnalyzerResult> res = analyzer.findImage(new File(file));
			System.out.println("Results: ");
			System.out.println(Arrays.toString(res.toArray()));
		}
	}
}
