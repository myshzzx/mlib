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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Some other IO stuff missing in Javas libraries. Not all is needed, but a copypaste from neotos-Common package.
 * @author tschinke
 */
public class IO 
{
    /**
     * 
     * @param source
     * @param destination
     * @param failIfExist
     * @return true if copyied, false if exists, otherwise exceptions
     */
    public static boolean copyFile(File source,File destination,boolean failIfExist)
    {
        if (source==null)
            throw new RuntimeException("src is null");
        if (destination==null)
            throw new RuntimeException("dest is null");
        if (source.isDirectory())
            throw new RuntimeException("src is directory");
        if (destination.isDirectory())
            throw new RuntimeException("dest is directory");
        
        if (failIfExist && destination.exists())
            return false;
        
      
        FileChannel inChannel=null;
        FileChannel outChannel=null;
        try {
        inChannel = new FileInputStream(source).getChannel();
        outChannel = new FileOutputStream(destination).getChannel();
        
        inChannel.transferTo(0, inChannel.size(), outChannel);
        return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            boolean eoc=false;
            try{
            if (inChannel != null)
                inChannel.close();
            }catch(Exception e)
            {
                eoc=true;
            }
            try{
            if (outChannel != null)
                outChannel.close();
            }catch(Exception e)
            {
                eoc=true;
            }
            if (eoc)
                throw new RuntimeException("input or output can't be closed");
            
        } 
    }
    
    
    public static void copyTree(File src, File dest)
    {
        if (!src.isDirectory())
            throw new RuntimeException("Source is no directory: "+src.getAbsolutePath());
        if (!dest.isDirectory())
            throw new RuntimeException("Destination is no directory: "+dest.getAbsolutePath());
        
        File[] files=src.listFiles(new FileFilter() {

            public boolean accept(File arg0) {
                return !arg0.isDirectory();
            }
        });
        
        File[] dirs=src.listFiles(new FileFilter() {

            public boolean accept(File arg0) {
                return arg0.isDirectory();
            }
        });
        
        for (File f:files)
        {
            try{
                File target=new File(IO.addDirSep(dest.getAbsolutePath())+f.getName());

                IO.copyFile(f, target, false);
                System.out.println("copied "+f.getAbsolutePath()+" -> "+target.getAbsolutePath());
            }catch(Exception e)
            {
                System.out.println("Can't copy "+f.getAbsolutePath());
                e.printStackTrace();
            }
                
        }
        
        for (File d:dirs)
        {
            File target=new File(IO.addDirSep(dest.getAbsolutePath())+d.getName());
            if (!target.exists())
                target.mkdir();
            copyTree(d, target);
            
        }
        
    }
    
    /**
     * Checks wether the given file, assumed to be a directory, is writeable or not
     * @param dir
     * @return
     */
    public static boolean isWritable(File dir)
    {
        if ((!dir.exists())||(!dir.isDirectory()))
            throw new RuntimeException("Directory does not exist: "+dir.getAbsolutePath());
        
        dir.setWritable(true);
        return dir.canWrite();
    }
    
    public static boolean copyFile(String source,String destination,boolean failIfExist)
    {
        return copyFile(new File(source), new File(destination), failIfExist);
    }
    /**
     * 
     * @param name creates a Folder name in users homedirectory
     * @return
     */
    public static File getUserProgramHome(String name)
    {
       String appDir = System.getProperty("user.home") + File.separator + name + File.separator;
     
       File f=new File(appDir);
       if (!f.exists())
               f.mkdir();
       
       return f;
     
    }
    
    public static void removeDirContent(String dir)
    {
        removeDirContent(new File(dir));
    }
    /**
     * Removes all files and directories recursivley in dir, but dir will remain
     * @param dir
     */
    public static void removeDirContent(File dir)
    {
        if (dir==null)
            throw new RuntimeException("Cannot clear null-dir");
        if (!dir.exists()||!dir.isDirectory())
            throw new RuntimeException("Directory does not exist: "+dir.getAbsolutePath());
        
        for (File f:dir.listFiles())
        {
            if (f.isDirectory())
                removeDirContent(f);
            f.delete();
        }
    }
    
    /**
     * 
     * @param name creates a Folder name in programs workingdirectory 
     * @return
     */
    public static String getWorkingDir(String name)
    {
        String appDir = System.getProperty("user.dir") + File.separator + name + File.separator;
     
       File f=new File(appDir);
       if (!f.exists())
               f.mkdir();
        
       return appDir;
    }
    
      public static String getFileExt(String f)
    {
        if (!f.contains("."))
            return "";
        return f.substring(f.lastIndexOf("."));
    }
    
    public static String removeFileExt(String f)
    {
       if (!f.contains("."))
            return f;
        return f.substring(0,f.lastIndexOf(".")); 
    }
    
    public static String getFileName(String f)
    {
        if (!f.contains(File.separator))
            return f;
        return f.substring(f.lastIndexOf(File.separator)+1);
    }
    
    public static String addDirSep(String s)
    {
        if (!s.endsWith(File.separator))
            s+=File.separator;
        return s;
    }
}
