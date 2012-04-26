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

import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.sound.sampled.*;
import org.soundboard.audio.*;
import org.soundboard.audio.tts.*;
import org.soundboard.server.*;
import org.soundboard.util.*;

public class SoundLibrary {
   
   private static Map<String, SoundLibrary> INSTANCES = new HashMap<String, SoundLibrary>();
   public static final String DEFAULT_LIBRARY = "defaulLibrary";

   private Map<String, ChunkedByteBuffer> library = new TreeMap<String, ChunkedByteBuffer>();
   private Map<String, Long> loadAttempts = new HashMap<String, Long>();
   private Set<DirectoryListeningData> listenToDirs = new HashSet<DirectoryListeningData>();
   private String libraryName;
   private boolean logAdditions = false;
   
   private ActiveLibrary activeLibrary = null;
   
   private SoundLibrary(String libraryName) {
      this.libraryName = libraryName;
   }
   
   public static SoundLibrary getInstance() {
      return getInstance(DEFAULT_LIBRARY);
   }
   
   public static SoundLibrary getInstance(String libraryName) {
      if (INSTANCES.get(libraryName) == null) {
         SoundLibrary instance = new SoundLibrary(libraryName);
         INSTANCES.put(libraryName, instance);
      }
      return INSTANCES.get(libraryName);
   }
   
   public static boolean libraryExists(String libraryName) {
      return INSTANCES.get(libraryName) != null;
   }
   
   /**
    * refresh the load attempts so that they will be tried again.
    */
   public void resetLoadAttempts() {
      loadAttempts.clear();
   }
   
   /**
    * check if the given file should be attempted to be loaded
    */
   public boolean attemptLoad(File file) {
      boolean attempt = false;
      if (file != null) {
         String shortName = file.getName().substring(0, file.getName().lastIndexOf(".")).toLowerCase();
         Long lastAttemptedModifyTimestamp = loadAttempts.get(shortName);
         attempt = (lastAttemptedModifyTimestamp == null || lastAttemptedModifyTimestamp.longValue() != file.lastModified());
      }
      return attempt;
   }

   /**
    * register a directory to be listened for with the active library
    */
   public void registerListener(String dirname) {
      registerListener(dirname, false);
   }
   public void registerListener(String dirname, boolean recurseSubdirectories) {
      registerListener(dirname, false, null);
   }
   public void registerListener(String dirname, boolean recurseSubdirectories, FilenameFilter filter) {
      listenToDirs.add(new DirectoryListeningData(new File(dirname), recurseSubdirectories, filter));
      
      if (activeLibrary == null || !activeLibrary.isAlive()) {
         activeLibrary = new ActiveLibrary();
      }
   }
   
   public void stopActiveLibrary() {
      listenToDirs.clear();
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
   public boolean addFile(File source) {
      boolean added = false;
      if (source != null && !source.isDirectory()) {
         try {
            String shortName = source.getName().substring(0, source.getName().lastIndexOf(".")).toLowerCase();
            if (attemptLoad(source) && library.get(shortName) == null) {
               loadAttempts.put(shortName, new Long(source.lastModified()));
               long fileLen = source.length();
               //use the file length to set the buffer size
               int chunkSize = ChunkedByteBuffer.DEFAULT_CHUNK_SIZE;
               int numChunks = ChunkedByteBuffer.DEFAULT_NUMBER_OF_CHUNKS;
               if (fileLen > chunkSize * numChunks) {
                  numChunks = new Long((fileLen / chunkSize) + 1).intValue();
               }
               ChunkedByteBuffer data;
               if (Pattern.compile("\\.txt$").matcher(source.getName()).find()) {
                  ChunkedCharBuffer text = new ChunkedCharBuffer(chunkSize, numChunks);
                  FileReader fr = new FileReader(source);
                  text.append(fr);
                  fr.close();

                  data = GoogleTTS.toSoundBytes(text.toString());
               } else {
                  data = new ChunkedByteBuffer(chunkSize, numChunks);
                  FileInputStream fis = new FileInputStream(source);
                  data.append(fis);
                  fis.close(); // Release the file lock
               }
               
               //check that the data is valid audio data
               AudioInputStream audioStream = AudioSystem.getAudioInputStream(data.toInputStream());
               DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioStream.getFormat());
               try {
                  SoundPlayer.getInstance().getDevice().getLine(info);
               } catch (NullPointerException e) {
                  //ignore
               }
                  
               library.put(shortName, data);
               added = true;
               if (logAdditions && added) {
                  LoggingService.getInstance().log("Added '" + shortName + "' into library" + (!libraryName.equals(DEFAULT_LIBRARY) ? " " + libraryName : ""));
               }
            }
         } catch (Exception e) {
            LoggingService.getInstance().log("Unable to load '" + source.getAbsolutePath() + "' into library" + (!libraryName.equals(DEFAULT_LIBRARY) ? " " + libraryName : ""));
            LoggingService.getInstance().serverLog(e);
         }
      }
      return added;
   }
   
   public static final File TEMP_DIR;
   static {
      File tmpFile = new File(System.getenv().get("TEMP"));
      if (!tmpFile.exists()) {
         tmpFile = new File(".");
      }
      TEMP_DIR = tmpFile;
   }
   
   /**
    * get the list of files in the library (lists the short name -- lookup key)
    */
   public Set<String> list() {
      return library.keySet();
   }

   /**
    * get the data for the given short filename
    */
   public ChunkedByteBuffer getData(String shortName) {
      return library.get(shortName.toLowerCase());
   }
   
   /**
    * get the inputstream for the given short filename
    */
   public InputStream getDataStream(String shortName) {
      ChunkedByteBuffer data = library.get(shortName.toLowerCase());
      return data != null ? data.toInputStream() : null;
   }
   
   /**
    * get the inputstream for the given short filename
    */
   public ChunkedByteBuffer getAudioData(String shortName) {
      return library.get(shortName.toLowerCase());
   }
   
   class DirectoryListeningData {
      public File dir;
      public boolean recurse = false;
      public FilenameFilter filter = null;
      
      public DirectoryListeningData(File dir, boolean recurse, FilenameFilter filter) {
         this.dir = dir;
         this.recurse = recurse;
         this.filter = filter;
      }
   }
   
   class ActiveLibrary extends Thread {
      public ActiveLibrary() {
         this.setName("Active Library Listener for " + libraryName);
         this.start();
      }
      @Override
      public void run() {
         try {
            while (listenToDirs.size() > 0) {
               for (DirectoryListeningData data : listenToDirs) {
                  addDirectory(data.dir, data.recurse, data.filter);
               }
               sleep(10000);
               logAdditions = true;
            }
         } catch (Exception e) {
            LoggingService.getInstance().serverLog("Error actively listening for library changes.  De-activating");
         }
      }
   }
   
}
