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

import java.io.Serializable;


/**
 * This class capsules all different ImageFeature formats from different ImageFeatureExtractors. So when adding new Extractortypes
 * which results are not mappable, this class should be extended. Please note that in this case you have to clear your cache first.
 *
 * @author tschinke
 */
public class ImageFeature implements Comparable<ImageFeature>, Serializable {

	private float[] descriptor;
	private float orientation;
	private float scale;


	public ImageFeature() {

	}

	public ImageFeature(double[] desc) {
		descriptor = new float[desc.length];
		for (int i = 0; i < desc.length; i++) {
			descriptor[i] = (float) desc[i];
		}
	}


	public ImageFeature(float[] desc) {
		descriptor = desc;
	}

	public float[] getDescriptor() {
		return descriptor;
	}

	public void setDescriptor(float[] descriptor) {
		this.descriptor = descriptor;
	}

	public float getOrientation() {
		return orientation;
	}

	public void setOrientation(float orientation) {
		this.orientation = orientation;
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}


	public double distance(ImageFeature img) {
		float diff = 0;
		double dsq = 0;
		float[] descr1 = this.descriptor;
		float[] descr2 = img.descriptor;

		int d = this.descriptor.length;


		for (int i = 0; i < d; i++) {
			diff = descr1[i] - descr2[i];
			dsq += diff * diff;
		}
		return dsq;
	}

	public int compareTo(ImageFeature o) {
	    /*  float[] descr1=this.descriptor;
        float[] descr2=o.descriptor;
        int res=0;
        for (int i=0;i<descr1.length;i++)
        {
            res+=descr1[i]-descr2[i];
        }
        return res;*/
		return scale < o.scale ? 1 : scale == o.scale ? 0 : -1;
	}


}
