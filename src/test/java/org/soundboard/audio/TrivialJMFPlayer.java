/***
 **  @(#) TradeCard.com 1.0
 **
 **  Copyright (c) 2012 TradeCard, Inc. All Rights Reserved.
 **
 **
 **  THIS COMPUTER SOFTWARE IS THE PROPERTY OF TradeCard, Inc.
 **
 **  Permission is granted to use this software as specified by the TradeCard
 **  COMMERCIAL LICENSE AGREEMENT.  You may use this software only for
 **  commercial purposes, as specified in the details of the license.
 **  TRADECARD SHALL NOT BE LIABLE FOR ANY  DAMAGES SUFFERED BY
 **  THE LICENSEE AS A RESULT OF USING OR MODIFYING THIS SOFTWARE IN ANY WAY.
 **
 **  YOU MAY NOT DISTRIBUTE ANY SOURCE CODE OR OBJECT CODE FROM THE TradeCard.com
 **  TOOLKIT AT ANY TIME. VIOLATORS WILL BE PROSECUTED TO THE FULLEST EXTENT
 **  OF UNITED STATES LAW.
 **
 **  @version 1.0
 **  @author Copyright (c) 2012 TradeCard, Inc. All Rights Reserved.
 **
 **/
package org.soundboard.audio;

import java.awt.*;
import java.io.*;
import javax.media.*;
import javax.sound.sampled.*;

public class TrivialJMFPlayer extends Frame {

   public static void main(String[] args) {
      try {
         Frame f = new TrivialJMFPlayer();
         f.pack();
         f.setVisible(true);
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }
   }

   public TrivialJMFPlayer() throws Exception {
      FileDialog fd = new FileDialog(this, "TrivialJMFPlayer", FileDialog.LOAD);
      fd.setVisible(true);
      File file = new File(fd.getDirectory(), fd.getFile());

      AudioInputStream in = AudioSystem.getAudioInputStream(file);
      AudioInputStream din = null;
      AudioFormat baseFormat = in.getFormat();
      AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
      din = AudioSystem.getAudioInputStream(decodedFormat, in);
      // Play now.
      rawplay(decodedFormat, din);
      in.close();
      Player p = Manager.createRealizedPlayer(file.toURI().toURL());
      Component c = p.getVisualComponent();
      add(c);
      p.start();
   }
   
   private void rawplay(AudioFormat targetFormat, AudioInputStream din) throws IOException,                                                                                                LineUnavailableException
   {
     byte[] data = new byte[4096];
     SourceDataLine line = getLine(targetFormat);
     if (line != null)
     {
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
       if (nBytesWritten == 0) {}
       // Stop
       line.drain();
       line.stop();
       line.close();
       din.close();
     }
   }

   private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException
   {
     SourceDataLine res = null;
     DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
     res = (SourceDataLine) AudioSystem.getLine(info);
     res.open(audioFormat);
     return res;
   }
   
}