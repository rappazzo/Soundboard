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
import org.soundboard.server.SoundboardConfiguration;
import org.soundboard.util.ChunkedByteBuffer;

/**
 * @see http://www.voicerss.org/api/documentation.aspx
 **/
public class VoiceRssTTS extends TTS {
	
	public static final String API_KEY = "api-key";

	String apiKey;
	
	public VoiceRssTTS() {
		this.apiKey = SoundboardConfiguration.config().getProperty("TTS", API_KEY);
	}
   
   @Override
   public String say(String text) {
      try {
         ChunkedByteBuffer bytes = toSoundBytes(text);
         
         //Since this instance is playing WAV data (see the request to VoiceRSS.org) the 
         //java player may work better for a byte stream 
         SoundPlayer sbPlayer = SoundPlayer.java();
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
      String language = "en-us";
      URL url = new URL(String.format("http://api.voicerss.org/?key=%s&src=%s&hl=%s&r=0&c=WAV&f=16khz_16bit_stereo", apiKey, encoded, language));
      URLConnection c = url.openConnection();
      ChunkedByteBuffer bytes = new ChunkedByteBuffer().append(c.getInputStream());
      return bytes;
   }

}
