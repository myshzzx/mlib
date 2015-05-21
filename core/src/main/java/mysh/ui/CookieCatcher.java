package mysh.ui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * open a web browser to get cookie.
 *
 * @author Mysh
 * @since 2015/1/12 17:55
 */
public class CookieCatcher {
	private final CountDownLatch closeLatch = new CountDownLatch(1);
	private final String cookieUrl;
	private volatile WebView browser;

	/**
	 * open a web browser to get cookie.
	 * <br/>
	 * WARNING: Platform.setImplicitExit(false) will be invoked when the open window closed, so
	 * javaFx-Application-Thread will not exit even if last window closed, and new opened browser
	 * can work normally.
	 * The thread is not daemon, to terminate it, invoke Platform.setImplicitExit(true) or
	 * Platform.exit().
	 *
	 * @param url       open url.
	 * @param cookieUrl get cookies where sent to.
	 */
	public CookieCatcher(String url, String cookieUrl) {
		this.cookieUrl = cookieUrl;

		JFXPanel fxPanel = new JFXPanel();
		Platform.runLater(() -> {
			browser = new WebView();
			Scene scene = new Scene(browser);
			fxPanel.setScene(scene);
			WebEngine browserEngine = browser.getEngine();
			browserEngine.load(url);
			browserEngine.setOnAlert(evt -> JOptionPane.showMessageDialog(fxPanel, evt.getData()));
		});

		SwingUtilities.invokeLater(() -> {
			JFrame f = new JFrame();
			f.setBounds(0, 0, 1000, 700);
			f.setLocationRelativeTo(null);
			f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			f.addWindowListener(
							new WindowAdapter() {
								@Override
								public void windowClosing(WindowEvent e) {
									Platform.setImplicitExit(false);
									closeLatch.countDown();
								}
							}
			);
			f.add(fxPanel);
			f.setVisible(true);
		});
	}

	/**
	 * wait for web browser window closed.
	 */
	public void waitForClose() throws InterruptedException {
		closeLatch.await();
	}

	/**
	 * get cookie that will be sent to {@link #cookieUrl}.
	 * this method will be blocked until the browser closed. return won't be null.
	 * <br/>
	 * cookie can also be got by
	 * <code>com.sun.webkit.network.CookieManager.getDefault().get("Cookie")</code>
	 */
	public List<String> getCookie() throws URISyntaxException, IOException, InterruptedException {
		this.waitForClose();

		CookieHandler cookieHandler = CookieManager.getDefault();
		if (cookieHandler == null) return emptyCookie;

		Map<String, List<String>> cm = cookieHandler.get(new URI(cookieUrl), Collections.emptyMap());

		if (cm == null)
			return emptyCookie;
		List<String> cookie = cm.get("Cookie");
		return cookie == null || cookie.size() == 0 ? emptyCookie : cookie;
	}

	private static final List<String> emptyCookie = Collections.singletonList("");

}
