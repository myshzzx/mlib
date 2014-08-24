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

package de.neotos.imageanalyzer;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * The FeatureManager is the core for working with Features in case of retrieval.
 *
 * @author tschinke
 */
public interface ImageFeatureManager<UO> {
	/**
	 * 保存状态到流.
	 */
	void storeToStream(ObjectOutputStream out) throws IOException;


	/**
	 * Puts a feature to the underlying data-structure.
	 *
	 * @param features   must be comparable.
	 * @param userObject user-defined object. note that normally this is not used as a key for efficient
	 *                   retrieval (but may depending on implementation) but for distinguishing different feature sets.
	 *                   So if you put different feature sets associated with equal objects this is handeld as the same
	 *                   feature set.
	 *                   It is recommend that this user object identifies a feature set,
	 *                   which is intended to be the features from one image,
	 *                   e.g. the filename from the image or something similar.
	 */
	public void putFeatures(List<ImageFeature> features, UO userObject);


	/**
	 * Note that not all implementations of efficient algorithms can provide this
	 *
	 * @return the features removed (may be empty)
	 */
	public List<ImageFeature> removeFeatures(UO userObject);


	/**
	 * Note that not all implementations of efficient algorithms can provide this
	 *
	 * @return the features found (may be empty)
	 */
	public List<ImageFeature> getFeatures(UO userObject);

	/**
	 * Creates a list of ImageAnalyzerResults. The count
	 * in ImageAnalyzerResult is the amount of nearest neihgbour feature matches, differing between different user objects.
	 *
	 * @throws InterruptedException execute timeout.
	 */
	public List<ImageAnalyzerResult<UO>> findMatches(List<ImageFeature> features, int minMatchCount) throws InterruptedException;

	/**
	 * timeout for each feature search(micro-second).
	 */
	void setEachFeatureTimeout(long timeout);

	void dispose();
}
