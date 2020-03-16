
package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class UIs {
	private static final Logger log = LoggerFactory.getLogger(UIs.class);
	/**
	 * 默认字体.
	 */
	public static final Font DefaultFont = new Font("Microsoft YaHei", Font.PLAIN, 13);
	
	/**
	 * 使用 Nimbus 皮肤.
	 */
	public static void useNimbusLookAndFeel() {
		
		try {
			for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException ex) {
			log.error("use Nimbus L&F failed.", ex);
		}
	}
	
	/**
	 * set tray icon popup menu.
	 */
	public static void trayIconPopupMenu(TrayIcon icon, JPopupMenu menu) {
		icon.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()
						|| Oss.isMac() && e.getButton() == MouseEvent.BUTTON1) {
					menu.setLocation(e.getX(), e.getY());
					menu.setInvoker(menu);
					menu.setVisible(true);
				}
			}
		});
		menu.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				menu.setVisible(false);
			}
		});
	}
	
	/**
	 * 取文件扩展名. 扩展名以 . 开头.
	 *
	 * @author Allen
	 */
	public interface FileExtensionGetter {
		
		/**
		 * 取以某扩展名结尾的文件.<br/>
		 * 若不是此扩展名, 则返回一个加上此扩展名的文件.
		 *
		 * @param fExtGetter 为 null, 则直接返回 file.
		 */
		static File ensureFileWithExtension(File file, FileExtensionGetter fExtGetter) {
			
			if (fExtGetter == null || fExtGetter.getFileExtension() == null)
				return file;
			
			String ext = fExtGetter.getFileExtension().toLowerCase();
			if (file.getPath().toLowerCase().endsWith(ext)) {
				return file;
			} else {
				return new File(file.getPath().concat(ext));
			}
		}
		
		/**
		 * 取文件扩展名. 扩展名以 . 开头.
		 */
		String getFileExtension();
	}
	
	/**
	 * 将字符串复制到系统剪贴板
	 *
	 * @param str 要复制的字符串
	 */
	public static void copyToSystemClipboard(String str) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable tText = new StringSelection(str);
		clipboard.setContents(tText, null);
	}
	
	/**
	 * get system clipboard content.
	 *
	 * @throws IllegalStateException      - if this clipboard is currently unavailable
	 * @throws UnsupportedFlavorException - if the requested DataFlavor is not available
	 * @throws IOException                - if the data in the requested DataFlavor can not be retrieved
	 */
	public static String getSysClipboard() {
		try {
			return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 重置当前 UI L&F 的默认显示字体.<br/>
	 * 此方法必须放在 L&F 设置之后, UI 组件创建之前, 否则无效.
	 *
	 * @param font 给定字体. 为 null 则使用 13号雅黑.
	 */
	public static void resetFont(Font font) {
		
		if (font == null)
			font = DefaultFont;
		
		for (Map.Entry<Object, Object> e : UIManager.getLookAndFeelDefaults().entrySet()) {
			if (((e.getValue() instanceof FontUIResource))) {
				e.setValue(font);
			}
		}
		
		UIManager.getLookAndFeelDefaults().put("ArrowButton.font", font);
		UIManager.getLookAndFeelDefaults().put("Button.font", font);
		UIManager.getLookAndFeelDefaults().put("CheckBox.font", font);
		UIManager.getLookAndFeelDefaults().put("CheckBoxMenuItem.font", font);
		UIManager.getLookAndFeelDefaults().put("ColorChooser.font", font);
		UIManager.getLookAndFeelDefaults().put("ComboBox.font", font);
		UIManager.getLookAndFeelDefaults().put("DesktopIcon.font", font);
		UIManager.getLookAndFeelDefaults().put("DesktopPane.font", font);
		UIManager.getLookAndFeelDefaults().put("EditorPane.font", font);
		UIManager.getLookAndFeelDefaults().put("FileChooser.font", font);
		UIManager.getLookAndFeelDefaults().put("FormattedTextField.font", font);
		UIManager.getLookAndFeelDefaults().put("InternalFrame.font", font);
		UIManager.getLookAndFeelDefaults().put("InternalFrameTitlePane.font", font);
		UIManager.getLookAndFeelDefaults().put("Label.font", font);
		UIManager.getLookAndFeelDefaults().put("List.font", font);
		UIManager.getLookAndFeelDefaults().put("Menu.font", font);
		UIManager.getLookAndFeelDefaults().put("MenuBar.font", font);
		UIManager.getLookAndFeelDefaults().put("MenuItem.font", font);
		UIManager.getLookAndFeelDefaults().put("OptionPane.font", font);
		UIManager.getLookAndFeelDefaults().put("Panel.font", font);
		UIManager.getLookAndFeelDefaults().put("PasswordField.font", font);
		UIManager.getLookAndFeelDefaults().put("PopupMenu.font", font);
		UIManager.getLookAndFeelDefaults().put("PopupMenuSeparator.font", font);
		UIManager.getLookAndFeelDefaults().put("ProgressBar.font", font);
		UIManager.getLookAndFeelDefaults().put("RadioButton.font", font);
		UIManager.getLookAndFeelDefaults().put("RadioButtonMenuItem.font", font);
		UIManager.getLookAndFeelDefaults().put("RootPane.font", font);
		UIManager.getLookAndFeelDefaults().put("ScrollBar.font", font);
		UIManager.getLookAndFeelDefaults().put("ScrollBarThumb.font", font);
		UIManager.getLookAndFeelDefaults().put("ScrollBarTrack.font", font);
		UIManager.getLookAndFeelDefaults().put("ScrollPane.font", font);
		UIManager.getLookAndFeelDefaults().put("Separator.font", font);
		UIManager.getLookAndFeelDefaults().put("Slider.font", font);
		UIManager.getLookAndFeelDefaults().put("SliderThumb.font", font);
		UIManager.getLookAndFeelDefaults().put("SliderTrack.font", font);
		UIManager.getLookAndFeelDefaults().put("Spinner.font", font);
		UIManager.getLookAndFeelDefaults().put("SplitPane.font", font);
		UIManager.getLookAndFeelDefaults().put("TabbedPane.font", font);
		UIManager.getLookAndFeelDefaults().put("Table.font", font);
		UIManager.getLookAndFeelDefaults().put("TableHeader.font", font);
		UIManager.getLookAndFeelDefaults().put("TextArea.font", font);
		UIManager.getLookAndFeelDefaults().put("TextField.font", font);
		UIManager.getLookAndFeelDefaults().put("TextPane.font", font);
		UIManager.getLookAndFeelDefaults().put("TitledBorder.font", font);
		UIManager.getLookAndFeelDefaults().put("ToggleButton.font", font);
		UIManager.getLookAndFeelDefaults().put("ToolBar.font", font);
		UIManager.getLookAndFeelDefaults().put("ToolTip.font", font);
		UIManager.getLookAndFeelDefaults().put("Tree.font", font);
		UIManager.getLookAndFeelDefaults().put("Viewport.font", font);
		
	}
	
	/**
	 * 从文件选择器选择保存目标文件, 已存在的文件询问是否要覆盖.<br/>
	 * 确保返回给定扩展名类型的文件.<br/>
	 * 取消操作返回 null.
	 *
	 * @param parent 父容器. 可以为 null.
	 * @param fExt   扩展名获取器. 为 null 表示不限扩展名.
	 */
	public static File getSaveFileWithOverwriteChecking(
			JFileChooser fileChooser,
			Component parent,
			FileExtensionGetter fExt) {
		
		File file = null;
		boolean fcSelectionMode = fileChooser.isMultiSelectionEnabled();
		fileChooser.setMultiSelectionEnabled(false);
		
		while (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION
				&& (file =
				FileExtensionGetter.ensureFileWithExtension(fileChooser.getSelectedFile(), fExt)).exists()) {
			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(parent, file.getName()
					+ " 已存在\n是否覆盖文件?", "覆盖确认", JOptionPane.YES_NO_OPTION))
				break;
			else
				file = null;
		}
		
		fileChooser.setMultiSelectionEnabled(fcSelectionMode);
		return file;
	}
	
	/**
	 * open url(non-blank) using default external browser.
	 */
	public static void openOuterBrowser(String openUrl) {
		if (Strings.isBlank(openUrl)) return;
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().browse(URI.create(openUrl));
			} catch (IOException e) {
				String errMsg = "open URL failed:" + openUrl;
				JOptionPane.showMessageDialog(null, errMsg + "\n" + e.getMessage());
				log.error(errMsg, e);
			}
		} else {
			log.error("Desktop not supported, url not opened: " + openUrl);
		}
	}
	
	public static JDialog infoMsg(Component parent, int width, int height, String msg) {
		JDialog j = new JDialog();
		j.setAlwaysOnTop(true);
		j.setUndecorated(true);
		j.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		Container cp = j.getContentPane();
		cp.setLayout(new BorderLayout());
		JLabel label = new JLabel(msg);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		cp.add(label, BorderLayout.CENTER);
		
		j.setBounds(0, 0, width, height);
		j.setLocationRelativeTo(parent);
		j.setVisible(true);
		return j;
	}
}
