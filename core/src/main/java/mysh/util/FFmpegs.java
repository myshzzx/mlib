package mysh.util;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import mysh.collect.Colls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Ffmpegs
 *
 * @author mysh
 * @since 2017/2/25
 */
@NotThreadSafe
public class FFmpegs {
	private static final Logger log = LoggerFactory.getLogger(FFmpegs.class);

	public static void h265(boolean accelerate, File file, int h265Crf, int audioChannel, File dst) throws IOException, InterruptedException {

		String cmd = String.format("ffmpeg -y %s -i \"%s\" -c:v libx265 -x265-params crf=%d -ac %d \"%s\"",
				accelerate ? "-hwaccel auto" : "",
				file.getAbsolutePath(),
				h265Crf, audioChannel,
				dst.getAbsolutePath()
		);
		System.out.println("execute: " + cmd);
		Oss.executeCmd(cmd, true).waitFor();
	}

	private String overwrite = "";
	private String hardwareAccelerate = "";
	private String input = "";
	private String streams = " -map 0:v -map 0:a -map 0:s? -c:s copy";
	private String videoOptions = " -c:v copy";
	private String videoFrameRate = "";
	private String audioOptions = " -c:a copy";
	private String audioChannels = "";
	private String audioBitRate = "";
	private String ss, to;
	private String threads = "";
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

	public FFmpegs merge(List<File> files) {
		if (Colls.isEmpty(files))
			throw new RuntimeException("merge files can't be blank");
		try {
			File fileList = File.createTempFile("ffmpeg-merge", ".txt");
			fileList.deleteOnExit();
			try (BufferedWriter out = Files.newWriter(fileList, Charset.defaultCharset())) {
				for (File file : files) {
					out.write("file '");
					out.write(file.getAbsolutePath());
					out.write("'");
					out.newLine();
				}
			}
			input = " -f concat -safe 0 -i \"" + fileList.getAbsolutePath() + "\"";
		} catch (IOException e) {
			throw new RuntimeException("write list file error.", e);
		}
		return this;
	}

	private static final Joiner colonJoiner = Joiner.on(":");

	/**
	 * set start time, expression can be separate using: [\s,.:/\\]+
	 *
	 * @param fromTime time exp, can be like -> 3:37 2/34/11 03.21
	 */
	public FFmpegs from(String fromTime) {
		if (Strings.isNotBlank(fromTime)) {
			ss = colonJoiner.join(fromTime.trim().split("[\\s,.:/\\\\]+"));
		} else {
			ss = null;
		}
		return this;
	}

	/**
	 * set to time, expression can be separate using: [\s,.:/\\]+
	 *
	 * @param toTime time exp, can be like -> 3:37 2/34/11 03.21
	 */
	public FFmpegs to(String toTime) {
		if (Strings.isNotBlank(toTime)) {
			to = colonJoiner.join(toTime.trim().split("[\\s,.:/\\\\]+"));
		} else {
			to = null;
		}
		return this;
	}

	/**
	 * http://ffmpeg.org/ffmpeg-codecs.html#libx265
	 */
	public FFmpegs videoH265Params(int crf) {
		videoOptions = " -c:v libx265 -x265-params crf=" + crf;
		return this;
	}

	public FFmpegs frameRate(int rate) {
		videoFrameRate = " -r " + rate;
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

	@Deprecated
	public FFmpegs threads(int t) {
		if (t < 1)
			throw new IllegalArgumentException("threads should be positive");
		//		threads = " -threads " + t;
		return this;
	}

	public FFmpegs output(File file) {
		output = " \"" + file.getAbsolutePath() + "\"";
		return this;
	}

	/**
	 * return immediately, not wait the process terminate
	 */
	public Process go() throws IOException, InterruptedException {
		String cmd = getCmd();
		log.info("execute: " + cmd);
		return Oss.executeCmd(cmd, true);
	}

	public String getCmd() {
		return "ffmpeg"
				+ overwrite + hardwareAccelerate
				+ input
				+ (Strings.isNotBlank(ss) ? " -ss " + ss : "")
				+ (Strings.isNotBlank(to) ? " -to " + to : "")
				+ streams
				+ videoOptions
				+ videoFrameRate
				+ audioOptions
				+ audioBitRate + audioChannels
				+ threads
				+ output;
	}
}
