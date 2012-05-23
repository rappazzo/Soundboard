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
import org.soundboard.library.*;
import org.soundboard.server.*;
import org.soundboard.server.inputservice.*;
import org.soundboard.util.*;

public class GibberishCommand extends Command {
   
   /**
    * get the command description
    */
   @Override public String getDescription() {
      return "Randomly plays X number of sounds from the given library.  ";
   }

   public GibberishCommand() {
   }
   
   /**
    * execute the command with the given arguements.  Note that args[0] is the name of the command.
    */
   @Override public String execute(InputService inputService, String who, String[] args, boolean isCron, boolean respondWithHtml) {
      StringBuffer out = new StringBuffer();
      LoggingService.getInstance().log((isCron ? "cron (" + who +")" : who) + ": " + args[0]);
      //make the command flexible by seeking args ranther than forcing them to be in a specific order
      Integer numberOfWords = null;
      int index; //remember where the numberOfWords arg was found
      for (index = 1; index < args.length && numberOfWords == null; index++) {
         try {
            numberOfWords = new Integer(args[1]);
         } catch (NumberFormatException e) {
            //that arg wasn't an integer -- move on the the next one
         }
      }
      String libName = null;
      if (args.length > (2 - (numberOfWords == null ? 1 : 0))) {
         libName = args[index == 1 ? 2 : 1].toLowerCase();
      }
      if (numberOfWords == null) {
         //a number between 4 and 10;
         numberOfWords = Randomizer.getRandomizer().nextInt(6) + 4;
      }
      if (libName == null || !SoundLibrary.libraryExists(libName)) {
         libName = SoundLibrary.DEFAULT_LIBRARY;
      }
      SoundLibrary lib = SoundLibrary.getInstance(libName);
      List<String> words = new ArrayList<String>();
      words.addAll(lib.list());
      //build the command to send to sentence
      String[] newArgs = new String[numberOfWords + 2];
      newArgs[0] = libName;
      for (int i = 1; i <= numberOfWords + 1; i++) {
         newArgs[i] = words.get(Randomizer.getRandomizer().nextInt(words.size()));
      }
      SentenceCommand sentence = new SentenceCommand();
      out.append(sentence.execute(inputService, who, newArgs, isCron, respondWithHtml));
      
      Statistics.incrementCount(args[0], who);
      return out.toString();
   }
   
}
