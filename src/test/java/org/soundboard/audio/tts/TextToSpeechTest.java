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
package org.soundboard.audio.tts;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.Ignore;
import org.soundboard.server.SoundboardConfiguration;
import org.soundboard.util.ChunkedByteBuffer;

import junit.framework.TestCase;

public class TextToSpeechTest extends TestCase {

   @Ignore("It is annoying to run every build")
   public void testVoiceRssTTS() throws Exception {
	  SoundboardConfiguration.create("./bin/soundboard.properties");
      ChunkedByteBuffer speech = new VoiceRssTTS()
    		  .toSoundBytes("This is a test of the text to speech system.");

      //should be an mp3
      AudioInputStream in = AudioSystem.getAudioInputStream(speech.toInputStream());
      AudioFormat baseFormat = in.getFormat();
      AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
      AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, in);

      byte[] data = new byte[4096];
      SourceDataLine line = null;
      DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
      line = (SourceDataLine)AudioSystem.getLine(info);
      line.open(decodedFormat);
      if (line != null) {
         // Start
         line.start();
         int nBytesRead = 0;
         int nBytesWritten = 0;
         while (nBytesRead != -1) {
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
         line.close();
         din.close();
      }
   }

}
