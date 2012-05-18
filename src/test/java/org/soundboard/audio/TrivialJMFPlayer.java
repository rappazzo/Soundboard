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

import javax.media.*;
import java.io.File;
import java.awt.*;

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

   public TrivialJMFPlayer() throws java.io.IOException, java.net.MalformedURLException, javax.media.MediaException {
      FileDialog fd = new FileDialog(this, "TrivialJMFPlayer", FileDialog.LOAD);
      fd.setVisible(true);
      File f = new File(fd.getDirectory(), fd.getFile());
      Player p = Manager.createRealizedPlayer(f.toURI().toURL());
      Component c = p.getVisualComponent();
      add(c);
      p.start();
   }
}