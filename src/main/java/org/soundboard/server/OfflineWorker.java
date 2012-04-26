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


public class OfflineWorker extends Thread {
   
   public static final int DEFAULT_PRIORITY = 7;
   
   OfflineTask task;
   
   /**
    * Constructor
    */
   public OfflineWorker(OfflineTask task) {
      this(task, DEFAULT_PRIORITY, true);
   }
   
   public OfflineWorker(OfflineTask task, boolean startImmediately) {
      this(task, DEFAULT_PRIORITY, startImmediately);
   }
   
   public OfflineWorker(OfflineTask task, int priority) {
      this(task, priority, false);
   }
   
   public OfflineWorker(OfflineTask task, int priority, boolean startImmediately) {
      this.setName("Offline Task: " + task.getClass().getSimpleName());
      this.task = task;
      this.setPriority(priority);
      if (startImmediately) {
         this.start();
      }
   }
   
   @Override
   public void run() {
      if (task != null) {
         task.doOfflineWork();
      }
   }
   
}
