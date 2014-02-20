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

public class RandomSoundPlayerCommand extends Command {
   
   /**
    * get the command description
    */
   @Override public String getDescription() {
      return "Plays a random sound from the given sound library.  ";
   }

   public RandomSoundPlayerCommand() {
   }
   
   /**
    * execute the command with the given arguements.  Note that args[0] is the name of the command.
    */
   @Override public String execute(InputService inputService, String who, String[] args, boolean isCron, boolean respondWithHtml) {
      StringBuffer out = new StringBuffer();
      LoggingService.getInstance().log((isCron ? "cron (" + who +")" : who) + ": " + args[0]);
      SoundPlayer player = SoundPlayer.get();
      String libName = args[0].toLowerCase();
      if (!SoundLibrarian.libraryExists(libName)) {
         libName = SoundLibrarian.DEFAULT_LIBRARY;
      }
      SoundLibrary lib = SoundLibrarian.getInstance(libName);
      List<String> responses = new ArrayList<String>();
      responses.addAll(lib.list());
      if (responses.size() > 0) {
         String sound = responses.get(Randomizer.getRandomizer().nextInt(responses.size()));
         player.play(lib, sound);
         Statistics.incrementCount(args[0], who);
      }
      return out.toString();
   }
   
}
