package org.soundboard.audio;

import java.io.InputStreamReader;
import java.util.List;

import org.soundboard.util.CharsetConstants;
import org.soundboard.util.ChunkedCharBuffer;

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class SpotifyViaBash {

   // future: music player interface
   private static final SpotifyViaBash INSTANCE = new SpotifyViaBash();

   public static SpotifyViaBash getInstance() {
      return INSTANCE;
   }

   private SpotifyViaBash() {
   }

   public static final Supplier<Boolean> ENABLED = Suppliers.memoize(new Supplier<Boolean>() {
      @Override
      public Boolean get() {
         boolean enabled = false;
         ProcessBuilder procBuilder = new ProcessBuilder();
         procBuilder.command("bash", "-c", "spotify");
         try {
            Process proc = procBuilder.start();
            proc.waitFor();
            enabled = proc.exitValue() == 0;
         } catch (Exception ignore) {
         }
         return Boolean.valueOf(enabled);
      }
   });

   /**
    * This command only passes along the underlying spotify command and returns
    * the result
    */
   public String send(List<String> args) {
      if (ENABLED.get().booleanValue()) {
         ProcessBuilder procBuilder = new ProcessBuilder();
         StringBuilder command = new StringBuilder();
         command.append("spotify ");
         if (args != null && !args.isEmpty()) {
            command.append(Joiner.on(' ').join(args));
         }
         procBuilder.command("bash", "-c", command.toString());
         // lazy way to include all output
         procBuilder.redirectErrorStream(true);
         try {
            Process proc = procBuilder.start();
            proc.waitFor();
            ChunkedCharBuffer responseBuffer = new ChunkedCharBuffer();
            responseBuffer.append(new InputStreamReader(proc.getInputStream(), CharsetConstants.UTF8));
            //remove bash control color codes
            String response = responseBuffer.toString();
            if (response.startsWith("Usage")) {
               int reindex = response.indexOf("Commands:") + 10;
               if (reindex > 0) {
                  response = response.substring(reindex);
               }
            }
            return response;
         } catch (Exception ignore) {
         }
      } else {
    	  return "Spofity is not enabled";
      }
      return null;
   }

}
