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
public final class TimeoutTask {

   private OfflineTask task;
   private long timeout;
   private Object monitor = new Object();
   private Thread mainThread = null;

   /**
    * Constructors
    */
   public TimeoutTask(OfflineTask task, long timeout) {
      this(task, timeout, false);
   }

   public TimeoutTask(OfflineTask task, long timeout, boolean startImmediately) {
      this.task = task;
      this.timeout = timeout;
      if (startImmediately) {
         doWork();
      }
   }

   public void doWork() {
      mainThread = Thread.currentThread();
      TimeoutTimer timeoutTimer = new TimeoutTimer(timeout) {
         @Override public void timeout() {
            mainThread.interrupt();
         }
      };
      OfflineWorker worker = new OfflineWorker(task, false);
      timeoutTimer.start();
      worker.start();
      try {
         synchronized (monitor) {
            while (timeoutTimer.isRunning()) {
               monitor.wait();
            }
         }
      } catch (InterruptedException e) {
         worker.interrupt();
      }
   }

}
