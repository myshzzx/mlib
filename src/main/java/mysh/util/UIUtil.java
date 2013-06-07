
package mysh.util;

import org.apache.log4j.chainsaw.Main;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	 * 重置当前 UI L&F 的默认显示字体.
	 * 
	 * @param font
	 *               给定字体. 为 null 则使用 13号雅黑.
	 */
	public static void resetFont(Font font) {

		if (font == null)
			font = new Font("Microsoft YaHei", Font.PLAIN, 13);

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
