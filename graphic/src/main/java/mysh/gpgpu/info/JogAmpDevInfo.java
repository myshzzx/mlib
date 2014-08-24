/*
 * Created on Tuesday, September 07 2010 21:33
 */

package mysh.gpgpu.info;

import com.jogamp.common.JogampRuntimeException;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.JoclVersion;
import com.jogamp.opencl.util.ExceptionReporter;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Displays OpenCL information in a table.
 * @author Michael Bien
 */
public class JogAmpDevInfo {

    public static void main(String[] args) {

        Logger logger = Logger.getLogger(JogAmpDevInfo.class.getName());

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            logger.log(Level.INFO, null, ex);
        }

        final JoclVersion joclVersion = JoclVersion.getInstance();
        logger.info("\n" + joclVersion.getAllVersions(null).toString());

        try{
            CLPlatform.initialize();
        }catch(JogampRuntimeException ex) {
            logger.log(Level.SEVERE, null, ex);
            ExceptionReporter.appear("I tried hard but I really can't initialize JOCL. Is OpenCL properly set up?", ex);
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
