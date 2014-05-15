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
package org.soundboard.audio;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.soundboard.library.SoundLibrarian;
import org.soundboard.library.SoundLibrary;
import org.soundboard.server.LoggingService;
import org.soundboard.util.ChunkedByteBuffer;
import com.google.common.base.Joiner;

public class SoundPlayerOSX extends SoundPlayer {
   
   private List<Process> NOW_PLAYING = new ArrayList();
   
   /**
    * play a sound from the defaul library
    * @return info text
    */
   @Override
   public String play(String sound) {
      return play(SoundLibrarian.getInstance(), sound);
   }
   
   /**
    * stop playing all sounds which are currently playing
    */
   @Override
   public void stop() {
      for (Process p : NOW_PLAYING) {
         p.destroy();
      }
      NOW_PLAYING.clear();
   }
   
   /**
    * play a sound from the given library
    * @return info text
    */
   @Override
   public String play(SoundLibrary library, String... sound) {
      StringBuilder result = new StringBuilder();
      if (sound != null) {
         final List<ProcessBuilder> toPlay = new ArrayList();
         for (String name : sound) {
            File audioFile = library.getFile(name);
            if (audioFile != null) {
               ProcessBuilder procBuilder = new ProcessBuilder();
               procBuilder.command("afplay", audioFile.getAbsolutePath());
               toPlay.add(procBuilder);
            } else {
               if (result.length() > 0) {
                  result.append(", ");
               }
               result.append(name);
            }
         }
         if (!toPlay.isEmpty()) {
            //Note: this is unstoppable
            //TODO: execute these in a separate thread -- warning NOW_PLAYING concurrency
            for (ProcessBuilder procBuilder : toPlay) {
               try {
                  Process proc = procBuilder.start();
                  proc.waitFor();
               } catch (Exception e) {
                  LoggingService.getInstance().serverLog("Error playing " + Joiner.on(" ").join(sound) + ": ");
                  e.printStackTrace(LoggingService.getInstance().getServerLog());
               }
            }
         }
         if (result.length() > 0) {
            result.insert(0, "Could not find ");
            result.append(" in the library.");
         }
      } else {
         result.append("Attempt to play a sound file before the device is specified");
      }
      if (result.length() > 0) {
         LoggingService.getInstance().serverLog(result.toString());
         return result.toString();
      }
      return null;
   }
   
   public void play(File soundData, String name) {
      try {
         ProcessBuilder procBuilder = new ProcessBuilder("afplay", soundData.getAbsolutePath());
         Process proc = procBuilder.start();
         NOW_PLAYING.add(proc);
      } catch (Exception e) {
         LoggingService.getInstance().serverLog("Error playing " + name + ": ");
         e.printStackTrace(LoggingService.getInstance().getServerLog());
      }
   }
   
   @Override public void play(ChunkedByteBuffer soundData, String name) {
      //this could write to a file, but for now, no-op 
   }

}
