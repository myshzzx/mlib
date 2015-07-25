package mysh.captcha;

import com.jhlabs.image.RippleFilter;
import com.jhlabs.image.TransformFilter;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Captcha {
	private int width;
	private int height;
	private Random r = new Random();
	private int textFontSize = 30;
	private List<Font> textFonts = new ArrayList<>();
	private Color textFontColor = Color.black;
	private Color backgroundColorFrom = Color.LIGHT_GRAY;
	private Color backgroundColorTo = Color.white;
	private Color noiseColor = Color.black;
	private int textCharLengthMin = 5;
	private char[] textCharString = "eghkmpqwyBEFGHKPQRS".toCharArray();
	private int charSpace = -4;
	private boolean useNoise = false;

	public Captcha(int width, int height) {
		this.width = width;
		this.height = height;
		addTextFont("Times New Roman");
	}

	public BufferedImage createImage(String text) {
		BufferedImage bi = renderWord(text);
		if (useNoise)
			makeNoise(bi, .1f, .1f, .9f, .9f);
		bi = getDistortedImage(bi);
		bi = addBackground(bi);
		return bi;
	}

	public String createText(int addRandom) {
		int length;
		if (addRandom > 0)
			length = textCharLengthMin + r.nextInt(addRandom);
		else
			length = textCharLengthMin;
		char[] text = new char[length];

		for (int i = 0; i < length; i++) {
			text[i] = textCharString[r.nextInt(textCharString.length)];
		}

		return new String(text);
	}

	BufferedImage renderWord(String text) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2D = image.createGraphics();
		g2D.setColor(textFontColor);

		RenderingHints hints = new RenderingHints(
						RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
		hints.add(new RenderingHints(RenderingHints.KEY_RENDERING,
						RenderingHints.VALUE_RENDER_QUALITY));
		g2D.setRenderingHints(hints);

		FontRenderContext frc = g2D.getFontRenderContext();

		int x = r.nextInt(textFontSize / 2);
		int y = textFontSize;
		for (int i = 0; i < text.length(); i++) {
			Font chosenFont = textFonts.get(r.nextInt(textFonts.size()));
			char[] charToDraw = new char[]{text.charAt(i)};
			g2D.setFont(chosenFont);

			y += r.nextInt(textFontSize / 7) * (r.nextInt(2) == 0 ? -1 : 1);
			g2D.drawChars(charToDraw, 0, charToDraw.length, x, y);

			GlyphVector gv = chosenFont.createGlyphVector(frc, charToDraw);
			x += (int) gv.getVisualBounds().getWidth() + charSpace;
		}

		return image;
	}

	BufferedImage getDistortedImage(BufferedImage baseImage) {
		RippleFilter rippleFilter = new RippleFilter();
		rippleFilter.setWaveType(RippleFilter.SINE);
		rippleFilter.setXAmplitude(1f + r.nextFloat() * 2);
		rippleFilter.setYAmplitude(4f + r.nextFloat() * 3);
		rippleFilter.setXWavelength(4);
		rippleFilter.setYWavelength((9f + r.nextFloat() * 10) * (r.nextInt(2) == 0 ? -1 : 1));
		rippleFilter.setEdgeAction(TransformFilter.ZERO);

		BufferedImage effectImage = rippleFilter.filter(baseImage, null);

		BufferedImage distortedImage = new BufferedImage(baseImage.getWidth(),
				baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics graphics = distortedImage.getGraphics();
		graphics.drawImage(effectImage, 0, 0, null, null);
		graphics.dispose();

		return distortedImage;
	}

	void makeNoise(BufferedImage image, float factorOne,
	               float factorTwo, float factorThree, float factorFour) {
		Color color = noiseColor;

		// image size
		int width = image.getWidth();
		int height = image.getHeight();

		// the points where the line changes the stroke and direction
		Point2D[] pts;

		// the curve from where the points are taken
		CubicCurve2D cc = new CubicCurve2D.Float(width * factorOne, height
						* r.nextFloat(), width * factorTwo, height
						* r.nextFloat(), width * factorThree, height
						* r.nextFloat(), width * factorFour, height
						* r.nextFloat());

		// creates an iterator to define the boundary of the flattened curve
		PathIterator pi = cc.getPathIterator(null, 2);
		Point2D tmp[] = new Point2D[200];
		int i = 0;

		// while pi is iterating the curve, adds points to tmp array
		while (!pi.isDone()) {
			float[] coords = new float[6];
			switch (pi.currentSegment(coords)) {
				case PathIterator.SEG_MOVETO:
				case PathIterator.SEG_LINETO:
					tmp[i] = new Point2D.Float(coords[0], coords[1]);
			}
			i++;
			pi.next();
		}

		pts = new Point2D[i];
		System.arraycopy(tmp, 0, pts, 0, i);

		Graphics2D graph = (Graphics2D) image.getGraphics();
		graph.setRenderingHints(new RenderingHints(
						RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON));

		graph.setColor(color);

		// for the maximum 3 point change the stroke and direction
		for (i = 0; i < pts.length - 1; i++) {
			if (i < 3)
				graph.setStroke(new BasicStroke(0.9f * (4 - i)));
			graph.drawLine((int) pts[i].getX(), (int) pts[i].getY(),
							(int) pts[i + 1].getX(), (int) pts[i + 1].getY());
		}

		graph.dispose();
	}

	BufferedImage addBackground(BufferedImage baseImage) {
		Color colorFrom = backgroundColorFrom;
		Color colorTo = backgroundColorTo;

		int width = baseImage.getWidth();
		int height = baseImage.getHeight();

		// create an opaque image
		BufferedImage imageWithBackground = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		Graphics2D graph = (Graphics2D) imageWithBackground.getGraphics();
		RenderingHints hints = new RenderingHints(
						RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_OFF);

		hints.add(new RenderingHints(RenderingHints.KEY_COLOR_RENDERING,
						RenderingHints.VALUE_COLOR_RENDER_QUALITY));
		hints.add(new RenderingHints(RenderingHints.KEY_ALPHA_INTERPOLATION,
						RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY));

		hints.add(new RenderingHints(RenderingHints.KEY_RENDERING,
						RenderingHints.VALUE_RENDER_QUALITY));

		graph.setRenderingHints(hints);

		GradientPaint paint = new GradientPaint(0, 0, colorFrom, width, height, colorTo);
		graph.setPaint(paint);
		graph.fill(new Rectangle2D.Double(0, 0, width, height));

		// draw the transparent image over the background
		graph.drawImage(baseImage, 0, 0, null);

		return imageWithBackground;
	}

	// get set =====================================

	public Captcha setBackgroundColorFrom(Color backgroundColorFrom) {
		this.backgroundColorFrom = backgroundColorFrom;
		return this;
	}

	public Captcha setBackgroundColorTo(Color backgroundColorTo) {
		this.backgroundColorTo = backgroundColorTo;
		return this;
	}

	public Captcha setNoiseColor(Color noiseColor) {
		this.noiseColor = noiseColor;
		return this;
	}

	public Captcha setTextCharSet(String textCharSet) {
		this.textCharString = textCharSet.toCharArray();
		return this;
	}

	public Captcha setTextCharLengthMin(int textCharLengthMin) {
		this.textCharLengthMin = textCharLengthMin;
		return this;
	}

	public Captcha setTextFontColor(Color textFontColor) {
		this.textFontColor = textFontColor;
		return this;
	}

	public final Captcha addTextFont(String textFont) {
		this.textFonts.add(new Font(textFont, Font.BOLD, textFontSize));
		return this;
	}

	public Captcha clearTextFonts() {
		this.textFonts.clear();
		return this;
	}

	public Captcha setTextFontSize(int textFontSize) {
		this.textFontSize = textFontSize;
		return this;
	}

	public Captcha setCharSpace(int charSpace) {
		this.charSpace = charSpace;
		return this;
	}

	public Captcha setUseNoise(boolean useNoise) {
		this.useNoise = useNoise;
		return this;
	}
}
