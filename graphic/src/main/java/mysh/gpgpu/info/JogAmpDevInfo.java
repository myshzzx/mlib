/*
 * Created on Tuesday, September 07 2010 21:33
 */

package mysh.gpgpu.info;

import com.jogamp.common.JogampRuntimeException;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.JoclVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Displays OpenCL information in a table.
 *
 * @author Michael Bien
 */
public class JogAmpDevInfo {
	private static final Logger logger = LoggerFactory.getLogger(JogAmpDevInfo.class);

	public static void main(String[] args) {


		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			logger.info(null, ex);
		}

		final JoclVersion joclVersion = JoclVersion.getInstance();
		logger.info("\n" + joclVersion.getAllVersions(null).toString());

		try {
			CLPlatform.initialize();
		} catch (JogampRuntimeException ex) {
			logger.info(null, ex);
			JOptionPane.showMessageDialog(
							null, "I tried hard but I really can't initialize JOCL. Is OpenCL properly set up?\n" +
											ex);
			return;
		}

		JFrame frame = new JFrame("OpenCL Info");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Container contentPane = frame.getContentPane();

		JEditorPane area = new JEditorPane();
		area.setContentType("text/html");
		area.setEditable(false);

		contentPane.add(new JScrollPane(area));

		area.setText(joclVersion.getOpenCLHtmlInfo(null).toString());

		frame.setSize(800, 600);
		frame.setVisible(true);

	}

}
