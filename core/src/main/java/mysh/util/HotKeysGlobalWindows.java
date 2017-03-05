package mysh.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * bind global hot keys. this is OS relative.
 * https://github.com/java-native-access/jna/blob/master/contrib/w32keyhook/com/sun/jna/contrib/demo/KeyHook.java
 *
 * @author mysh
 * @since 2015/8/17
 */
public class HotKeysGlobalWindows implements WinAPI {
	private static final Logger log = LoggerFactory.getLogger(HotKeysGlobalWindows.class);

	@GuardedBy("Class")
	private static Thread msgPeekThread;
	private static final List<KbAction> fullListeners = new CopyOnWriteArrayList<>();
	private static final Multimap<Integer, KbAction> specialListeners = HashMultimap.create();
	/**
	 * a quick check flag
	 */
	private static volatile boolean hasListener;

	public static class KeyDown {
		private boolean ctrl;
		private boolean alt;
		private boolean shift;
		private int vkCode;
		private String vkDesc;
		private boolean winChanges;
		private String winTitle;

		private KeyDown(boolean ctrl, boolean alt, boolean shift, int vkCode, String vkDesc, boolean winChanges, String winTitle) {
			this.ctrl = ctrl;
			this.alt = alt;
			this.shift = shift;
			this.vkCode = vkCode;
			this.vkDesc = vkDesc;
			this.winChanges = winChanges;
			this.winTitle = winTitle;
		}

		/**
		 * whether current pressed key is modifiers only (ctrl or shift or alt, but not winKey)
		 */
		public boolean isModifiers() {
			return vkCode > 15 && vkCode < 19 || vkCode > 159 && vkCode < 166;
		}

		@Override
		public String toString() {
			if ((ctrl || alt || shift) && (vkCode < 16 || vkCode > 18 && vkCode < 160 || vkCode > 165)) {
				StringBuilder sb = new StringBuilder();
				if (ctrl) sb.append("Ctrl+");
				if (alt) sb.append("Alt+");
				if (shift) sb.append("Shift+");
				sb.append(win32Vks[vkCode]);
				return sb.toString();
			} else
				return win32Vks[vkCode];
		}

		public boolean isCtrl() {
			return ctrl;
		}

		public boolean isAlt() {
			return alt;
		}

		public boolean isShift() {
			return shift;
		}

		/**
		 * A <a href='https://msdn.microsoft.com/en-us/library/windows/desktop/dd375731(v=vs.85).aspx'>virtual-key code</a>.
		 * The code must be a value in the range 1 to 254. VKs in {@link java.awt.event.KeyEvent} can be used here.
		 */
		public int getVkCode() {
			return vkCode;
		}

		/**
		 * description of the virtual key, or <i>null</i> if can't be mapped.
		 */
		public String getVkDesc() {
			return vkDesc;
		}

		/**
		 * whether window changes since last action. may not work properly when bind to a combination key.
		 */
		public boolean isWindowChanges() {
			return winChanges;
		}

		public String getWinTitle() {
			return winTitle;
		}
	}

	public interface KbAction {

		/**
		 * key down event. need unblock implementation.
		 */
		void onKeyDown(KeyDown keyDown);
	}

	/**
	 * listening all key actions
	 */
	public static synchronized void addWin32KeyboardListener(KbAction action) {
		chkOS();
		fullListeners.add(action);
		prepareHook();
	}

	public static synchronized void removeWin32KeyboardListener(KbAction action) {
		chkOS();
		fullListeners.remove(action);
		prepareHook();
	}

	/**
	 * listening to specific key action
	 */
	public static synchronized void bindWin32KeyboardListener(
					boolean ctrl, boolean alt, boolean shift, int vkCode, KbAction action) {
		chkOS();
		int key = genKey(ctrl, alt, shift, vkCode);
		specialListeners.put(key, action);
		prepareHook();
	}

	public static synchronized void unbindWin32KeyboardListener(
					boolean ctrl, boolean alt, boolean shift, int vkCode, KbAction action) {
		chkOS();
		int key = genKey(ctrl, alt, shift, vkCode);
		specialListeners.remove(key, action);
		prepareHook();
	}

	private static int genKey(boolean ctrl, boolean alt, boolean shift, int vkCode) {
		if (vkCode < 1 || vkCode > 254) throw new IllegalArgumentException("vkCode should be in [1,254]");
		if (ctrl) vkCode |= 0x8000;
		if (alt) vkCode |= 0x4000;
		if (shift) vkCode |= 0x2000;
		return vkCode;
	}

	private static void chkOS() {
		if (OSs.getOS() != OSs.OS.Windows)
			throw new RuntimeException("current OS is: " + OSs.getOS());
	}

