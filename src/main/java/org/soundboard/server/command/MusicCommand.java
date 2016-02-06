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

import java.util.Arrays;
import java.util.List;

import org.soundboard.audio.SpotifyViaBash;
import org.soundboard.server.LoggingService;
import org.soundboard.server.inputservice.InputService;

import com.google.common.base.Joiner;

public class MusicCommand extends Command {

   /**
    * get the command description
    */
   @Override public String getDescription() {
      return "Control the music.";
   }

   @Override
   public boolean isHttpCommand() {
      return false;
   }

   @Override
   public boolean isCronable() {
      return false;
   }

   /**
    * execute the command with the given arguments.  Note that args[0] is the name of the command.
    */
   @Override public String execute(InputService inputService, String who, String[] args, boolean isCron, boolean respondWithHtml) {
      SpotifyViaBash player = SpotifyViaBash.getInstance();
      StringBuilder out = new StringBuilder();
      List<String> command = args.length > 1 ? Arrays.asList(args).subList(1, args.length) : Arrays.<String>asList();
      String playInfo = player.send(command);
      if (playInfo != null && !playInfo.isEmpty()) {
    	  out.append(playInfo);
    	  out.append("\n");
      }

      LoggingService.getInstance().log(who + ": music " + Joiner.on(' ').join(command));
      return out.toString();
   }

}
