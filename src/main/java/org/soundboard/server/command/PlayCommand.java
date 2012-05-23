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

public class PlayCommand extends Command {
   
   /**
    * get the command description
    */
   @Override public String getDescription() {
      return "Play the named sound.";
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
      LoggingService.getInstance().log((isCron ? "cron (" + who +")" : who) + ": " + args[1]);
      SoundPlayer player = SoundPlayer.getInstance();
      String playInfo = player.play(args[1]);
      if (playInfo != null && !playInfo.equals("")) {
         out.append(playInfo);
         out.append("\n");
      }
      SoundLibrary lib = SoundLibrary.getInstance();
      if (lib.getAudioData(args[1]) != null) {
         Statistics.incrementCount(args[1], who);
      }
      return out.toString();
   }
   
}
