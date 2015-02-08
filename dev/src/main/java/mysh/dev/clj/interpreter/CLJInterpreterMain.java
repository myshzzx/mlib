/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mysh.dev.clj.interpreter;

import mysh.util.HotKeyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * @author Allen
 */
public class CLJInterpreterMain extends JPanel {
	private static final Logger log = LoggerFactory.getLogger(CLJInterpreterMain.class);

	private static final PipedInputStream inputIn = new PipedInputStream();
	private static PrintWriter inputOut;
	private static final PipedInputStream resultIn = new PipedInputStream();
	private static PipedOutputStream resultOut;
	private static final PipedInputStream errResultIn = new PipedInputStream();
	private static PipedOutputStream rerrResultOut;
	/**
	 * 命令提交历史.
	 */
	private List<String> commitHistory = new LinkedList<String>();
	/**
	 * 命令提交历史索引号.
	 */
	private volatile int commitHistoryPosition = -1;

	private static AtomicBoolean changeSysStandardOutFlag = new AtomicBoolean(false);
	private static InputStream oriIn = System.in;
	private static PrintStream oriOut = System.out;
	private static PrintStream oriErr = System.err;
	private final Thread showThread;
	private final Thread showErrThread;

	private static void changeSysStandardOut() {
		if (changeSysStandardOutFlag.compareAndSet(false, true)) {
			try {
				System.out.println(CLJInterpreterMain.class.toString() + " 将接管控制台输出.");

				inputOut = new PrintWriter(new PipedOutputStream(inputIn));
				resultOut = new PipedOutputStream(resultIn);
				rerrResultOut = new PipedOutputStream(errResultIn);

				System.setIn(inputIn);
				System.setOut(new PrintStream(resultOut));
				System.setErr(new PrintStream(rerrResultOut));
			} catch (IOException ex) {
				System.setIn(oriIn);
				System.setOut(oriOut);
				System.setErr(oriErr);
				log.error(ex.getMessage(), ex);
			}
		}
	}

	private final Thread cljThread;

	/**
	 * Creates new form CLJInterpreterMain
	 */
	public CLJInterpreterMain(JFrame outerFrame) {

		this.outerFrame = outerFrame;
		initComponents();

		// 输出展示线程
		showThread = new Thread("sysout-reader") {
			Pattern prompt = Pattern.compile("=> ");
			Scanner scanner = new Scanner(resultIn).useDelimiter(prompt);

			@Override
			public void run() {

				while (true) {
					try {
						String next = scanner.next();
						SwingUtilities.invokeAndWait(() -> {
							result.text.append(next + prompt);
							result.text.setCaretPosition(result.text.getDocument().getLength());
						});
					} catch (Throwable t) {
						oriErr.println(CLJInterpreterMain.class + " 输出展示线程 error:" + t);
					}
				}
			}
		};
		showThread.setDaemon(true);

		// 错误输出线程
		showErrThread = new Thread("syserr-reader") {
			Scanner scanner = new Scanner(errResultIn);

			@Override
			public void run() {
				while (true) {
					try {
						String nextError = scanner.nextLine();
						SwingUtilities.invokeAndWait(() -> input.text.append(nextError + "\n"));
					} catch (Throwable t) {
						oriErr.println(CLJInterpreterMain.class + "错误输出线程 error:" + t);
					}
				}
			}
		};
		showErrThread.setDaemon(true);

		// 清空结果热键.
		HotKeyUtil.registerHotKey(KeyEvent.VK_F1, 0, new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				result.text.setText("");
			}
		});


		//复位热键.
		HotKeyUtil.registerHotKey(KeyEvent.VK_ESCAPE, 0, new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				input.text.setText("");
				input.text.requestFocus();
			}
		});


		// 提交热键.
		HotKeyUtil.registerHotKey(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK, new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				String text = input.text.getText().trim();
				if (text.length() == 0)
					return;

				if (commitHistory.size() < 1
								|| !commitHistory.get(commitHistory.size() - 1).equals(text)) {
					commitHistory.add(text);
					commitHistoryPosition = commitHistory.size();
				}

				input.text.setText("");
				if (text.length() > 0) {
					result.text.append(text + "\n");
					inputOut.println(text);
					inputOut.flush();
				}
			}
		});


		// 历史记录
		this.input.text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent evt) {

				int historyLength = commitHistory.size();
				if ((evt.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) == 0
								|| historyLength < 1) {
					return;
				}

				switch (evt.getKeyCode()) {
					case KeyEvent.VK_LEFT:
						commitHistoryPosition--;
						break;
					case KeyEvent.VK_RIGHT:
						commitHistoryPosition++;
						break;
					default:
						return;
				}
				if (commitHistoryPosition >= historyLength) {
					commitHistoryPosition = historyLength - 1;
				}
				if (commitHistoryPosition < 0) {
					commitHistoryPosition = 0;
				}

				input.text.setText(commitHistory.get(commitHistoryPosition));

			}
		});

		// clojure 解析线程.
		cljThread = new Thread("cljMain") {
			@Override
			public void run() {
				clojure.main.main(new String[]{});
			}
		};
		cljThread.setDaemon(true);
	}

	void startCljThread() {
		changeSysStandardOut();

		showThread.start();
		showErrThread.start();
		cljThread.start();
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		jSplitPane1 = new JSplitPane();
		result = new mysh.dev.clj.interpreter.CLJTextPanel();
		input = new mysh.dev.clj.interpreter.CLJTextPanel();
		onTopCheck = new JCheckBox();

		setPreferredSize(new java.awt.Dimension(670, 559));
		setLayout(new java.awt.BorderLayout());

		jSplitPane1.setDividerLocation(400);
		jSplitPane1.setOrientation(JSplitPane.VERTICAL_SPLIT);

		result.setAutoscrolls(true);
		result.setFont(new java.awt.Font("Microsoft YaHei", 0, 12)); // NOI18N
		jSplitPane1.setTopComponent(result);

		input.setAutoscrolls(true);
		input.setFont(new java.awt.Font("Microsoft YaHei", 0, 12)); // NOI18N
		jSplitPane1.setRightComponent(input);

		add(jSplitPane1, java.awt.BorderLayout.CENTER);

		onTopCheck.setFont(new java.awt.Font("Microsoft YaHei", 0, 12)); // NOI18N
		onTopCheck.setText("置顶");
		onTopCheck.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				onTopCheckStateChanged(evt);
			}
		});
		add(onTopCheck, java.awt.BorderLayout.SOUTH);
	}// </editor-fold>//GEN-END:initComponents

	private void onTopCheckStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_onTopCheckStateChanged
		this.outerFrame.setAlwaysOnTop(this.onTopCheck.isSelected());
	}//GEN-LAST:event_onTopCheckStateChanged

	private JFrame outerFrame;

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private mysh.dev.clj.interpreter.CLJTextPanel input;
	private JSplitPane jSplitPane1;
	private JCheckBox onTopCheck;
	private mysh.dev.clj.interpreter.CLJTextPanel result;
	// End of variables declaration//GEN-END:variables
}
