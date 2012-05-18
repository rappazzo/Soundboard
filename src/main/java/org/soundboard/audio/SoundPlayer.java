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
package org.soundboard.audio;

import javax.sound.sampled.*;
import org.soundboard.library.*;
import org.soundboard.server.*;
import org.soundboard.util.*;

public class SoundPlayer {
   
   private static SoundPlayer INSTANCE = null;
   private Mixer device = null;
   private static boolean stopped = false;
   private static final ChunkedByteBuffer PAUSE = new ChunkedByteBuffer();
   static {
      //this is the wav file header
      //Offset   Size   Description      Value
      //------   ----   -----------      -----
      //0x00     4      Chunk ID         "RIFF" (0x52494646)
      //0x04     4      Chunk Data Size  (file size) - 8
      //0x08     4      RIFF Type        "WAVE" (0x57415645)
      PAUSE.append(new byte[] {0x52,0x49,0x46,0x46});
      PAUSE.append(new byte[] {(byte)0x3C,0x45,0x00,0x00});
      PAUSE.append(new byte[] {0x57,0x41,0x56,0x45});
      
      //Offset   Size   Description             Value
      //------   ----   -----------             -----
      //0x00     4      Chunk ID                "fmt " (0x666D7420)
      //0x04     4      Chunk Data Size          16 + extra format bytes
      //0x08     2      Compression code         1 - 65,535
      //0x0a     2      Number of channels       1 - 65,535
      //0x0c     4      Sample rate              1 - 0xFFFFFFFF
      //0x10     4      Average bytes/s          1 - 0xFFFFFFFF
      //0x14     2      Block align              1 - 65,535
      //0x16     2      Significant bits/sample  2 - 65,535
      //     -- OPTIONAL --
      //0x18     2      Extra format bytes       0 - 65,535
      //0x1a            Extra format bytes *
      PAUSE.append(new byte[] {0x66,0x6D,0x74,0x20});
      PAUSE.append(new byte[] {0x10,0x00,0x00,0x00});
      PAUSE.append(new byte[] {0x01,0x00});
      PAUSE.append(new byte[] {0x01,0x00});
      PAUSE.append(new byte[] {0x44,(byte)0xAC,0x00,0x00});
      PAUSE.append(new byte[] {(byte)0x88,(byte)0x58,0x01,0x00});
      PAUSE.append(new byte[] {0x02,0x00});
      PAUSE.append(new byte[] {0x10,0x00});
      
      //Offset   Length  Type    Description     Value
      //------   ------  ----    -----------     -----
      //0x00     4       char[4] chunk ID        "data" (0x64617461)
      //0x04     4       dword   chunk size      depends on sample length and compression
      //0x08            sample data
      PAUSE.append(new byte[] {0x64,0x61,0x74,0x61});
      PAUSE.append(new byte[] {0x18,0x45,0x00,0x00});
      //this is blank data which is a pause of about 0.2 seconds
      for (int i = 0; i < 4422; i++) {
         PAUSE.append(new byte[] {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00});
      }
   }
   
   protected SoundPlayer() {
   }
   
   public static SoundPlayer getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new SoundPlayer();
      }
      return INSTANCE;
   }
   
   public void initialize(Mixer device) {
      this.device = device;
   }
   
   /**
    * get an appropriate output stream for the given mixer and audio input stream
    */
   public AudioOutputStream getOutputStream(Mixer device, AudioInputStream audio) {
      return new AudioOutputStream(device, audio.getFormat());
   }
   
   /**
    * get the mixer device
    */
   public Mixer getDevice() {
      return device;
   }
   
   /**
    * play a sound from the defaul library
    * @return info text
    */
   public String play(String sound) {
      return play(SoundLibrary.getInstance(), sound);      
   }
   
   /**
    * stop playing all sounds which are currently playing
    */
   public void stop() {
      stopped = true;
   }
   
   /**
    * play a sound from the given library
    * @return info text
    */
   public String play(SoundLibrary library, String... sound) {
      StringBuilder result = new StringBuilder();
      if (device != null) {
         try {
            if (sound != null) {
               stopped = false;
               AudioFileFormat openStreamFormat = null;
               AudioOutputStream outputStream = null;
               for (String name : sound) {
                  ChunkedByteBuffer audio = library.getAudioData(name);
                  if (audio == null) {
                     if (result.length() > 0) {
                        result.append(", ");
                     }
                     result.append(name);
                     audio = PAUSE;
                     name = "PAUSE";
                  }
                  
                  AudioFileFormat format = AudioSystem.getAudioFileFormat(audio.toInputStream());
                  if (outputStream == null) {
                     outputStream = new AudioOutputStream(device, format.getFormat());
                  } else if (openStreamFormat != null && !openStreamFormat.getFormat().equals(format.getFormat()) ) {
                     outputStream.close();
                     outputStream = new AudioOutputStream(device, format.getFormat());
                     openStreamFormat = format;
                  }
                  play(audio, /*outputStream, */ name);
               }
               if (outputStream != null) {
                  outputStream.close();
               }
               if (result.length() > 0) {
                  result.insert(0, "Could not find ");
                  result.append(" in the library."); 
               }
            }
         } catch (Exception e) {
            result = new StringBuilder("***ERROR playing: " + e);
         }
      } else {
         result.append("Attempt to play a sound file before the device is specified");
      }
      if (result.length() > 0) {
         LoggingService.getInstance().serverLog(result.toString());
         return result.toString();
      }
      return null;
   }
   
   /**
    * play the given audio data in the chunked byte buffer
    */
   public String play(ChunkedByteBuffer data, String soundName) {
      String results = null;
      try {
         AudioOutputStream outputStream = getOutputStream(device, AudioSystem.getAudioInputStream(data.toInputStream()));
         results = play(data, outputStream, soundName);
         outputStream.close();
      } catch (Exception e) {
         LoggingService.getInstance().serverLog("Error playing " + soundName + ": ");
         e.printStackTrace(LoggingService.getInstance().getServerLog());
      }
      return results;
   }
   
   public void play0(ChunkedByteBuffer data, String soundName) {
      try {
         AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(data.toInputStream());
         try {
            Clip clip = AudioSystem.getClip(getDevice().getMixerInfo());
            clip.open(audioInputStream);
            try {
               clip.start();
               clip.drain();
            } finally {
               clip.close();
            }
         } finally {
            audioInputStream.close();
         }
      } catch (Exception e) {
         LoggingService.getInstance().serverLog("Error playing " + soundName + ": ");
         e.printStackTrace(LoggingService.getInstance().getServerLog());
      }
   }
   
   /**
    * play the given audio data in the chunked byte buffer
    * **** THIS METHOD DOES **NOT** CLOSE THE AUDIO OUTPUT STREAM ****
    */
   public String play(ChunkedByteBuffer data, AudioOutputStream outputStream, String soundName) {
      try {
         //uninterruptable:
         //data.writeTo(outputStream);
         
         int length = data.length();
         int chunkSize = data.getChunkSize();
         int stopChunk = length / chunkSize;
         int stopColumn = length % chunkSize;

         for (int i = 0; i < stopChunk && !stopped; i++) {
            outputStream.write(data.getChunk(i), 0, chunkSize);
         }

         if (stopColumn > 0 && !stopped) {
            outputStream.write(data.getChunk(stopChunk), 0, stopColumn);
         }
         outputStream.flush();
      } catch (Exception e) {
         LoggingService.getInstance().serverLog("Error playing " + soundName + ": ");
         e.printStackTrace(LoggingService.getInstance().getServerLog());
      }
      return null;
   }

}