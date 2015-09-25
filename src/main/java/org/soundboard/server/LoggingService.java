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

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.soundboard.server.inputservice.InputService;
import org.soundboard.util.User;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class LoggingService {

   private static LoggingService INSTANCE = new LoggingService();

   private SetMultimap<String, User> subscribers = HashMultimap.create();
   private Map<String, InputService> services = new HashMap<String, InputService>();
   
   private LoggingService() {}

   public static LoggingService getInstance() {
      return INSTANCE;
   }

   public void stopService() {
   }

   /**
    * get the server log stream
    */
   public PrintStream getServerLog() {
      return System.out;
   }

   /**
    * log a message
    */
   public void serverLog(String message) {
      System.out.println(message);
   }

   /**
    * log a message
    */
   public void serverLog(Throwable e) {
      System.out.println("Exception in thread: " + Thread.currentThread().getName());
      e.printStackTrace();
   }

   /**
    * log a message
    */
   public void log(String message) {
      serverLog(message);
   }

   /**
    * log a message
    */
   public void relay(final String who, final String message) {
     Server.OFFILINE_WORKER.submit(
        new Runnable() {
           @Override public void run() {
              for (User user : subscribers.values()) {
                 if (user.getServiceInstance() != null) {
                    user.getServiceInstance().send(who, message);
                 }
              }
           }
        }
     );
   }

   /**
    * register an available input service
    */
   public void registerInputService(InputService service) {
      services.put(service.getClass().getName(), service);
      services.put(service.getServiceName(), service);
   }

   /**
    * get the current list of subscribers
    */
   public String getSubscribers() {
      StringBuilder buf = new StringBuilder();
      for (User subscriber : subscribers.values()) {
         buf.append("\n");
         buf.append(subscriber.getServiceId());
      }
      return buf.toString();
   }

   /**
    * subscribe to the log
    */
   public void subscribe(String id, String inputServiceName) {
      addSubscription(id, inputServiceName, subscribers, "Subscriber");
   }

   /**
    * subscribe to the log
    */
   public void unsubscribe(String id, String inputServiceName) {
      removeSubscription(id, inputServiceName, subscribers, "Subscriber");
   }

   /**
    * subscribe to the log
    */
   public void addSubscription(String id, String inputServiceName, SetMultimap<String, User> subscriberMap, String subscriberType) {
      subscriberMap.put(inputServiceName, new User(id, services.get(inputServiceName)));
      int dotIndex = inputServiceName.lastIndexOf(".");
      if (dotIndex >= 0) {
    	 inputServiceName = inputServiceName.substring(dotIndex + 1);
      }
      LoggingService.getInstance().serverLog("Adding " + subscriberType + " [" + id + "], service: " + inputServiceName);
   }

   /**
    * subscribe to the log
    */
   public void removeSubscription(String id, String inputServiceName, SetMultimap<String, User> subscriberMap, String subscriberType) {
      Set<User> subscribersWithThisService = subscriberMap.get(inputServiceName);
      if (subscribersWithThisService != null) {
         Iterator<User> it = subscribersWithThisService.iterator();
         while (it.hasNext()) {
            if (id.equals(it.next().getServiceId())) {
               it.remove();
               String shortServiceName = inputServiceName;
               int dotIndex = inputServiceName.lastIndexOf(".");
               if (dotIndex >= 0) {
                  shortServiceName = inputServiceName.substring(dotIndex + 1);
               }
               LoggingService.getInstance().serverLog("Removing " + subscriberType + " [" + id + "], service " + shortServiceName);
            }
         }
      }
   }
   
}
