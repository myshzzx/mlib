
package mysh.util;

import javax.annotation.concurrent.GuardedBy;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 本地热键工具类.
 *
 * @author Allen
 */
public class HotKeysLocal {
	/**
	 * 键-动作映射.
	 * ActionMap doesn't need to be concurrent component because modify actions are protected
	 * by monitor lock, and key event dispatcher is in single thread env.
	 */
	@GuardedBy("self.Monitor")
	private static final Map<KeyStroke, Queue<Action>> ActionMap = new HashMap<>();

	static {
		// 注册一个键盘事件发布器.
		KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		kfm.addKeyEventDispatcher(e -> {
			KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
			Queue<Action> actions = ActionMap.get(keyStroke);
			if (actions != null) {
				ActionEvent ae = new ActionEvent(e.getSource(), e.getID(), null);
				for (Action action : actions) {
					SwingUtilities.invokeLater(() -> action.actionPerformed(ae));
				}
			}
			return false;
		});
	}

	/**
	 * 注册热键. <br/>
	 * 参数详见 {@link javax.swing.KeyStroke#getKeyStroke(int, int)}
	 *
	 * @param keyCode   键
	 * @param modifiers 功能键组合, 没有则为 0
	 * @param action    动作
	 */
	public static void registerHotKey(int keyCode, int modifiers, AbstractAction action) {
		synchronized (ActionMap) {
			KeyStroke ks = KeyStroke.getKeyStroke(keyCode, modifiers);
			ActionMap.computeIfAbsent(ks, k -> new ConcurrentLinkedQueue<>()).add(action);
		}
	}

	/**
	 * 反注册热键. <br/>
	 * 参数详见 {@link javax.swing.KeyStroke#getKeyStroke(int, int)}
	 *
	 * @param keyCode   键
	 * @param modifiers 功能键组合, 没有则为 0
	 * @param action    动作
	 */
	public static void unRegisterHotKey(int keyCode, int modifiers, AbstractAction action) {
		synchronized (ActionMap) {
			KeyStroke ks = KeyStroke.getKeyStroke(keyCode, modifiers);
			Queue<Action> actions = ActionMap.get(ks);
			if (actions != null) {
				actions.remove(action);
				if (actions.isEmpty())
					ActionMap.remove(ks);
			}
		}
	}
}
