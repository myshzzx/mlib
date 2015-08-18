package mysh.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static mysh.util.WinAPI.*;

/**
 * bind global hot keys. this is OS relative.
 *
 * @author mysh
 * @since 2015/8/17
 */
public class HotKeysGlobal {
	private static final Logger log = LoggerFactory.getLogger(HotKeysGlobal.class);

	private static Thread win32MsgPeekThread;
	private static final Multimap<Integer, Win32KbAction> win32KbListeners = HashMultimap.create();

	public interface Win32KbAction {
		/**
		 * @param vkCode     A <a href='https://msdn.microsoft.com/en-us/library/windows/desktop/dd375731(v=vs.85).aspx'>virtual-key code</a>.
		 *                   The code must be a value in the range 1 to 254. VKs in {@link java.awt.event.KeyEvent} can be used here.
		 * @param vkDesc     description of the virtual key, or <i>null</i> if can't be mapped.
		 * @param winChanges whether window changes since last action. may not work properly when bind to a combination key.
		 */
		void onKeyDown(boolean ctrl, boolean alt, boolean shift, int vkCode, String vkDesc, boolean winChanges, String winTitle);
	}

	public static synchronized void addWin32KeyboardListener(Win32KbAction action) {
		chkWindows();
		win32KbListeners.put(0, action);
		prepareWin32Hook();
	}

	public static synchronized void removeWin32KeyboardListener(Win32KbAction action) {
		chkWindows();
		win32KbListeners.remove(0, action);
		prepareWin32Hook();
	}

	public static synchronized void bindWin32KeyboardListener(
					boolean ctrl, boolean alt, boolean shift, int vkCode, Win32KbAction action) {
		chkWindows();
		int key = genKey(ctrl, alt, shift, vkCode);
		win32KbListeners.put(key, action);
		prepareWin32Hook();
	}

	public static synchronized void unbindWin32KeyboardListener(
					boolean ctrl, boolean alt, boolean shift, int vkCode, Win32KbAction action) {
		chkWindows();
		int key = genKey(ctrl, alt, shift, vkCode);
		win32KbListeners.remove(key, action);
		prepareWin32Hook();
	}

	private static int genKey(boolean ctrl, boolean alt, boolean shift, int vkCode) {
		if (vkCode < 1 || vkCode > 254) throw new IllegalArgumentException("vkCode should be in [1,254]");
		if (ctrl) vkCode |= 0x8000;
		if (alt) vkCode |= 0x4000;
		if (shift) vkCode |= 0x2000;
		return vkCode;
	}

	private static void chkWindows() {
		if (OSs.getOS() != OSs.OS.Windows)
			throw new RuntimeException("current OS is: " + OSs.getOS());
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
					User32.HHOOK kbHook;

					@Override
					public void run() {
						log.info(getName() + " start.");

						User32.MSG msg = new User32.MSG();
						try {
							long lastHookTime = 0, now;
							while (!this.isInterrupted()) {
								now = System.currentTimeMillis();
								if (now - lastHookTime > 100 * 1000) { // rehook every 100 seconds, in case of hook fails in a few minutes
									kbUnHook();
									kbHook();
									lastHookTime = now;
								}

//							https://msdn.microsoft.com/en-us/library/windows/desktop/ms644943(v=vs.85).aspx
								user32.PeekMessage(msg, null, 0, 0, 1);
								Thread.sleep(20);
							}
						} catch (Throwable e) {
							log.error("peek msg error", e);
						} finally {
							kbUnHook();
						}
					}

					private boolean isWindowChanges;
					private Pointer lastWindowPointer;
					private String lastWindowTitle;

					private void chkWindow() {
						Pointer winPtr = user32.GetForegroundWindow().getPointer();
						if (!winPtr.equals(lastWindowPointer)) {
							isWindowChanges = true;
							lastWindowPointer = winPtr;
							lastWindowTitle = getForeGroundWindowTitle();
						} else {
							isWindowChanges = false;
						}
					}

					/**
					 * https://msdn.microsoft.com/en-us/library/windows/desktop/ms644985(v=vs.85).aspx
					 */
					private WinUser.LowLevelKeyboardProc kbProc =
									(User32.LowLevelKeyboardProc) (int nCode, WinDef.WPARAM wp, User32.KBDLLHOOKSTRUCT kbStruct) -> {
										if (wp.intValue() == User32.WM_KEYDOWN || wp.intValue() == User32.WM_SYSKEYDOWN) {
											boolean ctrl = (user32.GetAsyncKeyState(User32.VK_CONTROL) & 0x8000) != 0;
											boolean alt = (user32.GetAsyncKeyState(User32.VK_MENU) & 0x8000) != 0;
											boolean shift = (user32.GetAsyncKeyState(User32.VK_SHIFT) & 0x8000) != 0;
											int vkCode = kbStruct.vkCode;
											String vkDesc = (vkCode > 0 && vkCode < 255) ? win32Vks[vkCode] : null;
											chkWindow();

											for (Win32KbAction listener : win32KbListeners.get(0)) {
												try {
													listener.onKeyDown(ctrl, alt, shift, vkCode, vkDesc, isWindowChanges, lastWindowTitle);
												} catch (Throwable t) {
													log.error("handle key action error", t);
												}
											}
											if (win32KbListeners.size() > 0) {
												int key = genKey(ctrl, alt, shift, vkCode);
												for (Win32KbAction listener : win32KbListeners.get(key)) {
													try {
														listener.onKeyDown(ctrl, alt, shift, vkCode, vkDesc, isWindowChanges, lastWindowTitle);
													} catch (Throwable t) {
														log.error("handle key action error", t);
													}
												}
											}
										}

										return user32.CallNextHookEx(kbHook, nCode, wp, kbStruct.getPointer());
									};

					private WinDef.HMODULE hMod = kernel32.GetModuleHandle(null);

					/**
					 * hook and msg peek need to be within the same thread
					 */
					private void kbHook() {
						if (kbHook != null) kbUnHook();

						// https://msdn.microsoft.com/en-us/library/windows/desktop/ms644990(v=vs.85).aspx
						kbHook = user32.SetWindowsHookEx(User32.WH_KEYBOARD_LL, kbProc, hMod, 0);
					}

					private void kbUnHook() {
						if (kbHook != null) {
//							log.info("UnhookWindowsHookEx");
							if (!user32.UnhookWindowsHookEx(kbHook)) {
								log.error("UnhookWindowsHookEx fail");
							}
							kbHook = null;
						}
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
