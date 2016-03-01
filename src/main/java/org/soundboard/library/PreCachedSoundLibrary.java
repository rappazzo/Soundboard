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
package org.soundboard.library;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.soundboard.audio.tts.VoiceRssTTS;
import org.soundboard.server.LoggingService;
import org.soundboard.util.ChunkedByteBuffer;
import org.soundboard.util.ChunkedCharBuffer;

public class PreCachedSoundLibrary extends SoundLibrary {

   private Map<String, ChunkedByteBuffer> library = new TreeMap<String, ChunkedByteBuffer>();
   private Map<String, File> files = new HashMap<String, File>();

   /**
    * add a file to the library
    * return if the file was added to the library
    */
   @Override public boolean addFile(File source) {
      boolean added = false;
      if (source != null && !source.isDirectory()) {
         try {
            String shortName = source.getName().substring(0, source.getName().lastIndexOf(".")).toLowerCase();
            if (shouldAttemptLoad(source) && library.get(shortName) == null) {
               markAttempt(source);
               long fileLen = source.length();
               //use the file length to set the buffer size
               int chunkSize = ChunkedByteBuffer.DEFAULT_CHUNK_SIZE;
               int numChunks = ChunkedByteBuffer.DEFAULT_NUMBER_OF_CHUNKS;
               if (fileLen > chunkSize * numChunks) {
                  numChunks = new Long((fileLen / chunkSize) + 1).intValue();
               }
               ChunkedByteBuffer data;
               if (Pattern.compile("\\.txt$").matcher(source.getName()).find()) {
                  ChunkedCharBuffer text = new ChunkedCharBuffer(chunkSize, numChunks);
                  FileReader fr = new FileReader(source);
                  text.append(fr);
                  fr.close();

                  data = new VoiceRssTTS().toSoundBytes(text.toString());
               } else {
                  data = new ChunkedByteBuffer(chunkSize, numChunks);
                  FileInputStream fis = new FileInputStream(source);
                  data.append(fis);
                  fis.close(); // Release the file lock
               }

               //check that the data is valid audio data
               AudioInputStream audioStream = AudioSystem.getAudioInputStream(data.toInputStream());
               DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioStream.getFormat());
               try {
                  AudioSystem.getLine(info);
                  library.put(shortName, data);
                  files.put(shortName, source);
                  added = true;
                  LoggingService.getInstance().log("Added '" + shortName + "' into library" + (!getName().equals(SoundLibrarian.DEFAULT_LIBRARY) ? " " + getName() : ""));
               } catch (Exception e) {
                  LoggingService.getInstance().log("NOT Added '" + shortName + "': "+e.getMessage());
               }
            }
         } catch (Exception e) {
            LoggingService.getInstance().log("Unable to load '" + source.getAbsolutePath() + "' into library" + (!getName().equals(SoundLibrarian.DEFAULT_LIBRARY) ? " " + getName() : ""));
            LoggingService.getInstance().serverLog(e);
         }
      }
      return added;
   }

   /**
    * get the list of files in the library (lists the short name -- lookup key)
    */
   @Override
   public Set<String> list() {
      return library.keySet();
   }

   /**
    * get the data for the given short filename
    */
   @Override
   public ChunkedByteBuffer getData(String shortName) {
      return library.get(shortName.toLowerCase());
   }

   /**
    * get the inputstream for the given short filename
    */
   @Override
   public InputStream getDataStream(String shortName) {
      ChunkedByteBuffer data = library.get(shortName.toLowerCase());
      return data != null ? data.toInputStream() : null;
   }

   @Override public File getFile(String name) {
      return files.get(name);
   }

}
