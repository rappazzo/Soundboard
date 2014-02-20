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

import org.soundboard.server.LoggingService;
import org.soundboard.util.Randomizer;

/**
 * 
 **/
public class OSxTTS extends TTS {
   
   private static final String[] VOICE = new String[] {
      "Agnes", "Kathy", "Princess", "Vicki", "Victoria", "Bruce", "Fred", "Junior", "Ralph"
   };
   
   @Override
   public String say(String text) {
      try {
         int voice = Randomizer.getRandomizer().nextInt(VOICE.length);
         ProcessBuilder procBuilder = new ProcessBuilder("say","-v", VOICE[voice], text);
         procBuilder.start();
         //don't wait
         return "I said \"" +text+"\"";
      } catch (Exception e) {
         LoggingService.getInstance().serverLog("Error Saying \""+text+"\": "+ e.getMessage());
         LoggingService.getInstance().serverLog(e);
         return null;
      }
   }

}
