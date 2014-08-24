package mysh.imagesearch;

import de.neotos.imageanalyzer.ImageFeatureManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.*;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Mysh
 * @since 14-1-20 下午4:18
 */
public class SiftHelperTestGUIStart {
	private static final Logger log = LoggerFactory.getLogger(SiftHelperTestGUIStart.class);
	private static SiftHelper.FeaMgrType mgrType = SiftHelper.FeaMgrType.KDTree;

	private SiftHelper<String> analyzer;
	private volatile ImgPreProc samplePreProc;
	private volatile ImgPreProc testPreProc;
	private volatile String sampleDir;
	private volatile File resultDir;

	private volatile boolean isDataPrepared = false;

	public static void main(String[] args) {
		UIUtil.useNimbusLookAndFeel();
		UIUtil.resetFont(null);
		new SiftHelperTestGUIStart();
	}

	public SiftHelperTestGUIStart() {
		JFrame f = new JFrame();
		try {
			SiftHelperTestGUI p = new SiftHelperTestGUI();
			resetOutput(p);
			restoreConfig(p);
			init(p);
			f.getContentPane().add(p);
			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			f.setBounds(0, 0, 700, 600);
			f.setLocationRelativeTo(null);
			f.setVisible(true);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(f, e.getMessage(), "error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}


	private void init(SiftHelperTestGUI p) throws Exception {
		analyzer = new SiftHelper<>(String.class, mgrType);
		analyzer.setMinMatchCount(2);

		p.injecter = new SiftHelperTestGUI.SiftHelperTestGUIInjecter() {
			@Override
			public void onPrepare(final SiftHelperTestGUI s) {
				prepareConfig(s);
				saveConfig(s);
			}

			@Override
			public void onSearch(final SiftHelperTestGUI s) {
				search(s);
			}
		};
	}

	private synchronized void prepareData() throws InterruptedException {

		if (!isDataPrepared) {
			ImageFeatureManager featureManager = SiftHelperTest.restoreFeaManager(mgrType);
			if (featureManager != null) {
				analyzer.setFeaturesManager(featureManager);
				isDataPrepared = true;
			} else if (new File(sampleDir).exists()) {

				final ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
				FileUtil.recurDir(new File(sampleDir), null, new FileUtil.FileTask() {
					@Override
					public void handle(final File f) {
						exec.submit(new Runnable() {
							@Override
							public void run() {
								try {
									analyzer.bindImage(f.getName(), ImageIO.read(f));
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
					}
				});
				exec.shutdown();
				exec.awaitTermination(1, TimeUnit.DAYS);
				SiftHelperTest.saveFeaManager(analyzer.getFeatureManager());
				isDataPrepared = true;
			} else
				isDataPrepared = false;

		}
	}

	private synchronized void prepareConfig(SiftHelperTestGUI s) {
		sampleDir = s.sampleDir.getText();
		resultDir = new File(s.resultDir.getText());
		samplePreProc = SiftHelperTest.genImgPreProc((int) s.samplePx.getValue());
		testPreProc = SiftHelperTest.genImgPreProc((int) s.targetPx.getValue());

		analyzer.setImgPreProc(samplePreProc);

		try {
			prepareData();
			SiftHelperTest.checkUserObjNum(analyzer);
			log.info("ready to search.");
		} catch (InterruptedException e) {
			log.error("prepareData error", e);
		}

		analyzer.getFeatureManager().setEachFeatureTimeout((int) s.eachFeatureTimeout.getValue());
	}

	private synchronized void search(SiftHelperTestGUI s) {
		String text = s.target.getText().trim();
		if (text.length() > 0) {
			SiftHelperTest.searchImg(analyzer, text, testPreProc, sampleDir, resultDir);
		}
	}

	private static final String CONFIG_SAVE = SiftHelperTestGUIStart.class.getSimpleName();

	private void saveConfig(SiftHelperTestGUI p) {
		File f = new File(CONFIG_SAVE);
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(CONFIG_SAVE))) {
			out.writeObject(p.sampleDir.getText());
			out.writeObject(p.resultDir.getText());
			out.writeObject(p.samplePx.getValue());
			out.writeObject(p.targetPx.getValue());
			out.writeObject(p.eachFeatureTimeout.getValue());
			out.writeObject(p.target.getText());
		} catch (Exception e) {

		}
	}

	private void restoreConfig(SiftHelperTestGUI p) {
		File f = new File(CONFIG_SAVE);
		if (f.exists() && f.isFile())
			try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(CONFIG_SAVE))) {
				p.sampleDir.setText(in.readObject().toString());
				p.resultDir.setText(in.readObject().toString());
				p.samplePx.setValue(in.readObject());
				p.targetPx.setValue(in.readObject());
				p.eachFeatureTimeout.setValue(in.readObject());
				p.target.setText(in.readObject().toString());
			} catch (Exception e) {

			}
	}

	private void resetOutput(final SiftHelperTestGUI p) throws IOException {
		PipedInputStream pipeIn = new PipedInputStream();
		final BufferedReader reader = new BufferedReader(new InputStreamReader((pipeIn)));
		PipedOutputStream pipeOut = new PipedOutputStream(pipeIn);
		PrintStream custPrint = new PrintStream(pipeOut);

		System.setOut(custPrint);

		Thread t = new Thread() {
			@Override
			public void run() {
				String line;
				while (true) {
					try {
						Thread.sleep(10);
						line = reader.readLine();
						p.output.setText(p.output.getText() + "\r\n" + line);
					} catch (Exception e) {
//						JOptionPane.showMessageDialog(p, e.getMessage(), "error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}
}

class UIUtil {
	private static final Logger log = LoggerFactory.getLogger(UIUtil.class);
	/**
	 * 默认字体.
	 */
	public static final Font DefaultFont = new Font("Microsoft YaHei", Font.PLAIN, 13);

	/**
	 * 使用 Nimbus 皮肤.
	 */
	public static void useNimbusLookAndFeel() {

		try {
			for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
						| UnsupportedLookAndFeelException ex) {
			log.error("use Nimbus L&F failed.", ex);
		}
	}

	/**
	 * 重置当前 UI L&F 的默认显示字体.<br/>
	 * 此方法必须放在 UI 组件创建之前, L&F 设置之后, 否则无效.
	 *
	 * @param font 给定字体. 为 null 则使用 13号雅黑.
	 */
	public static void resetFont(Font font) {

		if (font == null)
			font = DefaultFont;

		for (Map.Entry<Object, Object> e : UIManager.getLookAndFeelDefaults().entrySet()) {
			if (((e.getValue() instanceof FontUIResource))) {
				e.setValue(font);
			}
		}

		UIManager.getLookAndFeelDefaults().put("ArrowButton.font", font);
		UIManager.getLookAndFeelDefaults().put("Button.font", font);
		UIManager.getLookAndFeelDefaults().put("CheckBox.font", font);
		UIManager.getLookAndFeelDefaults().put("CheckBoxMenuItem.font", font);
		UIManager.getLookAndFeelDefaults().put("ColorChooser.font", font);
		UIManager.getLookAndFeelDefaults().put("ComboBox.font", font);
		UIManager.getLookAndFeelDefaults().put("DesktopIcon.font", font);
		UIManager.getLookAndFeelDefaults().put("DesktopPane.font", font);
		UIManager.getLookAndFeelDefaults().put("EditorPane.font", font);
		UIManager.getLookAndFeelDefaults().put("FileChooser.font", font);
		UIManager.getLookAndFeelDefaults().put("FormattedTextField.font", font);
		UIManager.getLookAndFeelDefaults().put("InternalFrame.font", font);
		UIManager.getLookAndFeelDefaults().put("InternalFrameTitlePane.font", font);
		UIManager.getLookAndFeelDefaults().put("Label.font", font);
		UIManager.getLookAndFeelDefaults().put("List.font", font);
		UIManager.getLookAndFeelDefaults().put("Menu.font", font);
		UIManager.getLookAndFeelDefaults().put("MenuBar.font", font);
		UIManager.getLookAndFeelDefaults().put("MenuItem.font", font);
		UIManager.getLookAndFeelDefaults().put("OptionPane.font", font);
		UIManager.getLookAndFeelDefaults().put("Panel.font", font);
		UIManager.getLookAndFeelDefaults().put("PasswordField.font", font);
		UIManager.getLookAndFeelDefaults().put("PopupMenu.font", font);
		UIManager.getLookAndFeelDefaults().put("PopupMenuSeparator.font", font);
		UIManager.getLookAndFeelDefaults().put("ProgressBar.font", font);
		UIManager.getLookAndFeelDefaults().put("RadioButton.font", font);
		UIManager.getLookAndFeelDefaults().put("RadioButtonMenuItem.font", font);
		UIManager.getLookAndFeelDefaults().put("RootPane.font", font);
		UIManager.getLookAndFeelDefaults().put("ScrollBar.font", font);
		UIManager.getLookAndFeelDefaults().put("ScrollBarThumb.font", font);
		UIManager.getLookAndFeelDefaults().put("ScrollBarTrack.font", font);
		UIManager.getLookAndFeelDefaults().put("ScrollPane.font", font);
		UIManager.getLookAndFeelDefaults().put("Separator.font", font);
		UIManager.getLookAndFeelDefaults().put("Slider.font", font);
		UIManager.getLookAndFeelDefaults().put("SliderThumb.font", font);
		UIManager.getLookAndFeelDefaults().put("SliderTrack.font", font);
		UIManager.getLookAndFeelDefaults().put("Spinner.font", font);
		UIManager.getLookAndFeelDefaults().put("SplitPane.font", font);
		UIManager.getLookAndFeelDefaults().put("TabbedPane.font", font);
		UIManager.getLookAndFeelDefaults().put("Table.font", font);
		UIManager.getLookAndFeelDefaults().put("TableHeader.font", font);
		UIManager.getLookAndFeelDefaults().put("TextArea.font", font);
		UIManager.getLookAndFeelDefaults().put("TextField.font", font);
		UIManager.getLookAndFeelDefaults().put("TextPane.font", font);
		UIManager.getLookAndFeelDefaults().put("TitledBorder.font", font);
		UIManager.getLookAndFeelDefaults().put("ToggleButton.font", font);
		UIManager.getLookAndFeelDefaults().put("ToolBar.font", font);
		UIManager.getLookAndFeelDefaults().put("ToolTip.font", font);
		UIManager.getLookAndFeelDefaults().put("Tree.font", font);
		UIManager.getLookAndFeelDefaults().put("Viewport.font", font);

	}

}
