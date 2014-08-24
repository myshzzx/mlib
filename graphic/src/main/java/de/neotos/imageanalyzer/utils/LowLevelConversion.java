/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.neotos.imageanalyzer.utils;

import java.awt.*;
import java.awt.image.*;

/**
 *
 * @author tschinke
 */
public class LowLevelConversion {

    public final static int RGB2Grey(int argb) {
        // int a = (argb >> 24) & 0xff;
        int r = (argb >> 16) & 0xff;
        int g = (argb >> 8) & 0xff;
        int b = (argb) & 0xff;

        //int rgb=(0xff000000 | ((r<<16)&0xff0000) | ((g<<8)&0xff00) | (b&0xff));
        int y = (int) Math.round(0.299f * r + 0.587f * g + 0.114f * b);
        return y;
    }

    


    /**
     * converts a bufferedimage the fastest possible way in java
     * @param bim a bufferedimage containing a databufferbyte or databufferint
     * @return an int-array containing int values for color in the range of 0-255
     */
    public static final int[] convert2grey_fast(BufferedImage bim) {
        return convert2grey_fast(bim.getRaster().getDataBuffer());
    }

    /**
     * converts a bufferedimage in the fastest possible way for java
     * @param bim a bufferedimage containing a databufferbyte or databufferint
     * @return an int-array containing int values for color in the range of 0-255
     */
    public static final int[] convert2grey_fast(DataBuffer bim) {
        if (bim.getDataType() == DataBuffer.TYPE_BYTE) {
            DataBufferByte dbi = (DataBufferByte) bim;
            byte[] data = dbi.getData();
            int[] copy = new int[data.length / 3];
            int z=0;
            final int l=data.length;
            for (int i = 1; i < l; i += 3) {
                copy[z] = data[i] & 0x000000FF;  //just take green component
                z++; //cheaper than i/3

            }
            return copy;
        } else {
            DataBufferInt dbi = (DataBufferInt) bim;
            int[] data = dbi.getData();
            int[] copy = new int[data.length / 3];
            int z=0;
            final int l=data.length;
            for (int i = 1; i < l; i += 3) {
                copy[z] = data[i]; //just take green component
                z++; //cheaper than i/3
            }
            return copy;
        }
    }

    /**
     * This should work in most cases, but is at least 10 times slower then the fast method
     * @param img any image not null
     * @return an int-array containing int values for color in the range of 0-255
     */
    public static final int[] convert2grey(Image img) {
        PixelGrabber grabber = new PixelGrabber(img, 0, 0, -1, -1, true);
        try {
            grabber.grabPixels();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        int[] data = (int[]) grabber.getPixels();
        int[] image = new int[data.length];

        for (int d = 0; d < data.length; d++) {
            image[d] = RGB2Grey(data[d]);

        }
        return image;
    }

}
