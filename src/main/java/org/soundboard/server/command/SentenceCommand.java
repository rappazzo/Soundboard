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

import org.soundboard.audio.*;
import org.soundboard.library.*;
import org.soundboard.server.*;
import org.soundboard.server.inputservice.*;
import org.soundboard.util.*;

public class SentenceCommand extends Command {
   
   /**
    * get the command description
    */
   @Override public String getDescription() {
      return "Play the named sounds.";
   }
   
   @Override
   public boolean isHttpCommand() {
      return false;
   }

   /**
    * execute the command with the given arguements.  Note that args[0] is the name of the command.
    */
   @Override public String execute(InputService inputService, String who, String[] args, boolean isCron, boolean respondWithHtml) {
      StringBuffer out = new StringBuffer();
      String[] words = new String[args.length - 1];
      if (args.length > 1) {
         System.arraycopy(args, 1, words, 0, words.length);
         LoggingService.getInstance().log((isCron ? "cron (" + who +")" : who) + ": " + args[0] + " \"" + StringUtil.join(words, " ") + "\"");

         String libName = args[0].toLowerCase();
         if (!SoundLibrary.libraryExists(libName)) {
            libName = SoundLibrary.DEFAULT_LIBRARY;
         }
         SoundLibrary lib = SoundLibrary.getInstance(libName);
         SoundPlayer player = SoundPlayer.getInstance();
         String playInfo = player.play(lib, words);
         if (playInfo != null && !playInfo.equals("")) {
            out.append(playInfo);
            out.append("\n");
         }
         Statistics.incrementCount(args[0], who);
      }
      return out.toString();
   }
   
}
