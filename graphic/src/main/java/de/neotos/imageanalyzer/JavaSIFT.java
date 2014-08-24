package de.neotos.imageanalyzer;


/**
 * The following Text is taken from Java_SIFT.java written by Stephan Saalfeld:
 * ==============================================================================
 *
 * Align a stack consecutively using automatically extracted robust landmark
 * correspondences.
 *
 * The plugin uses the Scale Invariant Feature Transform (SIFT) by David Lowe
 * \cite{Lowe04} and the Random Sample Consensus (RANSAC) by Fishler and Bolles
 * \citet{FischlerB81} to identify landmark correspondences.
 *
 * It identifies a rigid transformation for the second of two slices that maps
 * the correspondences of the second optimally to those of the first.
 *
 * BibTeX:
 * <pre>
 * &#64;article{Lowe04,
 *   author    = {David G. Lowe},
 *   title     = {Distinctive Image Features from Scale-Invariant Keypoints},
 *   journal   = {International Journal of Computer Vision},
 *   year      = {2004},
 *   volume    = {60},
 *   number    = {2},
 *   pages     = {91--110},
 * }
 * &#64;article{FischlerB81,
 *	 author    = {Martin A. Fischler and Robert C. Bolles},
 *   title     = {Random sample consensus: a paradigm for model fitting with applications to image analysis and automated cartography},
 *   journal   = {Communications of the ACM},
 *   volume    = {24},
 *   number    = {6},
 *   year      = {1981},
 *   pages     = {381--395},
 *   publisher = {ACM Press},
 *   address   = {New York, NY, USA},
 *   issn      = {0001-0782},
 *   doi       = {http://doi.acm.org/10.1145/358669.358692},
 * }
 * </pre>
 *
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
 * @version 0.1
 *
 *
 *
 */


import mpi.cbg.fly.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.PixelGrabber;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * The JavaSIFT class is a wrapper Class around the JavaSIFT implementation written by Stephan Saalfeld.
 * Its not very fast but plain Java and indenpendent from other libraries (you even don't need ImageJ).
 *
 * @author tschinke
 */
public class JavaSIFT implements ImageFeatureExtractor {
	private static final Logger log = LoggerFactory.getLogger(JavaSIFT.class);

	// steps
	private int steps = 3;
	// initial sigma
	private float initial_sigma = 1.6f;
	// background colour
	private double bg = 0.0;
	// feature descriptor size
	private int fdsize = 4;
	// feature descriptor orientation bins
	private int fdbins = 8;
	// size restrictions for scale octaves, use octaves < max_size and > min_size only
	private int min_size = 64;
	private int max_size = 1024;


	/**
	 * Set true to double the size of the image by linear interpolation to
	 * ( with * 2 + 1 ) * ( height * 2 + 1 ).  Thus we can start identifying
	 * DoG extrema with $\sigma = INITIAL_SIGMA / 2$ like proposed by
	 * \citet{Lowe04}.
	 * <p/>
	 * This is useful for images scmaller than 1000px per side only.
	 */
	private boolean upscale = true;


	private static float normTo1(int b) {
		return (float) (b / 255.0f);
	}

	private static int RGB2Grey(int argb) {
		// int a = (argb >> 24) & 0xff;
		int r = (argb >> 16) & 0xff;
		int g = (argb >> 8) & 0xff;
		int b = (argb) & 0xff;

		//int rgb=(0xff000000 | ((r<<16)&0xff0000) | ((g<<8)&0xff00) | (b&0xff));
		int y = (int) Math.round(0.299f * r + 0.587f * g + 0.114f * b);
		return y;
	}

	private FloatArray2D convert(java.awt.Image img) {

		FloatArray2D image;
		PixelGrabber grabber = new PixelGrabber(img, 0, 0, -1, -1, true);
		try {
			grabber.grabPixels();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		int[] data = (int[]) grabber.getPixels();

		image = new FloatArray2D(grabber.getWidth(), grabber.getHeight());
		for (int d = 0; d < data.length; d++)
			image.data[d] = normTo1(RGB2Grey(data[d]));
		return image;
	}

	private List<ImageFeature> convert(List<Feature> features) {
		List<ImageFeature> res = new ArrayList<ImageFeature>();
		for (Feature f : features) {
			ImageFeature imageFeature = new ImageFeature();
			imageFeature.setDescriptor((f.descriptor));
			imageFeature.setOrientation(f.orientation);
			imageFeature.setScale(f.scale);
			res.add(imageFeature);
		}
		return res;
	}

	private float[] convert(float[] desc) {
		for (int i = 0; i < desc.length; i++) {
			int int_val = (int) (512 * desc[i]);
			int_val = Math.min(255, int_val);
			desc[i] = int_val;
		}
		return desc;
	}

	public List<ImageFeature> getFeatures(java.awt.Image img) {
		List<Feature> fs;

		FloatArray2DSIFT sift = new FloatArray2DSIFT(fdsize, fdbins);
		FloatArray2D fa = convert(img);
		Filter.enhance(fa, 1.0f);

		if (upscale) {
			FloatArray2D fat = new FloatArray2D(fa.width * 2 - 1, fa.height * 2 - 1);
			FloatArray2DScaleOctave.upsample(fa, fat);
			fa = fat;
			fa = Filter.computeGaussianFastMirror(fa, (float) Math.sqrt(initial_sigma * initial_sigma - 1.0));
		} else
			fa = Filter.computeGaussianFastMirror(fa, (float) Math.sqrt(initial_sigma * initial_sigma - 0.25));

		long start_time = System.currentTimeMillis();
		sift.init(fa, steps, initial_sigma, min_size, max_size);
		fs = sift.run(max_size);
		Collections.sort(fs);
		log.debug("took " + (System.currentTimeMillis() - start_time) + "ms, "
						+ fs.size() + " features identified and processed");

		return convert(fs);
	}


}
