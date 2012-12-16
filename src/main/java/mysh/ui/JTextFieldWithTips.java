
package mysh.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Objects;

import javax.swing.JTextField;

/**
 * 在文本区内部带灰色提示标签的文本输入框.
 * 
 * @author Allen
 */
public final class JTextFieldWithTips extends JTextField {

	private static final long serialVersionUID = 2071121893110220885L;

	/**
	 * 标签.
	 */
	private String label = "";

	/**
	 * 当前是否展示标签.
	 */
	private boolean isShowLabel = false;

	/**
	 * 默认字体.
	 */
	private Font defaultFont = new Font(null, Font.PLAIN, 13);

	/**
	 * 标签字体.
	 */
	private Font labelFont = new Font(null, Font.ITALIC, 13);

	/**
	 * 默认颜色.
	 */
	private Color defaultForeground = Color.BLACK;

	/**
	 * 标签颜色.
	 */
	private final Color labelForeground = Color.GRAY;

	public JTextFieldWithTips() {

		super();
		this.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {

				checkToDisplayLabel();
			}

			@Override
			public void focusGained(FocusEvent e) {

				checkToDisplayLabel();
			}
		});
	}

	/**
	 * 检查是否展示标签.
	 */
	private void checkToDisplayLabel() {

		if (this.label == null || this.label.length() == 0) {
			return;
		}

		if (this.isFocusOwner()) {
			if (this.isShowLabel) {
				this.isShowLabel = false;
				super.setText("");
				super.setFont(this.defaultFont);
				super.setForeground(this.defaultForeground);
			}
		} else {
			if (this.getText().length() == 0) {
				this.isShowLabel = true;
				super.setText(this.label);
				super.setFont(this.labelFont);
				super.setForeground(this.labelForeground);
			} else if (this.isShowLabel) {
				this.isShowLabel = false;
				super.setFont(this.defaultFont);
				super.setForeground(this.defaultForeground);
			}
		}
	}

	@Override
	public void setFont(Font f) {

		Objects.requireNonNull(f);

		this.defaultFont = f;
		this.labelFont = new Font(f.getName(), Font.ITALIC, f.getSize());

		if (this.isShowLabel) {
			super.setFont(this.labelFont);
		} else {
			super.setFont(this.defaultFont);
		}
	}

	@Override
	public void setForeground(Color fg) {

		Objects.requireNonNull(fg);

		this.defaultForeground = fg;

		if (!this.isShowLabel) {
			super.setForeground(this.defaultForeground);
		}
	}

	@Override
	public String getText() {

		if (this.isShowLabel)
			return "";
		else
			return super.getText();
	}

	@Override
	public void setText(String t) {

		super.setText(t == null ? "" : t);
		this.checkToDisplayLabel();
	}

	/**
	 * 取提示标签.
	 * 
	 * @return
	 */
	public String getLabel() {

		return label;
	}

	/**
	 * 设置提示标签.
	 * 
	 * @param label
	 * @return
	 */
	public JTextFieldWithTips setLabel(String label) {

		this.label = label == null ? "" : label;
		this.setToolTipText(label);
		this.checkToDisplayLabel();
		return this;
	}
}
