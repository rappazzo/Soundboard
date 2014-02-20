package org.soundboard.audio.tts;

import org.soundboard.server.LoggingService;
import org.soundboard.server.SoundboardConfiguration;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public abstract class TTS {
   private static Supplier<TTS> INSTANCE = Suppliers.memoize(new Supplier<TTS>() {
      @Override public TTS get() {
         TTS tts = (TTS)SoundboardConfiguration.config().getClassProperty("TTS.impl");
         if (tts == null) {
            LoggingService.getInstance().serverLog("Warning: MISSING TTS implementation ('TTS.impl')");
            tts = new TTS() {
               @Override public String say(String text) {
                  return "I don't know how to talk";
               }
            };
         }
         return tts;
      }
   });
   public static TTS get() {
      return INSTANCE.get();
   }

   /**
    * Say the given text
    * @return 'I said "<text>"' if it was successful
    */
   public abstract String say(String text);

}
