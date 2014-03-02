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
 * @author Torben Schinke <schinke[|@|]neotos.de> 
 */

package de.neotos.imageanalyzer.utils;

import de.neotos.imageanalyzer.ImageFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.PixelGrabber;
import java.io.*;
import java.util.List;

/**
 * If an ImageAnalyzer uses the FeatureCache, it is able to avoid unnessary featurecalculations. The result is an immanent speed up
 * of initial FeatureManager setup using same pictures.
 * The List of ImageFeatures are serialized using the standard Java ObjectInput/OutputStreams
 *
 * @author tschinke
 */
public class FeatureCache {
	private static final Logger log = LoggerFactory.getLogger(FeatureCache.class);
	private File dir;

	public FeatureCache(File dir) {
		this.dir = dir;
		if ((dir == null) || (!dir.isDirectory()))
			throw new IllegalArgumentException("Must be a Directory: " + dir);
	}

	public void storeFeatures(Image key, List<ImageFeature> features) {
		saveAsBin(new File(dir, getHash(key)), features);
	}

	/**
	 * @param key
	 * @return null if key not exists
	 */
	public List<ImageFeature> loadFeatures(Image key) {
		if (!keyExists(key))
			return null;
		else
			return (List<ImageFeature>) loadFromBin(new File(dir, getHash(key)));
	}

	public boolean keyExists(Image key) {
		File f = new File(dir, getHash(key));
		return f.exists();
	}

	private String getHash(Image img) {
		return Hash.getSHA1(getImageData(img));
	}

	private int[] getImageData(Image img) {

		PixelGrabber grabber = new PixelGrabber(img, 0, 0, -1, -1, true);
		try {
			grabber.grabPixels();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		int[] data = (int[]) grabber.getPixels();
		return data;
	}


	private boolean saveAsBin(File filename, Object o) {
		ObjectOutputStream objOut = null;
		try {
			objOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
			objOut.writeObject(o);
			objOut.close();
			log.debug("saved to bin-cache:" + filename);
			return true;
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			throw new RuntimeException(ex);
		} finally {
			try {
				objOut.close();
			} catch (IOException ex) {
				log.error(ex.getMessage(), ex);
				throw new RuntimeException(ex);
			}
		}
	}

	private Object loadFromBin(File filename) {
		ObjectInputStream objIn = null;
		if (!filename.exists())
			return null;
		try {
			objIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename)));
			Object o = objIn.readObject();
			objIn.close();
			log.debug("loaded from bin-cache:" + filename);

			return o;
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			throw new RuntimeException(ex);
		} finally {
			try {
				objIn.close();
			} catch (IOException ex) {
				log.error(ex.getMessage(), ex);
				throw new RuntimeException(ex);
			}
		}
	}

	/**
	 * removes all cached features
	 */
	public void clear() {
		for (File f : dir.listFiles())
			f.delete();
	}


}
