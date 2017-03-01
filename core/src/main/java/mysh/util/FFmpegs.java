package mysh.util;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;

/**
 * Ffmpegs
 *
 * @author mysh
 * @since 2017/2/25
 */
@NotThreadSafe
public class FFmpegs {
	public static void h265(boolean accelerate, File file, int h265Crf, int audioChannel, File dst) throws IOException, InterruptedException {

		String cmd = String.format("ffmpeg -y %s -i \"%s\" -c:v libx265 -x265-params crf=%d -ac %d \"%s\"",
						accelerate ? "-hwaccel auto" : "",
						file.getAbsolutePath(),
						h265Crf, audioChannel,
						dst.getAbsolutePath()
		);
		System.out.println("execute: " + cmd);
		OSs.executeCmd(cmd, true).waitFor();
	}

	private String overwrite = "";
	private String hardwareAccelerate = "";
	private String input = "";
	private String videoOptions = " -c:v copy";
	private String audioOptions = " -c:a copy";
	private String audioChannels = "";
	private String audioBitRate = "";
	private String output = "";

	private FFmpegs() {
	}

	public static FFmpegs create() {
		return new FFmpegs();
	}

	public FFmpegs overwrite() {
		overwrite = " -y";
		return this;
	}

	public FFmpegs hardwareAccel() {
		hardwareAccelerate = " -hwaccel auto";
		return this;
	}

	public FFmpegs input(File file) {
		input = " -i \"" + file.getAbsolutePath() + "\"";
		return this;
	}

	/**
	 * http://ffmpeg.org/ffmpeg-codecs.html#libx265
	 */
	public FFmpegs videoH265Params(int crf) {
		videoOptions = " -c:v libx265 -x265-params crf=" + crf;
		return this;
	}

	public FFmpegs audioOpus() {
		audioOptions = " -c:a libopus";
		return this;
	}

	/**
	 * http://ffmpeg.org/ffmpeg-codecs.html#libopus-1
	 */
	public FFmpegs audioOpusVoip() {
		audioOptions = " -c:a libopus -application voip";
		return this;
	}

	public FFmpegs audioKiloBitRate(int kbps) {
		audioBitRate = " -b:a " + kbps + "k";
		return this;
	}

	public FFmpegs audioChannels(int channelCount) {
		audioChannels = " -ac " + channelCount;
		return this;
	}

	public FFmpegs output(File file) {
		output = " \"" + file.getAbsolutePath() + "\"";
		return this;
	}

	public void go() throws IOException, InterruptedException {
		String cmd = "ffmpeg"
						+ overwrite + hardwareAccelerate
						+ input
						+ videoOptions
						+ audioOptions
						+ audioBitRate + audioChannels
						+ output;
		System.out.println("execute: " + cmd);
		OSs.executeCmd(cmd, true).waitFor();
	}
}
