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
package org.soundboard.util;

import java.util.*;
import org.soundboard.server.*;

public class Karma implements Stoppable {
   
   static interface Properties {
      public static final String THRESHOLD = "threshold";
      public static final String TRANSCENDENT = "transcendent";
   }

   private static Map<String, Integer> karma = new HashMap<String, Integer>();
   private static Set<String> transcendentUsers = new HashSet<String>();
   private static Karma INSTANCE = null;
   private static int threshold = -1; //0 or less is unlimited karma
   private static boolean monitoring = false;
   private Thread monitor = null;
   private static Object mutex = new Object();
   
   private Karma() {
   }
   
   /**
    * get the instance of karma
    */
   public static Karma getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new Karma();
      }
      return INSTANCE;
   }
   
   /**
    * initialize the karma instance
    */
   public void initialize(SoundboardConfiguration config) {
      threshold = config.getIntProperty(SoundboardConfiguration.KARMA, Properties.THRESHOLD);
      List<String> tUserProp = config.getMultiValuePropertyAsList(SoundboardConfiguration.KARMA, Properties.TRANSCENDENT);
      if (tUserProp != null) {
         transcendentUsers.addAll(tUserProp);
         LoggingService.getInstance().serverLog("Setting transcendant users (karma immune): " + transcendentUsers);
      }
   }

   /**
    * check the karma for the given user
    * @return if the user's karma is clear
    */
   public boolean checkKarma(String who) {
      if (transcendentUsers.contains(who)) {
         return true;
      } else {
         boolean clear = true;
         Integer whoKarmaInteger = karma.get(who);
         int whoKarma = whoKarmaInteger != null ? whoKarmaInteger.intValue() : 0;
         synchronized(mutex) {
            karma.put(who, Integer.valueOf(whoKarma + 1));
         }
         clear = (threshold <= 0 || whoKarma <= threshold);
         monitor();
         return clear;
      }
   }

   /**
    * check the karma for the given user
    * @return the current karma level for the given user
    */
   public int currentKarma(String who) {
      Integer whoKarma = karma.get(who);
      return whoKarma != null ? whoKarma.intValue() : 0;
   }

   /**
    * get the list of all currently karma'd users
    */
   public String currentKarma() {
      return currentKarma(true);
   }

   /**
    * get the list of all currently karma'd users.
    * @param displayAsNegative will list the karma values as negative
    */
   public String currentKarma(boolean displayAsNegative) {
      StringBuilder buf = new StringBuilder();
      for (Map.Entry<String, Integer> entry : karma.entrySet()) {
         Integer whoKarma = entry.getValue();
         if (whoKarma != null && whoKarma.intValue() != 0) {
            buf.append("\n");
            buf.append(entry.getKey());
            buf.append(" (");
            if (displayAsNegative) {
               buf.append("-");
            }
            buf.append(whoKarma);
            buf.append(")");
         }
      }
      return buf.toString();
   }
   
   /**
    * monitor the karmas
    */
   private void monitor() {
      if (monitor == null || !monitoring) {
         monitor = new Monitor();
      }
   }
   
   @Override public boolean isRunning() {
      return monitoring;
   }
   
   @Override public void stopRunning() {
      if (this.monitor != null && monitoring) {
         monitoring = false;
         try {
            monitor.interrupt();
         } catch (Exception e) {
            LoggingService.getInstance().serverLog("Error releasing the server: ");
            e.printStackTrace(LoggingService.getInstance().getServerLog());
         }
      }
   }
   
   class Monitor extends Thread {
      public Monitor() {
         this.setName("Karma Monitor");
         //only start this thread if we are not already actively monitoring
         if (!monitoring) {
            this.start();
         }
      }
      @Override
      public void run() {
         try {
            monitoring = true;
            while (monitoring && karma.size() > 0) {
               //karma reduces once a minute
               Thread.sleep(60 * 1000);
               synchronized(mutex) {
                  Iterator<Map.Entry<String, Integer>> it = karma.entrySet().iterator();
                  while (it.hasNext()) {
                     Map.Entry<String, Integer> entry = it.next();
                     Integer value = entry.getValue();
                     int intValue = value.intValue();
                     if (--intValue > 0) {
                        entry.setValue(Integer.valueOf(intValue));
                     } else {
                        //remove from the list if their karma is now clear
                        it.remove();
                     }
                  }
               }
            }
         } catch (InterruptedException e) {
            //abrupt stop of the karma monitoring
         }
         //once we fall out of the monitoring
         monitoring = false;
      }
   }
   
}
