
package mysh.util;

import java.awt.Component;
import java.awt.Font;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.chainsaw.Main;

public class UIUtil {

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
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * 取文件扩展名. 扩展名以 . 开头.
	 * 
	 * @author Allen
	 * 
	 */
	public static abstract class FileExtentionGetter {

		/**
		 * 取以某扩展名结尾的文件.<br/>
		 * 若不是此扩展名, 则返回一个加上此扩展名的文件.
		 * 
		 * @param file
		 * @param fileExtensionGetter
		 *               为 null, 则直接返回 file.
		 * @return
		 */
		public static File ensureFileWithExtention(File file, FileExtentionGetter fileExtensionGetter) {

			if (fileExtensionGetter == null || fileExtensionGetter.getFileExtention() == null)
				return file;

			String ext = fileExtensionGetter.getFileExtention().toLowerCase();
			if (file.getPath().toLowerCase().endsWith(ext)) {
				return file;
			} else {
				return new File(file.getPath().concat(ext));
			}
		}

		/**
		 * 取文件扩展名. 扩展名以 . 开头.
		 * 
		 * @return
		 */
		public abstract String getFileExtention();
	}

	/**
	 * 重置UI的显示字体.
	 * 
	 * @param font
	 *               给定字体. 为 null 则使用 13号雅黑.
	 */
	public static void resetFont(Font font) {

		if (font == null)
			font = new Font("Microsoft YaHei", Font.PLAIN, 13);

		UIManager.put("ArrowButton.font", font);
		UIManager.put("Button.font", font);
		UIManager.put("CheckBox.font", font);
		UIManager.put("CheckBoxMenuItem.font", font);
		UIManager.put("ColorChooser.font", font);
		UIManager.put("ComboBox.font", font);
		UIManager.put("DesktopIcon.font", font);
		UIManager.put("DesktopPane.font", font);
		UIManager.put("EditorPane.font", font);
		UIManager.put("FileChooser.font", font);
		UIManager.put("FormattedTextField.font", font);
		UIManager.put("InternalFrame.font", font);
		UIManager.put("InternalFrameTitlePane.font", font);
		UIManager.put("Label.font", font);
		UIManager.put("List.font", font);
		UIManager.put("Menu.font", font);
		UIManager.put("MenuBar.font", font);
		UIManager.put("MenuItem.font", font);
		UIManager.put("OptionPane.font", font);
		UIManager.put("Panel.font", font);
		UIManager.put("PasswordField.font", font);
		UIManager.put("PopupMenu.font", font);
		UIManager.put("PopupMenuSeparator.font", font);
		UIManager.put("ProgressBar.font", font);
		UIManager.put("RadioButton.font", font);
		UIManager.put("RadioButtonMenuItem.font", font);
		UIManager.put("RootPane.font", font);
		UIManager.put("ScrollBar.font", font);
		UIManager.put("ScrollBarThumb.font", font);
		UIManager.put("ScrollBarTrack.font", font);
		UIManager.put("ScrollPane.font", font);
		UIManager.put("Separator.font", font);
		UIManager.put("Slider.font", font);
		UIManager.put("SliderThumb.font", font);
		UIManager.put("SliderTrack.font", font);
		UIManager.put("Spinner.font", font);
		UIManager.put("SplitPane.font", font);
		UIManager.put("TabbedPane.font", font);
		UIManager.put("Table.font", font);
		UIManager.put("TableHeader.font", font);
		UIManager.put("TextArea.font", font);
		UIManager.put("TextField.font", font);
		UIManager.put("TextPane.font", font);
		UIManager.put("TitledBorder.font", font);
		UIManager.put("ToggleButton.font", font);
		UIManager.put("ToolBar.font", font);
		UIManager.put("ToolTip.font", font);
		UIManager.put("Tree.font", font);
		UIManager.put("Viewport.font", font);
	}

	/**
	 * 从文件选择器选择保存目标文件, 已存在的文件询问是否要覆盖.<br/>
	 * 确保返回给定扩展名类型的文件.<br/>
	 * 取消操作返回 null.
	 * 
	 * @param fileChooser
	 * @param parent
	 *               父容器. 可以为 null.
	 * @param fileExtensionGetter
	 *               扩展名获取器. 为 null 表示不限扩展名.
	 * @return
	 */
	public static File getSaveFileWithOverwriteChecking(JFileChooser fileChooser, Component parent,
			FileExtentionGetter fileExtensionGetter) {

		File file = null;
		boolean fileChooserSelectionMode = fileChooser.isMultiSelectionEnabled();
		fileChooser.setMultiSelectionEnabled(false);

		while (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION
				&& (file = FileExtentionGetter.ensureFileWithExtention(fileChooser.getSelectedFile(),
						fileExtensionGetter)).exists()) {
			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(parent, file.getName()
					+ " 已存在\n是否覆盖文件?", "覆盖确认", JOptionPane.YES_NO_OPTION))
				break;
			else
				file = null;
		}

		fileChooser.setMultiSelectionEnabled(fileChooserSelectionMode);
		return file;
	}

}
