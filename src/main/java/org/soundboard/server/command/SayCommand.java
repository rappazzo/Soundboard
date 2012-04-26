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

import java.io.*;
import javax.sound.sampled.*;
import org.soundboard.library.*;
import org.soundboard.server.*;
import org.soundboard.server.inputservice.*;
import org.soundboard.util.*;
import com.sun.speech.freetts.*;
import com.sun.speech.freetts.audio.*;

public class SayCommand extends Command {
   
   /**
    * get the command description
    */
   public String getDescription() {
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
   public String execute(InputService inputService, String who, String[] args, boolean isCron, boolean respondWithHtml) {
      StringBuffer out = new StringBuffer();
      String sentence = StringUtil.join(args,1, args.length, " ");
      LoggingService.getInstance().log((isCron ? "cron (" + who +")" : who) + ": " + sentence);
      
      try {
         String name = Thread.currentThread().getName().replaceAll("[^\\d\\w]+", "");
         String dataFileBase = SoundLibrary.TEMP_DIR + "/" + name;
         AudioPlayer player = new SingleFileAudioPlayer(dataFileBase, AudioFileFormat.Type.WAVE);
         player.setVolume(2);
         Voice voice = VoiceManager.getInstance().getVoice("kevin16");
         voice.setAudioPlayer(player);
         voice.setVolume(2);
         voice.allocate();
         voice.speak(sentence.trim());
         voice.deallocate();
         player.close();
         File dataFile = new File(dataFileBase + ".wav");
         long fileLen = dataFile.length();
         //use the file length to set the buffer size
         int chunkSize = ChunkedByteBuffer.DEFAULT_CHUNK_SIZE;
         int numChunks = ChunkedByteBuffer.DEFAULT_NUMBER_OF_CHUNKS;
         if (fileLen > chunkSize * numChunks) {
            numChunks = new Long((fileLen / chunkSize) + 1).intValue();
         }
         ChunkedByteBuffer data = new ChunkedByteBuffer(chunkSize, numChunks);
         FileInputStream fis = new FileInputStream(dataFile);
         data.append(fis);
         fis.close(); // Release the file lock
         
         org.soundboard.audio.SoundPlayer sbPlayer = org.soundboard.audio.SoundPlayer.getInstance();
         String playInfo = sbPlayer.play(data, sentence);
         if (playInfo != null && !playInfo.equals("")) {
            out.append(playInfo);
            out.append("\n");
         }
      } catch (Exception e) {
         LoggingService.getInstance().serverLog("Error Saying \""+sentence+"\": "+ e.getMessage());
         LoggingService.getInstance().serverLog(e);
      }
      return out.toString();
   }
   
}
