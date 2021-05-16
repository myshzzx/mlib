package mysh.os;

import mysh.tulskiy.keymaster.common.HotKeyListener;
import mysh.tulskiy.keymaster.common.Provider;

import javax.swing.*;

/**
 * HotKeysGlobal
 *
 * @author mysh
 * @since 2019/1/13
 */
public class HotKeysGlobal {
	
	private static Provider provider;
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(HotKeysGlobal::stop));
	}
	
	/**
	 * @param keyStroke for example: <b>control shift PLUS</b>, see {@link javax.swing.KeyStroke#getKeyStroke(java.lang.String)}
	 */
	public synchronized static void registerKeyListener(String keyStroke, HotKeyListener action) {
		registerKeyListener(KeyStroke.getKeyStroke(keyStroke), action);
	}
	
	public synchronized static void unregisterKeyListener(String keyStroke, HotKeyListener action) {
		unregisterKeyListener(KeyStroke.getKeyStroke(keyStroke), action);
	}
	
	public synchronized static void registerKeyListener(KeyStroke keyStroke, HotKeyListener action) {
		if (provider == null)
			provider = Provider.getCurrentProvider(true);
		provider.register(keyStroke, action);
	}
	
	public synchronized static void unregisterKeyListener(KeyStroke keyStroke, HotKeyListener action) {
		if (provider != null)
			provider.unregister(keyStroke, action);
	}
	
	/**
	 * Reset all hotkey listeners
	 */
	public synchronized static void reset() {
		if (provider != null)
			provider.reset();
	}
	
	public synchronized static void stop() {
		if (provider != null) {
			provider.stop();
			provider = null;
		}
	}
}
