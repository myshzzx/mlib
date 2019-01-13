package mysh.dev.ui;

import javafx.event.ActionEvent;
import javafx.scene.control.CheckBox;

/**
 * Settings
 *
 * @author mysh
 * @since 2019/1/14
 */
public class Settings {

	public CheckBox alwaysOnTopChk;

	public void alwaysOnTop(ActionEvent e) {
		ToolsIntegratedUI.frame.setAlwaysOnTop(alwaysOnTopChk.isSelected());
	}
}
