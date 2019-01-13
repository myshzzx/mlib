/*
 * Copyright (c) 2011 Denis Tulskiy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package mysh.tulskiy.keymaster.windows;

import com.sun.jna.platform.win32.User32;
import mysh.tulskiy.keymaster.common.HotKey;
import mysh.tulskiy.keymaster.common.HotKeyListener;
import mysh.tulskiy.keymaster.common.MediaKey;
import mysh.tulskiy.keymaster.common.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import static com.sun.jna.platform.win32.User32.*;

/**
 * Author: Denis Tulskiy
 * Date: 6/12/11
 */
public class WindowsProvider extends Provider {
    private static final Logger LOGGER = LoggerFactory.getLogger(WindowsProvider.class);
    private static volatile int idSeq = 0;

    private boolean listen;
    private Boolean reset = false;
    private final Object lock = new Object();
    private Thread thread;

    private Map<Integer, HotKey> hotKeys = new HashMap<Integer, HotKey>();
    private Queue<HotKey> registerQueue = new LinkedList<HotKey>();

    public void init() {
        Runnable runnable = new Runnable() {
            public void run() {
                LOGGER.info("Starting Windows global hotkey provider");
                MSG msg = new MSG();
                listen = true;
                while (listen) {
                    // https://msdn.microsoft.com/en-us/library/windows/desktop/ms644943
                    while (User32.INSTANCE.PeekMessage(msg, null, 0, 0, 0x0001)) {
                        if (msg.message == WM_HOTKEY) {
                            int id = msg.wParam.intValue();
                            HotKey hotKey = hotKeys.get(id);

                            if (hotKey != null) {
                                fireEvent(hotKey);
                            }
                        }
                    }

                    synchronized (lock) {
                        if (reset) {
                            LOGGER.info("Reset hotkeys");
                            for (Integer id : hotKeys.keySet()) {
                                User32.INSTANCE.UnregisterHotKey(null, id);
                            }

                            hotKeys.clear();
                            reset = false;
                            lock.notify();
                        }

                        while (!registerQueue.isEmpty()) {
                            register(registerQueue.poll());
                        }
                        try {
                            lock.wait(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                LOGGER.info("Exit listening thread");
            }
        };

        thread = new Thread(runnable);
        thread.start();
    }

    private void register(HotKey hotKey) {
        int id = idSeq++;
        int code = KeyMap.getCode(hotKey);
        if (User32.INSTANCE.RegisterHotKey(null, id, KeyMap.getModifiers(hotKey.keyStroke), code)) {
            LOGGER.info("Registering hotkey: " + hotKey);
            hotKeys.put(id, hotKey);
        } else {
            LOGGER.warn("Could not register hotkey: " + hotKey);
        }
    }

    public void register(KeyStroke keyCode, HotKeyListener listener) {
        synchronized (lock) {
            registerQueue.add(new HotKey(keyCode, listener));
        }
    }

    public void register(MediaKey mediaKey, HotKeyListener listener) {
        synchronized (lock) {
            registerQueue.add(new HotKey(mediaKey, listener));
        }
    }

    public void reset() {
        synchronized (lock) {
            reset = true;
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        listen = false;
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        super.stop();
    }
}
