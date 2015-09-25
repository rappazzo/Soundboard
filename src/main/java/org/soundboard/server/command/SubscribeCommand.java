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

import org.soundboard.server.*;
import org.soundboard.server.inputservice.*;

public class SubscribeCommand extends Command {
   
   /**
    * return if this command is able to be used in http
    */
   @Override public boolean isHttpCommand() {
      return false;
   }
   
   /**
    * get the command description
    */
   @Override  public String getDescription() {
      return "Subscribe to messages (for example who just played that sound).";
   }

   /**
    * execute the command with the given arguements.  Note that args[0] is the name of the command.
    */
   @Override public String execute(InputService inputService, String who, String[] args, boolean isCron, boolean respondWithHtml) {
      LoggingService.getInstance().serverLog(who + ": " + args[0]);
      LoggingService.getInstance().subscribe(who, inputService.getClass().getName());
      return "Subscribed " + who;
   }
   
}
