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
import java.util.regex.*;
import org.soundboard.library.*;
import org.soundboard.server.*;
import org.soundboard.server.inputservice.*;

public class ListCommand extends Command {
   
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
      return "Lists all of the sound files in the library.  Optionally accepts a RegEx arguement to filter the list.";
   }

   /**
    * execute the command with the given arguements.  Note that args[0] is the name of the command.
    */
   public String execute(InputService inputService, String who, String[] args, boolean isCron, boolean respondWithHtml) {
      LoggingService.getInstance().serverLog(who + ": " + args[0] + (args.length > 1 ? " " + args[1] : ""));
      StringBuilder out = new StringBuilder();
      SoundLibrary library = SoundLibrary.getInstance();
      Set<String> books = library.list();
      boolean returnedData = false;
      if (respondWithHtml) {
         out.append("<table>");
      }
      if (books.size() > 0) {
         Pattern pattern = null;
         if (args.length >= 2) {
            pattern = Pattern.compile(args[1], Pattern.CASE_INSENSITIVE);
         }
         for (String book : books) {
            if (pattern == null || pattern.matcher(book).find()) {
               if (respondWithHtml) {
                  out.append("<tr><td><a href=\"/play/");
                  out.append(book);
                  out.append("\">");
                  out.append(book);
                  out.append("</a>");
                  out.append("</td></tr>");
               } else {
                  out.append(book);
                  out.append("\n");
               }
               returnedData = true;
            }
         }
      }
      if (respondWithHtml) {
         out.append("</table>");
      }
      if (!returnedData) {
         out.append("No sounds in the library that match.");
         out.append("\n");
      }
      return out.toString();
   }
   
}