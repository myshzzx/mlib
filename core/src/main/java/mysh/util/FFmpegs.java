package mysh.util;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import lombok.Data;
import lombok.experimental.Accessors;
import mysh.collect.Colls;
import mysh.os.Oss;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Ffmpegs
 *
 * @author mysh
 * @since 2017/2/25
 */
@NotThreadSafe
public class FFmpegs {
	private static final Logger log = LoggerFactory.getLogger(FFmpegs.class);
	
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
	private String output = "";
	
	private MetaInfo srcMetaInfo;
	
	public FFmpegs overwrite() {
		cmd = null;
		overwrite = " -y";
		return this;
	}
	
	public FFmpegs hardwareAccel() {
		cmd = null;
		hardwareAccelerate = " -hwaccel auto";
		return this;
	}
	
	public FFmpegs input(File file) {
		cmd = null;
		srcMetaInfo = null;
		input = " -i \"" + file.getAbsolutePath() + "\"";
		return this;
	}
	
	private static final Joiner colonJoiner = Joiner.on(":");
	
	/**
	 * set start time, expression can be separate using: [\s,.:/\\]+
	 *
	 * @param fromTime time exp, can be like -> 3:37 2/34/11 03.21
	 */
	public FFmpegs from(String fromTime) {
		cmd = null;
		if (Strings.isNotBlank(fromTime)) {
			ss = colonJoiner.join(fromTime.trim().split("[\\s,.:/\\\\]+"));
		} else {
			ss = null;
		}
		return this;
	}
	
	/**
	 * set to time, expression can be separate using: [\s,.:/\\]+
	 * negative time can be used: -3.20 indicate 3:20 to the end.
	 *
	 * @param toTime time exp, can be like -> 3:37 2/34/11 03.21
	 */
	public FFmpegs to(String toTime) {
		cmd = null;
		if (Strings.isNotBlank(toTime)) {
			to = colonJoiner.join(toTime.trim().split("[\\s,.:/\\\\]+"));
		} else {
			to = null;
		}
		return this;
	}
	
	/**
	 * http://ffmpeg.org/ffmpeg-codecs.html#libx265
	 * ffmpeg  -encoders
	 * ffmpeg -h encoder=hevc_nvenc
	 */
	public FFmpegs videoH265Crf(int crf) {
		cmd = null;
		if (Strings.isNotBlank(hardwareAccelerate)) {
			switch (Oss.getGPU()) {
				case nVidia:
					videoOptions = " -c:v hevc_nvenc -qp " + crf;
					break;
				case AMD:
					videoOptions = " -c:v hevc_amf -qp_p " + crf + " -qp_i " + crf;
					break;
			}
		} else
			videoOptions = " -c:v libx265 -x265-params crf=" + crf;
		return this;
	}
	
	public FFmpegs videoH265Kbps(int kbps) {
		if (Strings.isNotBlank(hardwareAccelerate)) {
			switch (Oss.getGPU()) {
				case nVidia:
					videoOptions = " -c:v hevc_nvenc -b:v " + kbps + "k";
					break;
				case AMD:
					videoOptions = " -c:v hevc_amf -b:v " + kbps + "k";
					break;
			}
		} else
			videoOptions = " -c:v libx265 -b:v " + kbps + "k";
		return this;
	}
	
	public FFmpegs videoH265CompressPercent(int percent) {
		cmd = null;
		MetaInfo mi = getMetaInfo();
		int kbps = mi.getVideoKBitrate() * percent / 100;
		return videoH265Kbps(kbps);
	}
	
	public FFmpegs frameRate(int rate) {
		cmd = null;
		videoFrameRate = " -r " + rate;
		return this;
	}
	
	/**
	 * usable range ≥ 32Kbps. Recommended range ≥ 64Kbps
	 */
	public FFmpegs audioOpus() {
		cmd = null;
		audioOptions = " -c:a libopus";
		return this;
	}
	
	/**
	 * http://ffmpeg.org/ffmpeg-codecs.html#libopus-1
	 */
	public FFmpegs audioOpusVoip() {
		cmd = null;
		audioOptions = " -c:a libopus -application voip";
		return this;
	}
	
	/**
	 * usable range ≥ 96Kbps. Recommended range -aq 4 (≥ 128Kbps)
	 */
	public FFmpegs audioVorbis() {
		cmd = null;
		audioOptions = " -c:a libvorbis";
		return this;
	}
	
