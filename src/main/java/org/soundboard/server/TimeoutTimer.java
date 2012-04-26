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
package org.soundboard.server;

/**
 * @author mrappazz
 *
 *
 **/
public abstract class TimeoutTimer extends Thread implements Stoppable {

   private long timeout;
   private long checkRate = 100;
   private long elapsed = 0;
   private boolean running = true;

   public TimeoutTimer(long timeout) {
      this.timeout = timeout;
   }

   public final boolean isRunning() {
      return running;
   }

   public final void stopRunning() {
      running = false;
   }

   @Override 
   public final void run() {
      while (running) {
         try {
            Thread.sleep(checkRate);
         } catch (InterruptedException ioe) {
            continue;
         }
         elapsed = elapsed + checkRate;
         //Check to see if the time has been exceeded
         if (running && elapsed > timeout) {
            running = false;
            timeout();
         }
      }
   }

   public abstract void timeout();

}
