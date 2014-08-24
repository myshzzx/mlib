/*
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * NOTE:
 * The SIFT-method is protected by U.S. Patent 6,711,293: "Method and
 * apparatus for identifying scale invariant features in an image and use of
 * same for locating an object in an image" by the University of British
 * Columbia.  That is, for commercial applications the permission of the author
 * is required.
 *
 * @author Torben Schinke <schinke[|@|]neotos.de> 
 */
package de.neotos.imageanalyzer;


import de.neotos.imageanalyzer.utils.FeatureCache;
import de.neotos.imageanalyzer.utils.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This is the implementation of the ImageAnalyzer Interface. It can use various Feature Manager and various Feature Extractors.
 * Default it will use KDTreeManager and JavaSIFT Extractor.
 * This wonderful implementation can be driven in two wunderful modes: harden and unharden.
 * Harden means that no exception will be thrown but if possible false will be returned. Perhaps
 * the harden-mode is what you want to have in a productive enviroment. (Default it is unharden, so exceptions will be thrown for better debugging
 * purposes)
 *
 * @author tschinke
 */
@Deprecated
public class ImageAnalyzerImplementation implements ImageAnalyzer {
	private static final Logger log = LoggerFactory.getLogger(ImageAnalyzerImplementation.class);
	/**
	 * Value would be true or false. If true no exception will be thrown and
	 * the analyzer will do its best.
	 */
	public final static String OSA_HARDEN = "harden";


	/**
	 * Tell the analyzer that he has to use a cache. If an image he knows
	 * should be analyzed he will take it instead from the cache.
	 */
	public final static String OSA_USE_CACHE = "useCache";

	/**
	 * Instead of using the default-cache-dir (in the current home-dir)
	 * use this directory
	 */
	public final static String OSA_CACHE_DIR = "cacheDir";

	/**
	 * Overwrites the default-minimal amount of matches which are then interpreted as relevant
	 */
	public final static String OSA_MIN_MATCH_COUNT = "minMatchCount";

	/**
	 * Rekursivly adds all pictures from this directory and analyze them
	 */
	public final static String OSA_LOAD_PICTURES_FROM = "pictureImport";


	/**
	 * Will load addional pictures, seperated by systems path-seperator
	 */
	public final static String OSA_LOAD_PICTURE_LIST = "pictureList";

	private File cacheDir;
	private boolean useCache;
	private int minMatchCount;

	private boolean harden;

	private ImageFeatureManager featureManager;
	private ImageFeatureExtractor featureExtractor;
	private FeatureCache featureCache;

	/**
	 * Initializes a fully runnable Analyzer with default settings.<br/>
	 * Defaultsettings are:
	 * <ul>
	 * <li>harden=false (throw exceptions when possible)</li>
	 * <li>caching=true</li>
	 * <li>minMatchCount=5</li>
	 * </ul>
	 */
	public ImageAnalyzerImplementation() {
		initDefaults();
	}


	public ImageAnalyzerImplementation(ImageFeatureManager manager, ImageFeatureExtractor extractor) {
		initDefaults();
		this.featureManager = manager;
		this.featureExtractor = extractor;
	}

	/**
	 * Value would be true or false. If true no exception will be thrown and
	 * the analyzer will do its best.
	 */
	public void isHarden(boolean b) {
		this.harden = b;
	}

	/**
	 * Tell the analyzer that he has to use a cache. If an image he knows
	 * should be analyzed he will take it instead from the cache.
	 */
	public void useCache(boolean b) {

		this.useCache = b;

	}


	/**
	 * Instead of using the default-cache-dir (in the current home-dir)
	 * use this directory. Using this function will set useCache automatically to true
	 */
	public void setCacheDir(File dir) {
		this.useCache(true);
		cacheDir = dir;
		featureCache = new FeatureCache(dir);

	}

	/**
	 * Overwrites the default-minimal amount of matches which are then interpreted as relevant
	 */
	public void setMinMatchCount(int minCount) {
		this.minMatchCount = minCount;
	}


