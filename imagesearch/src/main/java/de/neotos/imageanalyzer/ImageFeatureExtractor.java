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

import java.awt.Image;
import java.util.List;

/**
 * Every Algorithm which creates robust features from Images have to implement this interface. Then it could be easly integrated into the
 * ImageAnalyzerImplementation.
 * @author tschinke
 */
public interface ImageFeatureExtractor {

    /**
     * Analyzes the given image and extracts a set of features describing the image
     * @param img (not null)
     * @return a List of identified feautures
     */
    public List<ImageFeature> getFeatures(Image img);
}
