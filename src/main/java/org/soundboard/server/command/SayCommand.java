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

import org.soundboard.audio.tts.*;
import org.soundboard.server.*;
import org.soundboard.server.inputservice.*;
import org.soundboard.util.*;

public class SayCommand extends Command {
   
   /**
    * get the command description
    */
   @Override public String getDescription() {
      return "Say the provided text.";
   }
   
   @Override
   public boolean isHttpCommand() {
      return false;
   }
   
   @Override
   public boolean affectsKarma() {
      return false;
   }

   /**
    * execute the command with the given arguements.  Note that args[0] is the name of the command.
    */
   @Override public String execute(InputService inputService, String who, String[] args, boolean isCron, boolean respondWithHtml) {
      StringBuffer out = new StringBuffer();
      String sentence = StringUtil.join(args,1, args.length, " ");
      LoggingService.getInstance().log((isCron ? "cron (" + who +")" : who) + ": " + sentence);
      
      try {
         org.soundboard.audio.SoundPlayer sbPlayer = org.soundboard.audio.SoundPlayer.getInstance();
         sbPlayer.play(GoogleTTS.toSoundBytes(sentence), sentence);
         out.append("I said: ");
         out.append(sentence);
         out.append("\n");
      } catch (Exception e) {
         LoggingService.getInstance().serverLog("Error Saying \""+sentence+"\": "+ e.getMessage());
         LoggingService.getInstance().serverLog(e);
      }
      return out.toString();
   }
   
}
