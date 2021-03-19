package mysh.ui;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Base64;

@Disabled
public class ImgRecognizerTest {

	@Test
	public void testGetCaptcha() throws Exception {
		ImgRecognizer cg = new ImgRecognizer().build("http://www.renrendai.com/image_https.jsp",
						"testGetCaptcha", null, null);
		System.out.println(cg.getText());
	}

	@Test
	public void testGetCaptcha2() throws Exception {
		String data = "data:image/gif;base64,R0lGODlhJQAOAKUAAAQGBIyOjMTGxDw+PCQiJOTm5LSytDQyNNTS1GxubPTy9BwaHJyanERGRCwqLLy+vOzu7Dw6PNza3AwKDJSSlMzOzERCRCQmJOzq7LS2tDQ2NNTW1Pz6/BweHJyenExKTCwuLMTCxP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACH5BAEAACIALAAAAAAlAA4AAAZlQJFwSCwaj8ikcslsOp/QqHRKjT5AoEMI8gE1IFXjAiGqdBIUUSARLl5CooeDUBBhCG1iCNABhCZDgHlCEQwiDBGCIop5ExwiHAAXGCIFeIMiGhkiBhpoamyYGxoOGhIYFg5fYUEAOw==";
		byte[] buf = Base64.getDecoder().decode(data.substring(data.indexOf(',') + 1));
		ImgRecognizer cg = new ImgRecognizer().build(buf, "testGetCaptcha2");
		System.out.println(cg.getText());
	}
}
