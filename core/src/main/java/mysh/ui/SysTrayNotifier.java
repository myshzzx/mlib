package mysh.ui;

import mysh.util.Oss;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;

/**
 * 任务栏通知.
 * 左键点击执行提示, 中键点击取消提示.
 *
 * @author Mysh
 * @since 2014/4/1 20:59
 */
public class SysTrayNotifier implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(SysTrayNotifier.class);
	
	/**
	 * msgs will be regarded equal if their class are the same and getMsg() return the same.
	 */
	public static abstract class Msg {
		protected Date receivedTime;
		
		public String getTitle() {
			return SysTrayNotifier.class.getName();
		}
		
		/**
		 * will not display tray icon bubble msg if this return null.
		 */
		public abstract String getMsg();
		
		public TrayIcon.MessageType getType() {
			return TrayIcon.MessageType.INFO;
		}
		
		public void act() {
		}
		
		@Override
		public boolean equals(Object obj) {
			return this == obj ||
					obj instanceof Msg && getClass() == obj.getClass()
							&& Objects.equals(getMsg(), ((Msg) obj).getMsg());
		}
		
		@Override
		public int hashCode() {
			String msg = getMsg();
			return msg != null ? msg.hashCode() : super.hashCode();
		}
		
		@Override
		public abstract String toString();
	}
	
	public static class ActionListener {
		/**
		 * @param unHandledMsgs unHandled msgs.
		 */
		protected void onAction(Set<Msg> unHandledMsgs) {
		}
		
		/**
		 * @param msg msg.
		 * @return flash icon or not.
		 */
		protected boolean isMsgFlash(Msg msg) {
			return true;
		}
	}
	
	private static final ActionListener DEFAULT_LISTENER = new ActionListener();
	
	private final Image blankImg;
	
	private final TrayIcon icon;
	private String title;
	private final ActionListener listener;
	private final Image oriTrayImg;
	
	private volatile Image currentImg;
	private volatile Set<Msg> unHandledMsgs = Collections.synchronizedSet(new HashSet<>());
	
	/**
	 * create a new sysTray notifier.
	 *
	 * @param icon     icon displayed in system tray.
	 * @param title    default icon title. can be null.
	 * @param listener system tray icon action listener. can be null.
	 */
	public SysTrayNotifier(final TrayIcon icon, String title, ActionListener listener) throws IOException {
		this.icon = icon;
		this.title = title == null ? getClass().getSimpleName() : title;
		this.listener = listener == null ? DEFAULT_LISTENER : listener;
		icon.setToolTip(this.title);
		
		currentImg = oriTrayImg = icon.getImage();
		blankImg = ImageIO.read(Thread.currentThread().getContextClassLoader().getResource("mysh/ui/blank16.png"));
		
		Runnable notifyAction = () -> {
			Set<Msg> t = this.unHandledMsgs;
			this.unHandledMsgs = Collections.synchronizedSet(new HashSet<>());
			this.listener.onAction(t);
			setFlashing(false);
			icon.setToolTip(this.title);
		};
		Runnable cancelAction = () -> {
			this.unHandledMsgs.clear();
			setFlashing(false);
			icon.setToolTip(this.title);
		};
		
		icon.addActionListener(e -> notifyAction.run());
		icon.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (Oss.getOS() == Oss.OS.Mac) {
					switch (e.getButton()) {
						case MouseEvent.BUTTON3:
							notifyAction.run();
							break;
						case MouseEvent.BUTTON2:
							cancelAction.run();
							break;
					}
				} else {
					switch (e.getButton()) {
						case MouseEvent.BUTTON1:
							notifyAction.run();
							break;
						case MouseEvent.BUTTON2:
							cancelAction.run();
							break;
					}
				}
			}
		});
		
		// flash thread
		flashThread = new Thread(this.getClass().getName() + "-flasher") {
			int iconFlashTime = 400;
			
			@Override
			public void run() {
				try {
					while (true) {
						icon.setImage(oriTrayImg);
						Thread.sleep(iconFlashTime);
						icon.setImage(currentImg);
						Thread.sleep(iconFlashTime);
					}
				} catch (Exception e) {
					if (!(e instanceof InterruptedException))
						log.error("systray icon flash error.", e);
				}
			}
		};
		flashThread.setDaemon(true);
		flashThread.start();
	}
	
	private Thread flashThread;
	
	@Override
	public void close() {
		flashThread.interrupt();
	}
	
	public void newMsg(Msg msg) {
		log.info("on receive new msg: " + msg);
		
		msg.receivedTime = new Date();
		if (msg.getMsg() != null)
			this.icon.displayMessage(msg.getTitle(), msg.getMsg(),
					msg.getType() == null ? TrayIcon.MessageType.INFO : msg.getType());
		
		this.unHandledMsgs.add(msg);
		
		final StringBuilder tooltips = new StringBuilder();
		unHandledMsgs.stream()
		             .filter(m -> m.getMsg() != null)
		             .forEach(m -> {
			             tooltips.append(m.getMsg());
			             tooltips.append('\n');
		             });
		icon.setToolTip("");
		icon.setToolTip(tooltips.toString());
		
		if (this.listener.isMsgFlash(msg))
			this.setFlashing(true);
	}
	
	private void setFlashing(boolean flag) {
		if (flag)
			currentImg = blankImg;
		else
			currentImg = oriTrayImg;
	}
	
}
