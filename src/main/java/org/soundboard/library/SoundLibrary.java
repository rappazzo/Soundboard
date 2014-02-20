/***
 **
 ** This library is free software; you can redistribute it and/or
 ** modify it under the terms of the GNU Lesser General Public
 ** License as published by the Free Software Foundation; either
 ** version 2.1 of the License, or (at your option) any later version.
 **
 ** This library is distributed in the hope that it will be useful,
 ** but WITHOUT ANY WARRANTY; without even the implied warranty of
 ** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 ** Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public
 ** License along with this library; if not, write to the Free Software
 ** Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 **
 **/
package org.soundboard.library;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.soundboard.util.ChunkedByteBuffer;
import com.google.common.base.Preconditions;

public abstract class SoundLibrary {

   private Map<String, Long> loadAttempts = new HashMap<String, Long>();
   private String libraryName = null;
   
   
   public void setName(String libraryName) {
      Preconditions.checkState(this.libraryName == null || this.libraryName.equals(libraryName), "%s is being renamed to %s", this.libraryName, this.libraryName);
      this.libraryName = libraryName;
   }
   
   public String getName() {
      return this.libraryName;
   }
   
   /**
    * refresh the load attempts so that they will be tried again.
    */
   public final void resetLoadAttempts() {
      loadAttempts.clear();
   }
   
   protected void markAttempt(File source) {
      String shortName = source.getName().substring(0, source.getName().lastIndexOf(".")).toLowerCase();
      loadAttempts.put(shortName, new Long(source.lastModified()));
   }
   
   /**
    * check if the given file should be attempted to be loaded
    */
   protected final boolean shouldAttemptLoad(File file) {
      boolean attempt = false;
      if (file != null) {
         String shortName = file.getName().substring(0, file.getName().lastIndexOf(".")).toLowerCase();
         Long lastAttemptedModifyTimestamp = loadAttempts.get(shortName);
         attempt = (lastAttemptedModifyTimestamp == null || lastAttemptedModifyTimestamp.longValue() != file.lastModified());
      }
      return attempt;
   }
   
   /**
    * add a directory to the library
    * return if the file was added to the library
    */
   public void addDirectory(String dirname) {
      addDirectory(dirname, false);
   }
   
   /**
    * add a directory to the library
    * return if the file was added to the library
    */
   public void addDirectory(String dirname, boolean recurseSubdirectories) {
      addDirectory(dirname, recurseSubdirectories, null);
   }
   
   /**
    * add a directory to the library
    * return if the file was added to the library
    */
   public void addDirectory(String dirname, boolean recurseSubdirectories, FilenameFilter filter) {
      addDirectory(new File(dirname), recurseSubdirectories, filter);
   }
   
   /**
    * add a directory to the library
    * return if the file was added to the library
    */
   public void addDirectory(File dir, boolean recurseSubdirectories, FilenameFilter filter) {
      if (dir.isDirectory()) {
         File[] files = filter != null ? dir.listFiles(filter) : dir.listFiles();
         if (files != null) {
            for (File file : files) {
               if (file.isDirectory() && recurseSubdirectories) {
                  addDirectory(file, recurseSubdirectories, filter);
               } else {
                  addFile(file);
               }
            }
         }
      }
   }
   
   /**
    * add a file to the library
    * return if the file was added to the library
    */
   public boolean addFile(String filename) {
      return addFile(new File(filename));
   }
   
   /**
    * add a file to the library
    * return if the file was added to the library
    */
   public abstract boolean addFile(File source);
   
   /**
    * get the list of files in the library (lists the short name -- lookup key)
    */
   public abstract Set<String> list();

   /**
    * Get the file for the given name
    */
   public abstract File getFile(String name);
   
   /**
    * get the data for the given short filename
    */
   public abstract ChunkedByteBuffer getData(String name);
   
   /**
    * get the inputstream for the given short filename
    */
   public InputStream getDataStream(String name) {
      ChunkedByteBuffer data = getData(name);
      return data != null ? data.toInputStream() : null;
   }
   
}
