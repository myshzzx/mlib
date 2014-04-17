
package mysh.dev.encoding;

import mysh.util.EncodingUtil;
import mysh.util.FileUtil;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.File;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author Allen
 */
public class UIController {

	private Encoding ui;

	private static final String TRANS_STRING = "转换";

	private static final String CANCEL_STRING = "取消";

	/**
	 * 是否取消转换.
	 */
	private volatile boolean isCancelTrans = false;

	UIController(Encoding ui) {

		this.ui = ui;
	}

	/**
	 * 点击 "转换".
	 */
	void onTrans() {

		if (TRANS_STRING.equals(this.ui.trans.getText())) {
			this.isCancelTrans = false;
			this.ui.trans.setText(CANCEL_STRING);
			this.setStatusBar("转换中...");

			TransType type = null;
			switch (this.ui.transType.getSelectedIndex()) {
				case 0: // gbk to utf8(without BOM)
					type = TransType.GBK2UTF8WithoutBOM;
					break;
				case 1: // utf8 to gbk
					type = TransType.UTF82GBK;
					break;
			}

			final TransType transType = type;
			new SwingWorker<Boolean, Object>() {

				/**
				 * 转换操作. 操作成功返回 true.
				 *
				 * @see javax.swing.SwingWorker#doInBackground()
				 */
				@Override
				protected Boolean doInBackground() throws Exception {

					Objects.requireNonNull(transType, "未知转换类型.");

					File sourceDir = new File(ui.source.getText().trim());
					if (!sourceDir.canRead()) {
						throw new RuntimeException("源不正确或不可读.");
					}

					Pattern fileNamePattern = Pattern.compile(ui.fileNamePattern.getText().trim());

					File desDir = new File(ui.destination.getText().trim());
					if (!desDir.exists()) {
						desDir.mkdirs();
					}
					if (!desDir.isDirectory() || !desDir.canRead()) {
						throw new RuntimeException("目标不是目录或不可读.");
					}

					return trans(sourceDir, fileNamePattern, ui.isConsiderSubDirs.isSelected(), desDir,
									transType);
				}

				protected void done() {

					boolean complete = false;
					try {
						complete = this.get();
					} catch (Exception e) {
						setStatusBar("转换失败. " + e.getMessage());
						return;
					} finally {
						isCancelTrans = true;
						ui.trans.setText(TRANS_STRING);
					}

					if (complete) {
						resetStatusBar();
					} else {
						setStatusBar("转换中断.");
					}
				}

				;

			}.execute();
		} else {
			this.isCancelTrans = true;
			this.ui.trans.setText(TRANS_STRING);
		}
	}

	/**
	 * 转换.
	 *
	 * @param srcDir            源 文件或目录
	 * @param fileNamePattern   源文件名匹配
	 * @param isConsiderSubDirs 是否遍历子目录
	 * @param desDir            目标文件夹
	 * @param transType         转换类型
	 * @return 是否完成转换.
	 * @throws Exception
	 */
	private boolean trans(File srcDir, Pattern fileNamePattern, boolean isConsiderSubDirs, File desDir,
	                      TransType transType) throws Exception {

		if (this.isCancelTrans) {
			return false;
		}

		File desFile = new File(desDir.getAbsolutePath() + "/" + srcDir.getName());
		if (srcDir.isFile() && srcDir.canRead()
						&& fileNamePattern.matcher(srcDir.getName().toLowerCase()).matches()) {
			byte[] fileByteArray = FileUtil.readFileToByteArray(srcDir.getAbsolutePath(), 100_000_000);

			if (transType == TransType.UTF82GBK) {

				if (!EncodingUtil.isUTF8Bytes(fileByteArray)) {
					if (this.ui.autoRecognize.isSelected()
									|| JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(
									this.ui,
									srcDir.getAbsolutePath()
													+ "\n不是 UTF-8 编码, 仍然要把它当作 UTF-8 编码来转换吗?\n选 否 则直接复制源文件",
									Encoding.TITLE, JOptionPane.YES_NO_OPTION
					)) {
						FileUtils.copyFile(srcDir, desFile);
						return true;
					}
				}

				fileByteArray = getUTF8WithoutBOM(fileByteArray);
			} else if (transType == TransType.GBK2UTF8WithoutBOM) {
				if (EncodingUtil.isUTF8Bytes(fileByteArray)) {
					if (this.ui.autoRecognize.isSelected()
									|| JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(
									this.ui,
									srcDir.getAbsolutePath()
													+ "\n符合 UTF-8 编码, 仍然要把它当作 GBK 编码来转换吗?\n选 否 则直接复制源文件(不带BOM)",
									Encoding.TITLE, JOptionPane.YES_NO_OPTION
					)) {
						fileByteArray = getUTF8WithoutBOM(fileByteArray);
						FileUtil.writeFile(desFile.getAbsolutePath(), fileByteArray);
						return true;
					}
				}

			}
			FileUtils.write(desFile, new String(fileByteArray, transType.getSrcEncoding()),
							transType.getDesEncoding(), false);
		} else if (srcDir.isDirectory() && srcDir.canRead()) {
			for (File childFile : srcDir.listFiles()) {
				if (!isConsiderSubDirs && childFile.isDirectory())
					continue;

				if (!this.trans(childFile, fileNamePattern, isConsiderSubDirs, desFile, transType)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * 取不带BOM的UTF8字节数组.
	 *
	 * @param fileByteArray UTF8字节数组.
	 * @return
	 */
	private byte[] getUTF8WithoutBOM(byte[] fileByteArray) {

		if (fileByteArray.length >= 3 && fileByteArray[0] == (byte) 0xEF && fileByteArray[1] == (byte) 0xBB
						&& fileByteArray[2] == (byte) 0xBF) {
			byte[] utf8ByteArrayWithoutBOM = new byte[fileByteArray.length - 3];
			System.arraycopy(fileByteArray, 3, utf8ByteArrayWithoutBOM, 0, utf8ByteArrayWithoutBOM.length);
			fileByteArray = utf8ByteArrayWithoutBOM;
		}
		return fileByteArray;
	}

	void setStatusBar(String status) {

		if (status != null)
			this.ui.statusBar.setText(status);
	}

	void resetStatusBar() {

		this.ui.statusBar.setText("就绪.");
	}
}
