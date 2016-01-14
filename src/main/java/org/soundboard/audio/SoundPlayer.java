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

import org.soundboard.library.SoundLibrarian;
import org.soundboard.library.SoundLibrary;
import org.soundboard.server.LoggingService;
import org.soundboard.server.SoundboardConfiguration;
import org.soundboard.util.ChunkedByteBuffer;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public abstract class SoundPlayer {
   
   static Supplier<SoundPlayer> INSTANCE = Suppliers.memoize(new Supplier<SoundPlayer>() {
      @Override public SoundPlayer get() {
         SoundPlayer lib = (SoundPlayer)SoundboardConfiguration.config().getClassProperty("SoundPlayer.impl");
         if (lib == null) {
            LoggingService.getInstance().serverLog("Error: MISSING SoundPlayer implementation ('SoundPlayer.impl')");
         }
         return lib;
      }
   });
   public static SoundPlayer get() {
      return INSTANCE.get();
   }
   public static SoundPlayer java() {
      return JavaSoundPlayer.INSTANCE;
   }
   
   /**
    * play a sound from the default library
    * @return info text
    */
   public String play(String sound) {
      return play(SoundLibrarian.getInstance(), sound);
   }
   
   /**
    * play a sound from the given library
    * @return info text
    */
   public abstract String play(SoundLibrary library, String... sound);
   
   /**
    * If this sound player supports playing bytes, play the given sound, otherwise, no-op
    * @param soundData
    * @param name
    */
   public abstract void play(ChunkedByteBuffer soundData, String name);
   
   /**
    * stop playing all sounds which are currently playing
    */
   public abstract void stop();
   
}