	/**
	 * usable range ≥ 32Kbps (depending on profile and audio). Recommended range ≥ 128Kbps
	 */
	public FFmpegs audioAac() {
		cmd = null;
		audioOptions = " -c:a aac";
		return this;
	}
	
	public FFmpegs audioKiloBitRate(int kbps) {
		cmd = null;
		audioBitRate = " -b:a " + kbps + "k";
		return this;
	}
	
	public FFmpegs audioChannels(int channelCount) {
		cmd = null;
		audioChannels = " -ac " + channelCount;
		return this;
	}
	
	public FFmpegs output(File file) {
		cmd = null;
		output = " \"" + file.getAbsolutePath() + "\"";
		return this;
	}
	
	/**
	 * return immediately, not wait the process terminate
	 */
	public Process go() throws IOException {
		String cmd = getCmd();
		log.info("execute: " + cmd);
		return Oss.executeCmd(cmd, true);
	}
	
	private String cmd;
	
	public String getCmd() {
		if (cmd == null) {
			String realTo = to;
			if (Strings.isNotBlank(to) && to.startsWith("-")) {
				int sec = getMetaInfo().getDuration();
				if (sec > 0) {
					String[] tt = to.substring(1).split(":");
					for (int i = 0; i < tt.length; i++) {
						sec -= Integer.parseInt(tt[tt.length - 1 - i]) * Math.pow(60, i);
					}
					realTo = String.format("%d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
				}
			}
			
			cmd = "ffmpeg"
					+ overwrite + hardwareAccelerate
					+ input
					+ (Strings.isNotBlank(ss) ? " -ss " + ss : "")
					+ (Strings.isNotBlank(realTo) ? " -to " + realTo : "")
					+ streams
					+ videoOptions
					+ videoFrameRate
					+ audioOptions
					+ audioBitRate + audioChannels
					+ output;
		}
		
		return cmd;
	}
	
	public FFmpegs merge(List<File> files) {
		cmd = null;
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
	
	public static Process addSubtitle(File video, File subtitle, boolean overwrite, File target) throws IOException {
		String cmd = String.format("ffmpeg -i \"%s\" -i \"%s\" -map 0 -map 1:v -map 1:a -c copy %s \"%s\"",
				subtitle.getAbsolutePath(), video.getAbsolutePath(), overwrite ? "-y" : "", target.getAbsolutePath());
		log.info("execute: " + cmd);
		return Oss.executeCmd(cmd, true);
	}
	
	@Data
	@Accessors(chain = true)
	public static class MetaInfo {
		List<Map<String, String>> infos = new ArrayList<>();
		
		private String query(Map<String, String> req, String param) {
			INFO:
			for (Map<String, String> info : infos) {
				for (Map.Entry<String, String> e : req.entrySet()) {
					if (!Objects.equals(info.get(e.getKey()), e.getValue()))
						continue INFO;
				}
				String value = info.get(param);
				if ("N/A".equals(value))
					continue;
				return value;
			}
			return null;
		}
		
		/**
		 * media duration in seconds
		 */
		public int getDuration() {
			String d = query(Colls.ofHashMap("_type", "[FORMAT]"), "duration");
			if (d == null) {
				d = query(Colls.ofHashMap("_type", "[STREAM]"), "duration");
			}
			return d != null ? Double.valueOf(d).intValue() : 0;
		}
		
		public int getVideoKBitrate() {
			String br = query(Colls.ofHashMap("_type", "[STREAM]", "codec_type", "video"), "bit_rate");
			if (br == null) {
				br = query(Colls.ofHashMap("_type", "[FORMAT]"), "bit_rate");
			}
			return br != null ? Integer.parseInt(br) / 1000 : 0;
		}
	}
	
	private MetaInfo getMetaInfo() {
		if (srcMetaInfo == null) {
			String output = new String(Oss.readFromProcess("ffprobe" + input + " -show_format -show_streams"));
			srcMetaInfo = new MetaInfo();
			
			Map<String, String> t = new HashMap<>();
			for (String kv : output.split("[\\r\\n]+")) {
				if (kv.contains("=")) {
					String[] kvs = kv.split("=", 2);
					t.put(kvs[0], kvs[1]);
				} else if (kv.startsWith("[/"))
					srcMetaInfo.infos.add(t);
				else if (kv.startsWith("[")) {
					t = new HashMap<>();
					t.put("_type", kv);
				}
			}
		}
		
		return srcMetaInfo;
	}
}
