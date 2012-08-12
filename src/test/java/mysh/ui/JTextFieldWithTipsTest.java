
package mysh.ui;

import java.awt.BorderLayout;
import java.awt.TextField;

import javax.swing.JFrame;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class JTextFieldWithTipsTest {

	public static void main(String[] args) {

		new JTextFieldWithTipsTest().test();
	}

	@Test
	public void test() {

		JFrame f = new JFrame("JTextFieldWithTipsTest");
		f.setLocationRelativeTo(null);
		f.setLayout(new BorderLayout());

		JTextFieldWithTips text = new JTextFieldWithTips();
		text.setLabel("tool tips");
		text.setFont(new java.awt.Font("Microsoft YaHei", 0, 14)); // NOI18N
		text.setText("init");
		text.setToolTipText("init");
		f.getContentPane().add(text, BorderLayout.NORTH);

		TextField text2 = new TextField();
		f.getContentPane().add(text2, BorderLayout.CENTER);

		f.setSize(420, 150);
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		text2.requestFocus();
		System.out.println("end");
	}

}