	private static synchronized void prepareHook() {
		if (fullListeners.isEmpty() && specialListeners.isEmpty()) {
			hasListener = false;

			if (msgPeekThread != null) {
				// the way to stop PeekMessage
//				msgPeekThread.interrupt();
//				msgPeekThread = null;

				// no way to stop GetMessage, even though Thread.stop
//				msgPeekThread.stop();
			}
		} else {
			hasListener = true;

			if (msgPeekThread == null) {
				msgPeekThread = new Thread("msgPeekThread") {
					User32.HHOOK kbHook;

					@Override
					public void run() {
						log.info(getName() + " start.");

						User32.MSG msg = new User32.MSG();
						try {
							kbHook();
							while (!this.isInterrupted()) {
								Thread.sleep(27);
								// peekMsg need a return flag, here I use thread interrupt
								// https://msdn.microsoft.com/en-us/library/windows/desktop/ms644943
//								user32.PeekMessage(msg, null, 0, 0, 1);

								// getMsg will never return, even though Thread.stop
								// https://msdn.microsoft.com/en-us/library/windows/desktop/ms644936
								user32.GetMessage(msg, null, 0, 0);
							}
						} catch (Throwable e) {
							log.error("peek msg error", e);
						} finally {
							kbUnHook();
						}
					}

					private boolean isWindowChanges;
					private long lastWindowPointer;
					private String lastWindowTitle;

					private void chkWindow() {
						long winPtr = Pointer.nativeValue(user32.GetForegroundWindow().getPointer());
						if (winPtr != lastWindowPointer) {
							isWindowChanges = true;
							lastWindowPointer = winPtr;
							lastWindowTitle = WinAPI.getForeGroundWindowTitle();
						} else {
							isWindowChanges = false;
						}
					}

					/**
					 * https://msdn.microsoft.com/en-us/library/windows/desktop/ms644985
					 */
					private WinUser.LowLevelKeyboardProc kbProc =
									(User32.LowLevelKeyboardProc) (int nCode, WinDef.WPARAM wp, User32.KBDLLHOOKSTRUCT kbStruct) -> {
										if (hasListener &&
														(wp.intValue() == User32.WM_KEYDOWN || wp.intValue() == User32.WM_SYSKEYDOWN)) {
											boolean ctrl = (user32.GetAsyncKeyState(User32.VK_CONTROL) & 0x8000) != 0;
											boolean alt = (user32.GetAsyncKeyState(User32.VK_MENU) & 0x8000) != 0;
											boolean shift = (user32.GetAsyncKeyState(User32.VK_SHIFT) & 0x8000) != 0;
											int vkCode = kbStruct.vkCode;
											String vkDesc = (vkCode > 0 && vkCode < 255) ? win32Vks[vkCode] : null;
											chkWindow();

											KeyDown keyDown = new KeyDown(ctrl, alt, shift, vkCode, vkDesc,
															isWindowChanges, lastWindowTitle);

											for (KbAction listener : fullListeners) {
												try {
													listener.onKeyDown(keyDown);
												} catch (Throwable t) {
													log.error("handle full keyDown action error: " + keyDown, t);
												}
											}

											int key = genKey(ctrl, alt, shift, vkCode);
											if (specialListeners.containsKey(key)) {
												for (KbAction listener : specialListeners.get(key)) {
													try {
														listener.onKeyDown(keyDown);
													} catch (Throwable t) {
														log.error("handle specific keyDown action error: " + keyDown, t);
													}
												}
											}
										}

										return user32.CallNextHookEx(kbHook, nCode, wp,
														new WinDef.LPARAM(Pointer.nativeValue(kbStruct.getPointer())));
									};

					private WinDef.HMODULE hMod = kernel32.GetModuleHandle(null);

					/**
					 * hook and msg peek need to be within the same thread
					 */
					private void kbHook() {
						if (kbHook != null)
							kbUnHook();

						log.info("hook kb action");

						// https://msdn.microsoft.com/en-us/library/windows/desktop/ms644990
						kbHook = user32.SetWindowsHookEx(User32.WH_KEYBOARD_LL, kbProc, hMod, 0);
					}

					private void kbUnHook() {
						if (kbHook != null) {
							log.info("unhook kb action");
							if (!user32.UnhookWindowsHookEx(kbHook)) {
								log.error("UnhookWindowsHookEx fail");
							}
							kbHook = null;
						}
					}
				};
				msgPeekThread.setDaemon(true);
				msgPeekThread.start();
			}
		}
	}

