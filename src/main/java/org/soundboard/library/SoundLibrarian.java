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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.soundboard.server.LoggingService;
import org.soundboard.server.SoundboardConfiguration;
import org.soundboard.util.NamedThreadFactory;
import com.google.common.base.Preconditions;

public class SoundLibrarian {
   
   private static Map<String, SoundLibrary> INSTANCES = new HashMap<String, SoundLibrary>();
   public static final String DEFAULT_LIBRARY = "defaulLibrary";

   private static Set<DirectoryListeningData> listenToDirs = new HashSet<DirectoryListeningData>();
   private static ExecutorService LIBRARY_THREAD_POOL = Executors.newCachedThreadPool(new NamedThreadFactory("Active Library Listener", true));

   private static Future activeLibrary = null;
   
   public static SoundLibrary getInstance() {
      return getInstance(DEFAULT_LIBRARY);
   }
   
   public static SoundLibrary getInstance(String libraryName) {
      if (INSTANCES.get(libraryName) == null) {
         SoundLibrary instance = newSoundLibrary(libraryName);
         INSTANCES.put(libraryName, instance);
      }
      return INSTANCES.get(libraryName);
   }
   
   private static SoundLibrary newSoundLibrary(String libraryName) {
      SoundLibrary lib = (SoundLibrary)SoundboardConfiguration.config().getClassProperty("SoundLibrary.impl");
      Preconditions.checkState(lib != null, "Error: MISSING SoundLibrary implementation ('SoundLibrary.impl')");
      lib.setName(libraryName);
      return lib;
   }

   public static boolean libraryExists(String libraryName) {
      return INSTANCES.get(libraryName) != null;
   }
   
   /**
    * register a directory to be listened for with the active library
    */
   public static void registerListener(SoundLibrary lib, String dirname) {
      registerListener(lib, dirname, false);
   }
   public static void registerListener(SoundLibrary lib, String dirname, boolean recurseSubdirectories) {
      registerListener(lib, dirname, false, null);
   }
   public static void registerListener(SoundLibrary lib, String dirname, boolean recurseSubdirectories, FilenameFilter filter) {
      listenToDirs.add(new DirectoryListeningData(lib, new File(dirname), recurseSubdirectories, filter));
      
      if (activeLibrary == null || activeLibrary.isDone()) {
         activeLibrary = LIBRARY_THREAD_POOL.submit(new ActiveLibrary());
      }
   }
   
   public static void stopActiveLibrary() {
      listenToDirs.clear();
   }
   
   
   static class DirectoryListeningData {
      private SoundLibrary lib;
      private File dir;
      private boolean recurse = false;
      private FilenameFilter filter = null;
      
      public DirectoryListeningData(SoundLibrary lib, File dir, boolean recurse, FilenameFilter filter) {
         this.lib = lib;
         this.dir = dir;
         this.recurse = recurse;
         this.filter = filter;
      }
   }
   
   //TODO: try to use the OS file system to listen for changes
   private static class ActiveLibrary implements Runnable {
      @Override public void run() {
         try {
            while (listenToDirs.size() > 0) {
               for (DirectoryListeningData data : listenToDirs) {
                  data.lib.addDirectory(data.dir, data.recurse, data.filter);
               }
               Thread.sleep(10000);
            }
         } catch (Exception e) {
            LoggingService.getInstance().serverLog("Error actively listening for library changes.  De-activating");
         }
      }
   }
   
}
