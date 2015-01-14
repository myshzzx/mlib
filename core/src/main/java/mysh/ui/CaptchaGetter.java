package mysh.ui;

import mysh.net.httpclient.HttpClientAssist;
import mysh.net.httpclient.HttpClientConfig;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * @author Mysh
 * @since 2015/1/14 9:44
 */
public class CaptchaGetter {

	private final JFrame frame;
	private final JTextField text;
	private final CountDownLatch textLatch;

	/**
	 * display a captcha image and a input text field to get captcha by human labor.
	 *
	 * @param imgUrl     image url.
	 * @param title      window title, can be null.
	 * @param httpAssist http client, can be null.
	 * @param reqHeaders request headers, can be null.
	 */
	public CaptchaGetter(String imgUrl, String title,
	                     HttpClientAssist httpAssist, Map<String, String> reqHeaders)
					throws IOException, InterruptedException {

		Objects.requireNonNull(imgUrl, "image url can't be null");

		if (httpAssist == null)
			httpAssist = new HttpClientAssist(new HttpClientConfig());

		try (HttpClientAssist.UrlEntity imgUe = httpAssist.access(imgUrl, reqHeaders)) {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			imgUe.bufWriteTo(buf);
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(buf.toByteArray()));

			frame = new JFrame();
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frame.getContentPane().setLayout(new BorderLayout());
			CaptchaPanel imgPanel = new CaptchaPanel(img);
			frame.add(imgPanel, BorderLayout.CENTER);

			textLatch = new CountDownLatch(1);
			text = new JFormattedTextField();
			text.addActionListener(e -> textLatch.countDown());

			frame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					textLatch.countDown();
				}
			});
			frame.add(text, BorderLayout.SOUTH);
			frame.setBounds(0, 0, 350, 200);
			frame.setLocationRelativeTo(null);
			frame.setTitle(title != null ? title : "Input Captcha");
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
		}
	}

	/**
	 * get input captcha.
	 */
	public String getCaptcha() throws InterruptedException {
		textLatch.await();
		frame.dispose();
		return text.getText();
	}

	private static class CaptchaPanel extends JPanel {
		private static final long serialVersionUID = 4833366611371266515L;
		private BufferedImage img;

		public CaptchaPanel(BufferedImage img) {
			this.img = img;
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(img, 0, 0, null);
		}
	}
}
