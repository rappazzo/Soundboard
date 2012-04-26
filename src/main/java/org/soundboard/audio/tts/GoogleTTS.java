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
package org.soundboard.audio.tts;

import java.net.*;
import org.soundboard.util.*;

/**
 * 
 **/
public class GoogleTTS {
   
   public static ChunkedByteBuffer toSoundBytes(String text) {
      try {
         String encoded = URLEncoder.encode(text, "UTF-8"); //text.replace(' ', '+')
         URL url = new URL("http://translate.google.com/translate_tts?tl=en&ie=UTF-8&q="+encoded);
         URLConnection c = url.openConnection();
         c.addRequestProperty("User-Agent", "Mozilla/4.76");
         return new ChunkedByteBuffer().append(c.getInputStream());
      } catch (Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
         return null;
      }
   }

}
