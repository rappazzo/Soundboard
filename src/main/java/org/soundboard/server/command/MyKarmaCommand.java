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

import org.soundboard.server.inputservice.*;
import org.soundboard.util.*;

public class MyKarmaCommand extends Command {
   
   public static final String ALL = "ALL";
   
   /**
    * return if this command should affects karma
    */
   public boolean affectsKarma() {
      return false;
   }
   
   /**
    * get the command description
    */
   public String getDescription() {
      return "Gets the karma level for the given user (or your karma level by default).  ";
   }

   /**
    * execute the command with the given arguements.  Note that args[0] is the name of the command.
    */
   public String execute(InputService inputService, String who, String[] args, boolean isCron, boolean respondWithHtml) {
      StringBuffer out = new StringBuffer();
      Karma karma = Karma.getInstance();
      String query = args.length > 1 ? args[1] : who;
      if (query.equals(ALL)) {
         out.append("Karma levels for ALL (non-zero): ");
         String allKarma = karma.currentKarma();
         out.append(StringUtil.isNotNullOrEmpty(allKarma) ? allKarma : " -- None.  The soundboard is balanced. :-) --");
      } else {
         out.append("Karma for " + query + " is " + (-1 * karma.currentKarma(query)));
      }
      return out.toString();
   }
   
}
