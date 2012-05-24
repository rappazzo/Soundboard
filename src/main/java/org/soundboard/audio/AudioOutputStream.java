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

import java.io.*;
import javax.sound.sampled.*;

public class AudioOutputStream extends OutputStream implements LineListener {
   SourceDataLine line = null;

   public AudioOutputStream(Mixer mixer, AudioInputStream audio) {
      this(mixer, audio.getFormat());
   }

   public AudioOutputStream(Mixer mixer, AudioFormat audioFormat) {
      DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
      try {
         line = (SourceDataLine)mixer.getLine(info);
         line.open(audioFormat);
         line.start();
         line.addLineListener(this);
      } catch (Exception e) {
         e.printStackTrace();
         throw new IllegalStateException(e.getMessage(), e);
      }
   }

   @Override
   public void write(int b) throws IOException {
      write(new byte[]
         {(byte)b}, 0, 1);
   }

   @Override
   public void write(byte[] b, int off, int len) throws IOException {
      line.write(b, off, len);
   }
   
   @Override
   public void flush() throws IOException {
      line.drain();
      super.flush();
   }

   @Override
   public void close() throws IOException {
      line.close();
      super.close();
   }
   
   @Override public void update(LineEvent event) {
//      LoggingService.serverLog(event.toString());      
   }
   
}