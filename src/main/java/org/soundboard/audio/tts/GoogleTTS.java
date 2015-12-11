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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import org.soundboard.audio.SoundPlayer;
import org.soundboard.server.LoggingService;
import org.soundboard.util.ChunkedByteBuffer;

/**
 * 
 **/
public class GoogleTTS extends TTS {
   
   @Override
   public String say(String text) {
      try {
         ChunkedByteBuffer bytes = toSoundBytes(text);

         SoundPlayer sbPlayer = SoundPlayer.get();
         sbPlayer.play(bytes, text);
         return "I said \"" +text+"\"";
      } catch (Exception e) {
         LoggingService.getInstance().serverLog("Error Saying \""+text+"\": "+ e.getMessage());
         LoggingService.getInstance().serverLog(e);
         return null;
      }
   }

   public ChunkedByteBuffer toSoundBytes(String text) throws UnsupportedEncodingException, MalformedURLException, IOException {
      String encoded = URLEncoder.encode(text, "UTF-8"); //text.replace(' ', '+')
      URL url = new URL("http://translate.google.com/translate_tts?tl=en&ie=UTF-8&client=t&q="+encoded);
      URLConnection c = url.openConnection();
      c.setRequestProperty("Referer", "http://translate.google.com/");
      c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB;     rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 (.NET CLR 3.5.30729)");
      ChunkedByteBuffer bytes = new ChunkedByteBuffer().append(c.getInputStream());
      return bytes;
   }

}
