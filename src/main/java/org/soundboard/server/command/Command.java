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

import org.soundboard.server.inputservice.InputService;

public abstract class Command {

   /**
    * return if this command is cronable
    */
   public boolean isCronable() {
      return true;
   }

   /**
    * return if this command is able to be used in http
    */
   public boolean isHttpCommand() {
      return true;
   }

   public boolean shouldRelay() {
      return true;
   }

   /**
    * get the command description
    */
   public abstract String getDescription();

   /**
    * execute the command with the given arguements.  Note that args[0] is the name of the command.
    */
   public abstract String execute(InputService inputService, String who, String[] args, boolean isCron, boolean respondWithHtml);

}
