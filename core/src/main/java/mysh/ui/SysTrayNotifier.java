package mysh.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.*;

/**
 * 任务栏通知.
 * 左键点击执行提示, 中键点击取消提示.
 *
 * @author Mysh
 * @since 2014/4/1 20:59
 */
public class SysTrayNotifier {
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
	}

	public static interface ActionListener {
		/**
		 * @param e
		 * @param unHandledMsgs unHandled msgs.
		 */
		public void onAction(ActionEvent e, Set<Msg> unHandledMsgs);

		/**
		 * @param msg msg.
		 * @return flash icon or not.
		 */
		default boolean onReceivingMsg(Msg msg) {
			return true;
		}
	}

	private final Image blankImg;

	private final TrayIcon icon;
	private final ActionListener listener;
	private final Image oriTrayImg;

	private volatile Image currentImg;
	private volatile Set<Msg> unHandledMsgs = Collections.synchronizedSet(new HashSet<>());

	public SysTrayNotifier(final TrayIcon icon, ActionListener listener) throws IOException {
		this.icon = icon;
		this.listener = listener;

		currentImg = oriTrayImg = icon.getImage();
		blankImg = ImageIO.read(Thread.currentThread().getContextClassLoader().getResource("mysh/ui/blank.gif"));

		icon.addActionListener(e -> {
							if (listener != null) {
								Set<Msg> t = unHandledMsgs;
								unHandledMsgs = Collections.synchronizedSet(new HashSet<>());
								listener.onAction(e, t);
							}
							setFlashing(false);
						}
		);

		// flash thread
		Thread flashThread = new Thread(this.getClass().getName() + "-flasher") {
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
					log.error("systray icon flash error.", e);
				}
			}
		};
		flashThread.setDaemon(true);
		flashThread.start();
	}

	public void newMsg(Msg msg) {
		msg.receivedTime = new Date();
		if (msg.getMsg() != null)
			this.icon.displayMessage(msg.getTitle(), msg.getMsg(),
							msg.getType() == null ? TrayIcon.MessageType.INFO : msg.getType());

		this.unHandledMsgs.add(msg);
		if (this.listener != null && this.listener.onReceivingMsg(msg))
			this.setFlashing(true);
	}

	private void setFlashing(boolean flag) {
		if (flag)
			currentImg = blankImg;
		else
			currentImg = oriTrayImg;
	}

}
