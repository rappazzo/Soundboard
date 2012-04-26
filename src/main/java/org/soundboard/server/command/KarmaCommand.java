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

public class KarmaCommand extends Command {
   
   /**
    * return if this command should affects karma
    */
   public boolean affectsKarma() {
      return true;
   }
   
   /**
    * return if this command is cronable
    */
   public boolean isCronable() {
      return false;
   }
   
   /**
    * return if this command is able to be used in http 
    */
   public boolean isHttpCommand() {
      return false;
   }
   
   /**
    * get the command description
    */
   public String getDescription() {
      return "Increase the bad karma level for the given person's IM ID.  Using this command will increase your bad karma also.";
   }

   /**
    * execute the command with the given arguements.  Note that args[0] is the name of the command.
    */
   public String execute(InputService inputService, String who, String[] args, boolean isCron, boolean respondWithHtml) {
      StringBuffer out = new StringBuffer();
      if (args.length > 1) {
         LoggingService.getInstance().serverLog(who + " is decreasing the karma for " + args[1]);
         Karma karma = Karma.getInstance();
         //this will actually make the command user's karma up by 2 (intentionally) (because they have already been checked)
         karma.checkKarma(who); 
         karma.checkKarma(args[1]);
         LoggingService.getInstance().privateMessage(args[1], who + " is decreasing your karma.  Your karma is now " + (-1 * karma.currentKarma(args[1])));
      }
      return out.toString();
   }
   
}
