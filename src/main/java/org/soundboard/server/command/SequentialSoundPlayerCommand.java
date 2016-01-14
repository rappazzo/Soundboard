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
package org.soundboard.server.command;

import java.util.*;
import org.soundboard.audio.*;
import org.soundboard.library.*;
import org.soundboard.server.*;
import org.soundboard.server.inputservice.*;
import org.soundboard.util.*;

public class SequentialSoundPlayerCommand extends Command {
   
   /**
    * get the command description
    */
   @Override public String getDescription() {
      return "Plays the next sound in sequential order in this library.";
   }

   private static int index = 0;
   
   public SequentialSoundPlayerCommand() {
   }
   
   /**
    * execute the command with the given arguements.  Note that args[0] is the name of the command.
    */
   @Override public String execute(InputService inputService, String who, String[] args, boolean isCron, boolean respondWithHtml) {
      StringBuffer out = new StringBuffer();
      LoggingService.getInstance().log((isCron ? "cron (" + who +")" : who) + ": " + args[0]);
      String libName = args[0].toLowerCase();
      if (!SoundLibrarian.libraryExists(libName)) {
         libName = SoundLibrarian.DEFAULT_LIBRARY;
      }
      SoundLibrary lib = SoundLibrarian.getInstance(libName);
      List<String> responses = new ArrayList<String>();
      responses.addAll(lib.list());
      
      //the java implementation makes the sounds play smoother, but only really supports WAV/PCM byte data
      SoundPlayer player = SoundPlayer.java();
      player.play(lib, responses.get(index < responses.size() ? index++ : 0));
      if (index >= responses.size()) {
         index = 0;
      }
      Statistics.incrementCount(args[0], who);
      return out.toString();
   }
   
}
