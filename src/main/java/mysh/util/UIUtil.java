
package mysh.util;

import java.awt.Font;

import javax.swing.UIManager;

public class UIUtil {

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
}
