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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import org.soundboard.library.SoundLibrarian;
import org.soundboard.library.SoundLibrary;
import org.soundboard.server.LoggingService;
import org.soundboard.server.SoundboardConfiguration;
import org.soundboard.util.ChunkedByteBuffer;

class JavaSoundPlayer extends SoundPlayer {
	
   static final JavaSoundPlayer INSTANCE = new JavaSoundPlayer();
   
   private final Mixer device;
   private Integer volume;
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
      //this is blank data which is a pause of about 0.1 seconds
      for (int i = 0; i < 2211; i++) {
         PAUSE.append(new byte[] {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00});
      }
   }
   
   protected JavaSoundPlayer() {
      this.volume = SoundboardConfiguration.config().getIntegerProperty("SoundPlayer.volume");
      this.device = getAudioPlaybackDevice(SoundboardConfiguration.config().getProperty(SoundboardConfiguration.AUDIO_PLAYBACK_DEVICE));
   }
   
   @Override public SoundPlayer setVolume(int volume) {
	   if (volume > 0) {
		   this.volume = volume;
	   }
	   return this;
   }
   
   /**
    * Get the Audio Playback Device with the given name.  If the device does not support playback, null is returned
    */
   public Mixer getAudioPlaybackDevice(String deviceName) {
      Mixer.Info[] mixerInfoData = AudioSystem.getMixerInfo();
      try {
         AudioFormat testFormat = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, (float)11025.0, 8, 1, 1, (float)11025.0, false);
         DataLine.Info info = new DataLine.Info(SourceDataLine.class, testFormat);
         if (mixerInfoData != null) {
            for (Mixer.Info mixerInfo : mixerInfoData) {
               Mixer mixer = AudioSystem.getMixer(mixerInfo);
               if (mixer.isLineSupported(info)) {
                  if (deviceName == null || mixerInfo.getName().equals(deviceName)) {
                     SourceDataLine dataline = null;
                     try {
                        dataline = (SourceDataLine)mixer.getLine(info);
                        dataline.open(testFormat);
                        dataline.start();
                     } catch (Exception e) {
                        if (dataline != null) {
                           try { dataline.close(); } catch (Exception ignored) {}
                        }
                        LoggingService.getInstance().log(" -- Unsupported Audio device: "+ mixerInfo.getName() + " Couldn't get line.info");
                        continue;
                     }
                     LoggingService.getInstance().log("Using Audio device: "+ mixerInfo.getName());
                     return mixer;
                  } else {
                     LoggingService.getInstance().log(" -- Undesired Audio device: "+ mixerInfo.getName());
                  }
               } else {
                  LoggingService.getInstance().log(" -- Unsupported Audio device: "+ mixerInfo.getName());
               }
            }
         }
      } catch (Exception e) {
         LoggingService.getInstance().log("ERROR trying to get Audio device " + e.getMessage());
      }
      LoggingService.getInstance().log("ERROR trying to get Audio device.  Could not find a supported device, or the desired device doesn't work.");
      System.exit(1);
      return null;
   }
   
   /**
    * get the mixer device
    */
   public Mixer getDevice() {
      return device;
   }
   
   /**
    * play a sound from the default library
    * @return info text
    */
   @Override
   public String play(String sound) {
      return play(SoundLibrarian.getInstance(), sound);
   }
   
   /**
    * stop playing all sounds which are currently playing
    */
   @Override
   public void stop() {
      stopped = true;
   }
   
   /**
    * play a sound from the given library
    * @return info text
    */
   @Override
   public String play(SoundLibrary library, String... sound) {
      StringBuilder result = new StringBuilder();
      if (device != null) {
         try {
            if (sound != null) {
               stopped = false;
               for (String name : sound) {
                  ChunkedByteBuffer audio = library.getData(name);
                  if (audio == null) {
                     if (result.length() > 0) {
                        result.append(", ");
                     }
                     result.append(name);
                     audio = PAUSE;
                     name = "PAUSE";
                  }
                  
                  play(audio, name);
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
   
   @Override
   public void play(ChunkedByteBuffer soundData, String name) {
      try {
         AudioInputStream in = AudioSystem.getAudioInputStream(soundData.toInputStream());
         AudioFormat baseFormat = in.getFormat();
         AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
         AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, in);
   
         byte[] data = new byte[4096];
         SourceDataLine line = null;
         DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
         line = (SourceDataLine)AudioSystem.getLine(info);
         line.open(decodedFormat);
         if (line != null) {
        	FloatControl volumeControl = null;
        	float previous = 1;
            if (this.volume != null) {
				volumeControl = (FloatControl)line.getControl(FloatControl.Type.MASTER_GAIN);
            	previous = volumeControl.getValue();
            	volumeControl.setValue(volume.floatValue());
            }
            // Start
            line.start();
            int nBytesRead = 0;
            int nBytesWritten = 0;
            while (nBytesRead != -1 && !stopped) {
               nBytesRead = din.read(data, 0, data.length);
               if (nBytesRead != -1) {
                  nBytesWritten = line.write(data, 0, nBytesRead);
               }
            }
            if (nBytesWritten == 0) {
            }
            // Stop
            line.drain();
            line.stop();
            if (volumeControl != null) {
            	volumeControl.setValue(previous);
            }
            line.close();
            din.close();
         }
      } catch (Exception e) {
         LoggingService.getInstance().serverLog("Error playing " + name + ": ");
         e.printStackTrace(LoggingService.getInstance().getServerLog());
      }
   }

}
