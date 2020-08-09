package mysh.dev.video;

import mysh.collect.Colls;
import mysh.collect.Pair;
import mysh.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

/**
 * FFmpegUIController
 *
 * @author mysh
 * @since 2017/6/13
 */
class FFmpegUIController {
	private static final Logger log = LoggerFactory.getLogger(FFmpegUIController.class);
	
	private volatile Runnable stopAction;
	private volatile Process process;
	private Component parent;
	
	public FFmpegUIController(Component parent) {
		this.parent = parent;
	}
	
	void stop() {
		if (stopAction != null)
			stopAction.run();
	}
	
	void h265(String src, String target, String temp, String start, String to, int crf,
	          boolean overwrite, boolean hwAccel, boolean frameRate, int frameRateValue,
	          boolean copyAudio, boolean mono, boolean opusKps, int opusKpsValue,
	          Runnable taskStart, Runnable taskComplete) {
		ForkJoinPool.commonPool().execute(() -> {
			try {
				File tempDir = null;
				if (Strings.isNotBlank(temp)) {
					tempDir = new File(temp.trim());
					if (!tempDir.isDirectory()) {
						JOptionPane.showMessageDialog(parent, "临时目录不存在或不是目录");
						return;
					}
				}
				Pair<List<File>, File> filePair = parseFiles(src, target, overwrite);
				if (filePair == null)
					return;
				
				List<File> srcFiles = filePair.getL();
				File targetFile = filePair.getR(), realTarget = targetFile;
				boolean isTargetDir = targetFile.isDirectory();
				if (srcFiles.size() > 1 && !isTargetDir) {
					JOptionPane.showMessageDialog(parent, "多源时目标必须是目录");
					return;
				}
				
				FFmpegs f = new FFmpegs();
				if (overwrite)
					f.overwrite();
				if (hwAccel)
					f.hardwareAccel();
				if (frameRate)
					f.frameRate(frameRateValue);
				if (!copyAudio) {
					f.audioOpus();
					if (mono)
						f.audioChannels(1);
					if (opusKps) {
						f.audioKiloBitRate(opusKpsValue);
						if (opusKpsValue < 96)
							f.audioOpusVoip();
					}
				}
				
				f.from(start).to(to).videoH265Params(crf);
				
				stopAction = renewStopAction();
				
				taskStart.run();
				for (File srcFile : srcFiles) {
					if (isTargetDir)
						realTarget = new File(targetFile,
								FilesUtil.getFileNameWithoutExtension(srcFile) + ".mkv");
					if (!overwrite)
						realTarget = FilesUtil.getWritableFile(realTarget);
					
					f.input(srcFile);
					
					double cpuUsage = Oss.getCpuUsageSystem();
					File tempFile = null;
					if (tempDir != null) {
						tempFile = new File(tempDir, System.nanoTime() + "-" + realTarget.getName());
						process = f.output(tempFile).go();
					} else
						process = f.output(realTarget).go();
					
					if ((!hwAccel || !FFmpegs.hasNvSupport()) && Oss.isWindows()) {
						try {
							WinAPI.getAllWinProcesses(true).stream()
							      .filter(p -> Objects.equals(f.getCmd(), p.getCmdLine()))
							      .forEach(p -> {
								      try {
									      Oss.changePriority(p.getPid(), Oss.OsProcPriority.VeryLow);
									
									      int availableProcessors = Runtime.getRuntime().availableProcessors();
									      Random rnd = new Random();
									
									      char[] maskChars = new char[availableProcessors];
									      Arrays.fill(maskChars, '0');
									      for (int i = 0; i < availableProcessors; i++) {
										      if (rnd.nextDouble() > cpuUsage)
											      maskChars[i] = '1';
									      }
									      long mask = Long.parseLong(new String(maskChars), 2);
									      WinAPI.setProcessAffinityMask(p.getPid(), Math.max(1, mask));
								      } catch (Exception e) {
									      log.info("change process resource fail: " + p.getCmdLine(), e);
								      }
							      });
						} catch (Exception e) {
							log.error("fetch-all-process-fail", e);
						}
					}
					process.waitFor();
					
					if (tempFile != null) {
						Files.move(tempFile.toPath(), realTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
					
					Thread.sleep(4000);
				}
			} catch (InterruptedException e) {
				log.info("task terminated");
			} catch (Exception e) {
				log.error("h265 error", e);
			} finally {
				taskComplete.run();
				stopAction = null;
			}
		});
	}
	
	void split(String src, String target, String temp, String start, String to,
	           boolean overwrite, boolean hwAccel,
	           Runnable taskStart, Runnable taskComplete) {
		ForkJoinPool.commonPool().execute(() -> {
			try {
				Pair<List<File>, File> filePair = parseFiles(src, temp, overwrite);
				if (filePair == null)
					filePair = parseFiles(src, target, overwrite);
				if (filePair == null)
					return;
				
				List<File> srcFiles = filePair.getL();
				File targetFile = filePair.getR(), realTarget = targetFile;
				boolean isTargetDir = targetFile.isDirectory();
				if (srcFiles.size() > 1 && !isTargetDir) {
					JOptionPane.showMessageDialog(parent, "多源时目标必须是目录");
					return;
				}
				
				FFmpegs f = new FFmpegs();
				if (overwrite)
					f.overwrite();
				if (hwAccel)
					f.hardwareAccel();
				f.from(start).to(to);
				
				stopAction = renewStopAction();
				
				taskStart.run();
				for (File srcFile : srcFiles) {
					if (isTargetDir)
						realTarget = new File(targetFile, srcFile.getName());
					if (!overwrite)
						realTarget = FilesUtil.getWritableFile(realTarget);
					
					process = f.input(srcFile).output(realTarget).go();
					process.waitFor();
				}
			} catch (InterruptedException e) {
				log.info("task terminated");
			} catch (Exception e) {
				log.error("split error", e);
			} finally {
				taskComplete.run();
				stopAction = null;
			}
		});
	}
	
	void merge(String src, String target,
	           boolean overwrite, boolean hwAccel,
	           Runnable taskStart, Runnable taskComplete) {
		ForkJoinPool.commonPool().execute(() -> {
			try {
				Pair<List<File>, File> filePair = parseFiles(src, target, overwrite);
				if (filePair == null)
					return;
				
				List<File> srcFiles = filePair.getL();
				File targetFile = filePair.getR();
				if (targetFile.exists() && !targetFile.isFile()) {
					JOptionPane.showMessageDialog(parent, "目标必须是文件");
					return;
				}
				if (srcFiles.size() < 2) {
					JOptionPane.showMessageDialog(parent, "只有一个源, 无须合并");
					return;
				}
				if (!overwrite && targetFile.exists())
					targetFile = FilesUtil.getWritableFile(targetFile);
				
				FFmpegs f = new FFmpegs();
				if (overwrite)
					f.overwrite();
				if (hwAccel)
					f.hardwareAccel();
				
				stopAction = renewStopAction();
				
				taskStart.run();
				process = f.merge(srcFiles).output(targetFile).go();
				process.waitFor();
			} catch (InterruptedException e) {
				log.info("task terminated");
			} catch (Exception e) {
				log.error("merge error", e);
			} finally {
				taskComplete.run();
				stopAction = null;
			}
		});
	}
	
	void subtitle(String src, String target, boolean overwrite, Runnable taskStart, Runnable taskComplete) {
		ForkJoinPool.commonPool().execute(() -> {
			try {
				Pair<List<File>, File> filePair = parseFiles(src, target, overwrite);
				if (filePair == null)
					filePair = parseFiles(src, target, overwrite);
				if (filePair == null)
					return;
				
				List<File> srcFiles = filePair.getL();
				if (srcFiles.size() != 2) {
					JOptionPane.showMessageDialog(parent, "请给定1个视频及其字幕文件");
					return;
				}
				File video = null, subtitle = null;
				Set<String> subExt = Colls.ofHashSet("srt", "ass", "sub", "sst");
				for (File srcFile : srcFiles) {
					if (subExt.contains(FilesUtil.getFileExtension(srcFile)))
						subtitle = srcFile;
					else
						video = srcFile;
				}
				if (video == null || subtitle == null) {
					JOptionPane.showMessageDialog(parent, "未识别的字幕格式");
					return;
				}
				File targetFile = filePair.getR(), realTarget = targetFile;
				if (targetFile.isDirectory())
					realTarget = new File(targetFile,
							FilesUtil.getFileNameWithoutExtension(video) + ".mkv");
				if (!overwrite)
					realTarget = FilesUtil.getWritableFile(realTarget);
				
				stopAction = renewStopAction();
				taskStart.run();
				FFmpegs.addSubtitle(video, subtitle, overwrite, realTarget).waitFor();
			} catch (InterruptedException e) {
				log.info("task terminated");
			} catch (Exception e) {
				log.error("subtitle error", e);
			} finally {
				taskComplete.run();
				stopAction = null;
			}
		});
	}
	
	private Runnable renewStopAction() {
		if (stopAction != null) {
			JOptionPane.showMessageDialog(parent, "异常,请重试");
			throw new RuntimeException();
		}
		
		Thread currentThread = Thread.currentThread();
		return () -> {
			Process p = this.process;
			currentThread.interrupt();
			
			if (p != null && p.isAlive()) {
				p.destroyForcibly();
			}
		};
	}
	
	private Pair<List<File>, File> parseFiles(String src, String target, boolean overwrite) {
		List<File> srcFiles = new ArrayList<>();
		
		if (Strings.isBlank(src) || Strings.isBlank(target))
			return null;
		
		String[] srcArr = src.trim().split("[\r\n]+");
		File targetFile = new File(target.trim());
		
		for (String s : srcArr) {
			File file = new File(s.trim());
			if (!file.exists() || !file.isFile()) {
				JOptionPane.showMessageDialog(parent, "源不存在或不是文件: " + s);
				return null;
			} else {
				srcFiles.add(file);
			}
		}
		return Pair.of(srcFiles, targetFile);
	}
}
