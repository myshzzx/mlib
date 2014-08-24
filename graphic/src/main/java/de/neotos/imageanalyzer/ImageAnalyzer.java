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
import java.util.Properties;

/**
 * A generic interface for image-recognition. 
 * @author tschinke
 */
public interface ImageAnalyzer {

    /**
     * The setup-method has a more optional state, so the effect on
     * the implementing class cannot be guaranteed. 
     * @param properties properties to can give to the implementing Class
     * @return true if setup was successful
     */
    public boolean setup(Properties properties);

    
    /**
     * Adds an image and binds an object to it.
     * @param image the image to bind
     * @param o is the object associated with the image. You can say that you can
     * distinguish key-features of images by their objects if these objects checkEquals-method
     * returns false.
     * @return on success true otherwise false
     */
    public boolean bindImage(Image image, Object o);

    /**
     * Removes and unbinds the key-features of a given image
     * @param image the image to unbind
     * @return on success true otherwise false
     */
    public boolean unbindImage(Image image);
    
    
    /**
     * Unbind all images which have an Object binded equal to o.
     * Please remember that an (efficient) implementation is not always possible or useful
     * @param o user-binded object
     * @return
     */
    public boolean unbindImage(Object o);

    /**
     * Performs image recognition on the given image and returns the most
     * probable results.
     * @param image the image to compare with images from the database
     * @return the list with the most probable results sorted by relevance. The
     *         most probable result is the first in the result list.
     */
    public List<ImageAnalyzerResult> findImage(Image image);
}






