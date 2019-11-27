package jsocks.socks;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

import javax.swing.*;
import javax.swing.plaf.metal.MetalIconFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

/**
 * run a socks5
 * <p>
 * http://code.google.com/p/ssh-persistent-tunnel/
 */
public class DpfSocksProxy {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JSch jsch = new JSch();
		try {
			String host = null;
			host = JOptionPane.showInputDialog("Enter username@hostname", System.getProperty("user.name") + "@localhost");
			String user = host.substring(0, host.indexOf('@'));
			host = host.substring(host.indexOf('@') + 1);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			Session session = jsch.getSession(user, host, 22);
			SysTray sysTray = new SysTray(session);
			session.setConfig(config);
			UserInfo ui = new MyUserInfo();
			session.setUserInfo(ui);
			session.connect();
			if (session.isConnected()) {
				sysTray.notifyTrayyMessage("tunnel established");
			}
			String port = JOptionPane.showInputDialog("Enter proxy server port", "9090");
			DynamicForwarder dynamicForwarder = new DynamicForwarder(Integer.parseInt(port), session);
			dynamicForwarder.run();
			sysTray.notifyTrayyMessage("socks proxy listening on localhost:" + port);
		} catch (JSchException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static class MyUserInfo implements UserInfo, UIKeyboardInteractive {
		public String getPassword() {
			return passwd;
		}
		
		public boolean promptYesNo(String str) {
			Object[] options = {"yes", "no"};
			int foo = JOptionPane.showOptionDialog(null, str, "Warning", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
			return foo == 0;
		}
		
		String passwd;
		JTextField passwordField = (JTextField) new JPasswordField(20);
		
		public String getPassphrase() {
			return null;
		}
		
		public boolean promptPassphrase(String message) {
			return true;
		}
		
		public boolean promptPassword(String message) {
			Object[] ob = {passwordField};
			int result = JOptionPane.showConfirmDialog(null, ob, message, JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				passwd = passwordField.getText();
				return true;
			} else {
				return false;
			}
		}
		
		public void showMessage(String message) {
			JOptionPane.showMessageDialog(null, message);
		}
		
		final GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
		private Container panel;
		
		public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo) {
			panel = new JPanel();
			panel.setLayout(new GridBagLayout());
			
			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.gridx = 0;
			panel.add(new JLabel(instruction), gbc);
			gbc.gridy++;
			
			gbc.gridwidth = GridBagConstraints.RELATIVE;
			
			JTextField[] texts = new JTextField[prompt.length];
			for (int i = 0; i < prompt.length; i++) {
				gbc.fill = GridBagConstraints.NONE;
				gbc.gridx = 0;
				gbc.weightx = 1;
				panel.add(new JLabel(prompt[i]), gbc);
				
				gbc.gridx = 1;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.weighty = 1;
				if (echo[i]) {
					texts[i] = new JTextField(20);
				} else {
					texts[i] = new JPasswordField(20);
				}
				panel.add(texts[i], gbc);
				gbc.gridy++;
			}
			
			if (JOptionPane.showConfirmDialog(null, panel, destination + ": " + name, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
				String[] response = new String[prompt.length];
				for (int i = 0; i < prompt.length; i++) {
					response[i] = texts[i].getText();
				}
				return response;
			} else {
				return null; // cancel
			}
		}
	}
	
	public static class SysTray {
		
		private Session session;
		private TrayIcon icon;
		
		public SysTray(Session session) {
			this.session = session;
			try {
				installSystemTray();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public void installSystemTray() throws Exception {
			PopupMenu menu = new PopupMenu();
			MenuItem exit = new MenuItem("Exit");
			exit.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					session.disconnect();
					System.exit(0);
				}
			});
			menu.add(exit);
			
			icon = new TrayIcon(getImage(), "Java application as a tray icon", menu);
			SystemTray.getSystemTray().add(icon);
		}
		
		private Image getImage() throws HeadlessException {
			Icon defaultIcon = MetalIconFactory.getTreeHardDriveIcon();
			Image img = new BufferedImage(defaultIcon.getIconWidth(), defaultIcon.getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR);
			defaultIcon.paintIcon(new Panel(), img.getGraphics(), 0, 0);
			return img;
		}
		
		public void notifyTrayyMessage(String message) {
			icon.displayMessage("Proxy Info", message, TrayIcon.MessageType.INFO);
		}
		
	}
}