	/**
	 * Rekursivly adds all pictures from this directory and analyze them
	 * Loads all currently supported imageformats by java (if you need other formats you have to register them before)
	 * It will use the complete Filenames as user-objects
	 *
	 * @return if harden then it will returns false if any picture has failed else true
	 */
	public boolean bindImages(File dir) {
		if ((dir == null) && (!harden))
			throw new RuntimeException("Directory must not be null");
		else if ((dir == null) && (harden))
			return false;
		if ((!dir.exists()) && (!harden))
			throw new RuntimeException("The given Directory is not existent: " + dir.getAbsolutePath());
		else if ((!dir.exists()) && (harden)) {
			try {
				dir.mkdirs();
			} catch (Throwable e) {
				log.warn("Could not create directories", e);
				return false;
			}
		}

		final List<String> extentions = Arrays.asList(ImageIO.getReaderFileSuffixes());
		File[] images = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				String ext = IO.getFileExt(pathname.getName());
				if (ext.length() < 1)
					return false;
				else
					return extentions.contains(ext.substring(1));
			}
		});
		return bindImages(Arrays.asList(images));

	}


	/**
	 * Load a list of existing files to the kd-tree. It will use
	 * the complete Filenames as user-objects
	 *
	 * @return if harden then it will returns false if any picture has failed else true
	 */
	public boolean bindImages(List<File> files) {
		boolean allOk = true;
		if ((files == null) && (!harden))
			throw new RuntimeException("File must not be null");
		else if ((files == null) && (harden))
			return false;

		for (File f : files) {
			if ((!f.exists()) && (!harden))
				throw new RuntimeException("The given File is not existent: " + f.getAbsolutePath());
			else if ((!f.exists()) && (harden)) {
				log.warn("The given File is not existent: " + f.getAbsolutePath());
				allOk = false;
				continue;
			}

			if ((f.isDirectory()) && (!harden))
				throw new RuntimeException("The given File is a directory, that is not allowed: " + f.getAbsolutePath());
			if ((f.isDirectory()) && (harden)) {
				log.warn("The given File is a directory, that is not allowed: " + f.getAbsolutePath());
				allOk = false;
				continue;
			}

			try {
				Image img = ImageIO.read(f);
				boolean bs = this.bindImage(img, f.getAbsolutePath());
				if (!bs)
					allOk = false;
			} catch (Throwable e) {
				allOk = false;
				if (!harden) {
					log.error("Could not load and bind image:" + f.getAbsolutePath(), e);
					throw new RuntimeException(e);
				} else {
					log.warn("Could not load and bind image:" + f.getAbsolutePath(), e);
					continue;
				}
			}

		}
		return allOk;


	}


	private void clearFeatureManager() {
		featureManager = null;
	}


	private void initDefaults() {
		setCacheDir(IO.getUserProgramHome(".ImageAnalzer_FeatureCache"));
		minMatchCount = 5;
		harden = false;
		featureManager = getFeatureManager();
		featureExtractor = getFeatureExtractor();

	}

	/**
	 * Please look at the constants OSA_<...> to get informations about possible parameters.
	 *
	 * @return if harden then it will returns false if picture has failed else true
	 */
	public boolean setup(Properties p) {
		boolean allOk = true;
		try {
			if (p.getProperty(OSA_HARDEN) != null)
				harden = Boolean.parseBoolean(p.getProperty(OSA_HARDEN));

			if (p.getProperty(OSA_CACHE_DIR) != null)
				setCacheDir(new File(p.getProperty(OSA_CACHE_DIR)));

			if (p.getProperty(OSA_LOAD_PICTURES_FROM) != null)
				if (!bindImages(new File(p.getProperty(OSA_LOAD_PICTURES_FROM))))
					allOk = false;

			if (p.getProperty(OSA_LOAD_PICTURE_LIST) != null) {
				String[] files = p.getProperty(OSA_LOAD_PICTURE_LIST).split(Pattern.quote(File.pathSeparator));
				List<File> ffiles = new ArrayList<File>();
				for (String fs : files)
					ffiles.add(new File(fs));
				if (!bindImages(ffiles))
					allOk = false;
			}

			if (p.getProperty(OSA_MIN_MATCH_COUNT) != null)
				setMinMatchCount(Integer.parseInt(p.getProperty(OSA_MIN_MATCH_COUNT)));


		} catch (Throwable e) {
			if (harden) {
				allOk = false;
				log.warn("Could not execute or parse all setupoptions", e);

			} else {
				log.error("Could not execute or parse all setupoptions", e);
				throw new RuntimeException("Could not execute or parse all setupoptions");
			}
		}
		return allOk;
	}

	private List<ImageFeature> getFeatures(Image image) {
		List<ImageFeature> features;
		if ((useCache) && (featureCache.keyExists(image)))
			features = featureCache.loadFeatures(image);
		else {
			features = getFeatureExtractor().getFeatures(image);
			if (useCache) {
				featureCache.storeFeatures(image, features);
			}
		}

		return features;
	}

	/**
	 * removes all ImageFeatures from the underlying cache
	 */
	public void clearCache() {
		featureCache.clear();
	}

	/**
	 * @param image
	 * @param o
	 * @return if harden then it will returns false if picture has failed else true
	 */
	public boolean bindImage(Image image, Object o) {
		try {
			List<ImageFeature> features = getFeatures(image);
			getFeatureManager().putFeatures(features, o);
		} catch (Throwable e) {
			if (!harden) {
				log.error("Not able to bind given image", e);
				throw new RuntimeException(e);
			} else {
				log.warn("Not able to bind given image", e);
				return false;
			}
		}
		return true;
	}

	/**
	 * @param f
	 * @param o
	 * @return if harden then it will returns false if picture has failed else true
	 */
	public boolean bindImage(File f, Object o) {
		try {
			if (f == null)
				throw new RuntimeException("given file cannot be null");
			if (!f.exists())
				throw new RuntimeException("given file doesn't exist");
			return bindImage(ImageIO.read(f), o);
		} catch (Exception e) {
			if (!harden) {
				log.error("Not able to bind given image", e);
				throw new RuntimeException(e);
			} else {
				log.warn("Not able to bind given image", e);
				return false;
			}
		}
	}


	public boolean bindImage(byte[] image, Object o) {
		try {
			InputStream bar = new ByteArrayInputStream(image);
			Image img = ImageIO.read(bar);
			return bindImage(img, o);
		} catch (Throwable ex) {
			if (!harden) {
				log.error("Not able to load image from byte-array", ex);
				throw new RuntimeException(ex);
			} else {
				log.warn("Not able to load image from byte-array", ex);
				return false;
			}

		}
	}

	/**
	 * @param image
	 * @return if harden then it will returns false if picture has failed else true
	 */
	public boolean unbindImage(Image image) {
		try {
			List<ImageFeature> features = getFeatures(image);
			getFeatureManager().removeFeatures(features);
		} catch (Throwable e) {
			if (!harden) {
				log.error("Not able to unbind given image", e);
				throw new RuntimeException(e);
			} else {
				log.warn("Not able to unbind given image", e);
				return false;
			}
		}
		return true;
	}


	public boolean unbindImage(File f) {
		try {
			if (f == null)
				throw new RuntimeException("given file cannot be null");
			if (!f.exists())
				throw new RuntimeException("given file doesn't exist");
			return unbindImage(ImageIO.read(f));
		} catch (Exception e) {
			if (!harden) {
				log.error("Not able to unbind given image", e);
				throw new RuntimeException(e);
			} else {
				log.warn("Not able to unbind given image", e);
				return false;
			}
		}
	}

	/**
	 * @param image
	 * @return if harden then it will returns false if picture has failed else true
	 */
	public List<ImageAnalyzerResult> findImage(Image image) {

		try {
			List<ImageFeature> features = getFeatures(image);
			long start = System.currentTimeMillis();
			log.debug("Begin to search similiar Images...");
			List<ImageAnalyzerResult> res = getFeatureManager().findMatches(features, this.minMatchCount);
			log.debug("... took " + (System.currentTimeMillis() - start) + " ms");

			highPassFilter(res);
			if (res.size() > 0)
				log.debug("Best match is " + res.get(0).userObj + " with " + res.get(0).matches + " matches");
			else
				log.debug("No similiar image was found.");

			return res;
		} catch (Throwable e) {
			if (!harden) {
				log.error("Not able to unbind given image", e);
				throw new RuntimeException(e);
			} else {
				log.warn("Not able to unbind given image", e);
				return new ArrayList<ImageAnalyzerResult>();
			}
		}

	}


	public List<ImageAnalyzerResult> findImage(byte[] image) {
		try {
			Image img = ImageIO.read(new ByteArrayInputStream(image));
			List<ImageAnalyzerResult> res = findImage(img);

			return res;
		} catch (Throwable e) {
			if (!harden) {
				log.error("Not able to find given image", e);
				throw new RuntimeException(e);
			} else {
				log.warn("Not able to find given image", e);
				return new ArrayList<ImageAnalyzerResult>();
			}
		}
	}


	public List<ImageAnalyzerResult> findImage(File f) {
		try {
			if (f == null)
				throw new RuntimeException("given file cannot be null");
			if (!f.exists())
				throw new RuntimeException("given file doesn't exist");
			return findImage(ImageIO.read(f));
		} catch (Throwable e) {
			if (!harden) {
				log.error("Not able to load given image", e);
				throw new RuntimeException(e);
			} else {
				log.warn("Not able to load given image", e);
				return new ArrayList<ImageAnalyzerResult>();
			}
		}
	}

	/**
	 * Due to a linear-search in the underlying kd-tree this function is not implemented yet.
	 * If you want this to be efficient, you can perhaps use an internal hashmap where
	 * the references of oject are maped to a list of keypoints in the kdtree
	 *
	 * @param o user-object for an image
	 * @return today always runtimeexceptions
	 */
	public boolean unbindImage(Object o) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * @return treeholder dependent of singelton or not
	 */
	private ImageFeatureManager getFeatureManager() {
		if (featureManager == null)
			featureManager = new KDTreeManager(128);
		return featureManager;
	}


	private ImageFeatureExtractor getFeatureExtractor() {
		if (featureExtractor == null)
			featureExtractor = new JavaSIFT();
		return featureExtractor;
	}


	private void highPassFilter(List<ImageAnalyzerResult> r) {
		log.debug("Results before HighPassFilter " + r.size());
		Iterator<ImageAnalyzerResult> it = r.iterator();
		while (it.hasNext()) {
			ImageAnalyzerResult iar = it.next();

			if (iar.matches < minMatchCount) {
				it.remove();
				log.debug("HighPassFilter removes " + iar.toString());
			}
		}
		Collections.sort(r);
		Collections.reverse(r);
		log.debug("Results after HighPassFilter " + r.size());

	}

}
