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
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Mysh
 * @since 2015/1/14 9:44
 */
public class ImgRecognizer {
	
	private final JFrame frame;
	private final JTextField text;
	private final CountDownLatch textLatch;
	
	public ImgRecognizer() {
		frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		
		textLatch = new CountDownLatch(1);
		text = new JFormattedTextField();
		text.addActionListener(e -> textLatch.countDown());
		
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				frame.dispose();
				textLatch.countDown();
			}
		});
		frame.add(text, BorderLayout.SOUTH);
		frame.setBounds(0, 0, 350, 200);
		frame.setLocationRelativeTo(null);
		frame.setAlwaysOnTop(true);
	}
	
	/**
	 * build a captcha image and a input text field to get captcha by human labor.
	 *
	 * @param imgUrl     image url.
	 * @param title      window title, can be null.
	 * @param httpAssist http client, can be null.
	 * @param reqHeaders request headers, can be null.
	 */
	public ImgRecognizer build(String imgUrl, String title,
	                           HttpClientAssist httpAssist, Map<String, String> reqHeaders)
			throws IOException, InterruptedException {
		
		Objects.requireNonNull(imgUrl, "image url can't be null");
		if (httpAssist == null)
			httpAssist = new HttpClientAssist(new HttpClientConfig());
		byte[] imgBuf;
		try (HttpClientAssist.UrlEntity imgUe = httpAssist.access(imgUrl, reqHeaders)) {
			imgBuf = imgUe.getEntityBuf();
		}
		return build(imgBuf, title);
	}
	
	/**
	 * build a captcha image and a input text field to get captcha by human labor.
	 *
	 * @param imgBase64Data like data:image/gif;base64,R0lGODlhJQAOAKUA...
	 * @param title         window title, can be null.
	 */
	public ImgRecognizer build(String imgBase64Data, String title) throws IOException {
		byte[] buf = Base64.getDecoder().decode(
				imgBase64Data.substring(imgBase64Data.indexOf(',') + 1));
		return build(buf, title);
	}
	
	/**
	 * build a captcha image and a input text field to get captcha by human labor.
	 *
	 * @param imgBuf img byte data
	 * @param title  window title, can be null.
	 */
	public ImgRecognizer build(byte[] imgBuf, String title) throws IOException {
		BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBuf));
		build(img, title);
		return this;
	}
	
	public ImgRecognizer build(BufferedImage img, String title) {
		CaptchaPanel imgPanel = new CaptchaPanel(img);
		frame.add(imgPanel, BorderLayout.CENTER);
		frame.setTitle(title != null ? title : "Input Captcha");
		frame.setVisible(true);
		frame.setAlwaysOnTop(false);
		return this;
	}
	
	/**
	 * get input text.
	 */
	public String getText() throws InterruptedException {
		textLatch.await();
		frame.dispose();
		return text.getText();
	}
	
	/**
	 * get input text.
	 */
	public String getText(long timeout, TimeUnit timeUnit) throws InterruptedException {
		textLatch.await(timeout, timeUnit);
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
