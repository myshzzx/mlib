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

import java.security.MessageDigest;

/**
 * The Hash Utilsclass makes you smile when hashing Integer/Byte arrays.
 * @author tschinke
 */
public class Hash {

    private static String getHashSumFromDigest(byte[] digest) {
        StringBuilder tmp=new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            String s = Integer.toHexString(digest[i] & 0xFF);
            if (s.length() == 1) {
                tmp.append("0");
            }
            tmp.append( s);
           
        }       
        return tmp.toString();
    }
    
    private static byte[] messageIntDigest(byte[] a) throws Exception {
        MessageDigest messagedigest = MessageDigest.getInstance("SHA1");
 
        messagedigest.update(a);
        return messagedigest.digest();
    }
    
    /**
     * Converts an integer array to a bytearray with a length of 4 times
     * @param b
     * @return
     */
    private static byte[] convert(int[] b)
    {
        byte[] res=new byte[b.length*4];
            for (int i=0;i<b.length;i++)
            {
                int z=i*4;
                int k=b[i];
                byte b0 =(byte) ((k >> 24) & 0xff);
                byte b1 =(byte) ( (k >> 16) & 0xff);
                byte b2 =(byte) ( (k >> 8) & 0xff);
                byte b3 =(byte) ( (k) & 0xff);
                res[z]=b0;
                res[z+1]=b1;
                res[z+2]=b2;
                res[z+3]=b3;
            }
        return res;
    }
    
    public static String getSHA1(byte[] b)
    {
        try {
            return getHashSumFromDigest(messageIntDigest(b));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static String getSHA1(int[] b)
    {
        try {
            
            return getHashSumFromDigest(messageIntDigest(convert(b)));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
