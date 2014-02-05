
package mysh.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;

/**
 * 热键工具类.
 * 
 * @author Allen
 * 
 */
public class HotKeyUtil {

	/**
	 * 键-动作映射.
	 */
	private static final HashMap<KeyStroke, Action> ActionMap = new HashMap<>();

	static {
		// 注册一个键盘事件发布器.
		KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		kfm.addKeyEventDispatcher(e -> {

			KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
			if (HotKeyUtil.ActionMap.containsKey(keyStroke)) {
				final Action action = HotKeyUtil.ActionMap.get(keyStroke);
				final ActionEvent ae = new ActionEvent(e.getSource(), e.getID(),
						null);
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {

						action.actionPerformed(ae);
					}
				});
				return true;
			}
			return false;
		});
	}

	/**
	 * 注册热键. <br/>
	 * 参数详见 {@link javax.swing.KeyStroke#getKeyStroke(int, int)}
	 * 
	 * @param keyCode
	 *               键
	 * @param modifiers
	 *               功能键组合, 没有则为 0
	 * @param action
	 *               动作
	 */
	public static void registerHotKey(int keyCode, int modifiers, AbstractAction action) {

		HotKeyUtil.ActionMap.put(KeyStroke.getKeyStroke(keyCode, modifiers), action);
	}
}
