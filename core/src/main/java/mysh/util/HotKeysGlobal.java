package mysh.util;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * HotKeysGlobal
 *
 * @author mysh
 * @since 2015/8/17
 */
public class HotKeysGlobal {
	private static final Logger log = LoggerFactory.getLogger(HotKeysGlobal.class);

	private static Thread win32MsgPeekThread;
	private static List<Win32KbAction> win32KbListeners = new ArrayList<>();

	public interface Win32KbAction {
		/**
		 * @param vkCode A <a href='https://msdn.microsoft.com/en-us/library/windows/desktop/dd375731(v=vs.85).aspx'>virtual-key code</a>.
		 *               The code must be a value in the range 1 to 254.
		 * @param vkDesc description of the virtual key, or <i>null</i> if can't be mapped.
		 */
		void onKeyDown(boolean ctrl, boolean alt, boolean shift, int vkCode, String vkDesc);
	}

	public static synchronized void addWin32KeyboardListener(Win32KbAction action) {
		if (OSs.getOS() != OSs.OS.Windows)
			throw new RuntimeException("current OS is: " + OSs.getOS());

		win32KbListeners.add(action);
		prepareWin32Hook();
	}

	public static synchronized void removeWin32KeyboardListener(Win32KbAction action) {
		if (OSs.getOS() != OSs.OS.Windows)
			throw new RuntimeException("current OS is: " + OSs.getOS());

		win32KbListeners.remove(action);
		prepareWin32Hook();
	}

	private static void prepareWin32Hook() {
		if (win32KbListeners.size() == 0) {
			if (win32MsgPeekThread != null) {
				win32MsgPeekThread.interrupt();
				win32MsgPeekThread = null;
			}
		} else {
			if (win32MsgPeekThread == null) {
				win32MsgPeekThread = new Thread("win32MsgPeekThread") {
					User32.HHOOK win32KbHook;

					@Override
					public void run() {
						win32KbHook();

						User32.MSG msg = new User32.MSG();
						System.out.println("thread start");
						try {
							while (!this.isInterrupted()) {
//							https://msdn.microsoft.com/en-us/library/windows/desktop/ms644943(v=vs.85).aspx
								User32.INSTANCE.PeekMessage(msg, null, 0, 0, 0);
								Thread.sleep(30);
							}
						} catch (Exception e) {
							log.error("peek msg error", e);
						} finally {
							if (!User32.INSTANCE.UnhookWindowsHookEx(win32KbHook)) {
								log.error("UnhookWindowsHookEx fail");
							}
						}
					}

					/**
					 * hook and msg peek need to be within the same thread
					 */
					private void win32KbHook() {
						// https://msdn.microsoft.com/en-us/library/windows/desktop/ms644990(v=vs.85).aspx
						win32KbHook = User32.INSTANCE.SetWindowsHookEx(User32.WH_KEYBOARD_LL,
										// https://msdn.microsoft.com/en-us/library/windows/desktop/ms644985(v=vs.85).aspx
										(User32.LowLevelKeyboardProc) (int nCode, WinDef.WPARAM wp, User32.KBDLLHOOKSTRUCT kbStruct) -> {
											if (wp.intValue() == User32.WM_KEYDOWN || wp.intValue() == User32.WM_SYSKEYDOWN) {
												short ctrl = User32.INSTANCE.GetAsyncKeyState(User32.VK_CONTROL);
												short alt = User32.INSTANCE.GetAsyncKeyState(User32.VK_MENU);
												short shift = User32.INSTANCE.GetAsyncKeyState(User32.VK_SHIFT);
												int vkCode = kbStruct.vkCode;
												for (Win32KbAction listener : win32KbListeners) {
													try {
														listener.onKeyDown(
																		(ctrl & 0x8000) != 0,
																		(alt & 0x8000) != 0,
																		(shift & 0x8000) != 0,
																		vkCode,
																		(vkCode > 0 && vkCode < 255) ? win32Vks[vkCode] : null);
													} catch (Throwable t) {
														log.error("handle key action error", t);
													}
												}
											}
											return User32.INSTANCE.CallNextHookEx(win32KbHook, nCode, wp, kbStruct.getPointer());
										}
										, Kernel32.INSTANCE.GetModuleHandle(null), 0);
					}
				};
				win32MsgPeekThread.setDaemon(true);
				win32MsgPeekThread.start();
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
					"SHIFT",
					"CTRL",
					"ALT",
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
					"Num pad 0",
					"Num pad 1",
					"Num pad 2",
					"Num pad 3",
					"Num pad 4",
					"Num pad 5",
					"Num pad 6",
					"Num pad 7",
					"Num pad 8",
					"Num pad 9",
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
					"Left SHIFT",
					"Right SHIFT",
					"Left CONTROL",
					"Right CONTROL",
					"Left MENU",
					"Right MENU",
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