	private static String[] win32Vks = {
					"0x0",
					"Left mouse button",
					"Right mouse button",
					"Control-break processing",
					"Middle mouse button (three-button mouse)",
					"X1 mouse button",
					"X2 mouse button",
					"Undefined",
					"BACKSPACE",
					"TAB",
					"Reserved",
					"Reserved",
					"CLEAR",
					"ENTER",
					"Undefined",
					"Undefined",
					"Shift",
					"Ctrl",
					"Alt",
					"PAUSE",
					"CAPS LOCK",
					"IME",
					"Undefined",
					"IME Junja mode",
					"IME final mode",
					"IME Hanja | Kanji mode",
					"Undefined",
					"ESC",
					"IME convert",
					"IME nonconvert",
					"IME accept",
					"IME mode change request",
					"SPACEBAR",
					"PAGE UP",
					"PAGE DOWN",
					"END",
					"HOME",
					"LEFT ARROW",
					"UP ARROW",
					"RIGHT ARROW",
					"DOWN ARROW",
					"SELECT",
					"PRINT",
					"EXECUTE",
					"PRINT SCREEN",
					"INS",
					"DEL",
					"HELP",
					"0",
					"1",
					"2",
					"3",
					"4",
					"5",
					"6",
					"7",
					"8",
					"9",
					"Undefined",
					"Undefined",
					"Undefined",
					"Undefined",
					"Undefined",
					"Undefined",
					"Undefined",
					"A",
					"B",
					"C",
					"D",
					"E",
					"F",
					"G",
					"H",
					"I",
					"J",
					"K",
					"L",
					"M",
					"N",
					"O",
					"P",
					"Q",
					"R",
					"S",
					"T",
					"U",
					"V",
					"W",
					"X",
					"Y",
					"Z",
					"Left Windows",
					"Right Windows",
					"Applications",
					"Reserved",
					"Computer Sleep",
					"NumPad 0",
					"NumPad 1",
					"NumPad 2",
					"NumPad 3",
					"NumPad 4",
					"NumPad 5",
					"NumPad 6",
					"NumPad 7",
					"NumPad 8",
					"NumPad 9",
					"Multiply",
					"Add",
					"Separator",
					"Subtract",
					"Decimal",
					"Divide",
					"F1",
					"F2",
					"F3",
					"F4",
					"F5",
					"F6",
					"F7",
					"F8",
					"F9",
					"F10",
					"F11",
					"F12",
					"F13",
					"F14",
					"F15",
					"F16",
					"F17",
					"F18",
					"F19",
					"F20",
					"F21",
					"F22",
					"F23",
					"F24",
					"Unassigned",
					"Unassigned",
					"Unassigned",
					"Unassigned",
					"Unassigned",
					"Unassigned",
					"Unassigned",
					"Unassigned",
					"NUM LOCK",
					"SCROLL LOCK",
					"OEM specific",
					"OEM specific",
					"OEM specific",
					"OEM specific",
					"OEM specific",
					"Unassigned",
					"Unassigned",
					"Unassigned",
					"Unassigned",
					"Unassigned",
					"Unassigned",
					"Unassigned",
					"Unassigned",
					"Unassigned",
					"LShift",
					"RShift",
					"LCtrl",
					"RCtrl",
					"LAlt",
					"RAlt",
					"Browser Back",
					"Browser Forward",
					"Browser Refresh",
					"Browser Stop",
					"Browser Search",
					"Browser Favorites",
					"Browser Start and Home",
					"Volume Mute",
					"Volume Down",
					"Volume Up",
					"Next Track",
					"Previous Track",
					"Stop Media",
					"Play/Pause Media",
					"Start Mail",
					"Select Media",
					"Start Application 1",
					"Start Application 2",
					"Reserved",
					"Reserved",
					"US: ;:",
					"+",
					",",
					"-",
					".",
					"US: /?",
					"US: `~",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Reserved",
					"Unassigned",
					"Unassigned",
					"Unassigned",
					"US: [{",
					"US: \\|",
					"US: ]}",
					"US: '\"",
					"miscellaneous characters",
					"Reserved",
					"OEM specific",
					"angle bracket | backslash",
					"OEM specific",
					"OEM specific",
					"IME PROCESS",
					"OEM specific",
					"VK_PACKET",
					"Unassigned",
					"OEM specific",
					"OEM specific",
					"OEM specific",
					"OEM specific",
					"OEM specific",
					"OEM specific",
					"OEM specific",
					"OEM specific",
					"OEM specific",
					"OEM specific",
					"OEM specific",
					"OEM specific",
					"OEM specific",
					"Attn",
					"CrSel",
					"ExSel",
					"Erase EOF",
					"Play",
					"Zoom",
					"Reserved",
					"PA1",
					"Clear",
	};
}
