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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.soundboard.server.command.Command;
import org.soundboard.server.command.CommandHandler;
import org.soundboard.server.inputservice.InputService;
import org.soundboard.util.ChunkedCharBuffer;
import org.soundboard.util.Randomizer;
import org.soundboard.util.StringUtil;
import org.soundboard.util.User;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class LoggingService implements Serializable {

   private static LoggingService INSTANCE = new LoggingService();

   private static final String FILE = "subscribers.txt";

   public static final String INTERLOPERS = "interlopers";
   public static final String FAKE_USERS = "fakeusers";
   public static final String CLIENT = "CLIENT:";

   private Thread subscriberThread = new SubscriberThread();
   private Queue<QueuedMessage> messageQueue = new LinkedList<QueuedMessage>();
   private SetMultimap<String, User> subscribers = HashMultimap.create();
   private SetMultimap<String, User> clients = HashMultimap.create();
   private Map<String, InputService> services = new HashMap<String, InputService>();
   private Set<String> interlopers = new HashSet<String>();
   private String[] fakeUsers = null;
   private boolean isClient = false;
   private boolean clientAck = false;
   private Object mutex = new Object();

   private LoggingService() {}

   public static LoggingService getInstance() {
      return INSTANCE;
   }

   private void clientAck() {
      clientAck = true;
   }

   public void setClient(User user, String connectionCommand) {
      serverLog("Trying to connect to soundboard server...");
      isClient = true;
      CommandHandler.register("ACKNOWLEDGED", new Command() {
         @Override
         public String getDescription() {
            return null;
         }
         @Override
         public String execute(InputService inputService, String who, String[] args, boolean isCron, boolean respondWithHtml) {
            clientAck();
            return null;
         }
      });
      //try to connect to the server.  Keep trying until it is done.
      try {
         while (!clientAck) {
            serverLog("Sending server connect command.");
            InputService serverService = user.getServiceInstance();
            serverService.send(user.getServiceId(), connectionCommand);
            Thread.sleep(10000);
         }
      } catch (Exception e) {
         //ignore - no ack;
      }
      CommandHandler.unregister("ACKNOWLEDGED");
      serverLog("Connected to " + user.getServiceId());
   }

   public void stopService() {
      try {
         subscriberThread.interrupt();
      } catch (Exception e) {
         serverLog("Exception stopping subscriber thread");
      }
   }

   public void setupDefence(String[] interloperList, String[] fakeUserList) {
      if (interloperList != null) {
         for (String who : interloperList) {
            interlopers.add(who.toLowerCase());
            serverLog("Preparing for INTERLOPER: " + who);
         }
      }
      if (fakeUserList != null && fakeUserList.length > 0 ) {
         fakeUsers = fakeUserList;
      }
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
      synchronized (mutex) {
         messageQueue.offer(new QueuedMessage(message));
      }
   }

   /**
    * log a message
    */
   public void relay(final String who, final String message) {
      if (!isClient) {
         Server.OFFILINE_WORKER.submit(
            new Runnable() {
               @Override public void run() {
                  for (User user : clients.values()) {
                     if (user.getServiceInstance() != null) {
                        user.getServiceInstance().send(who, message);
                     }
                  }
               }
            }
         );
      }
   }

   /**
    * log a message
    */
   public void privateMessage(String who, String message) {
      serverLog("Private Message to " + who + " -> " + message);
      synchronized (mutex) {
         messageQueue.offer(new QueuedMessage(message, who));
      }
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
    * add a client
    */
   public void addClient(String id, String inputServiceName) {
      addSubscription(id, inputServiceName, clients, "Client");
   }

   /**
    * add a client
    */
   public void removeClient(String id, String inputServiceName) {
      removeSubscription(id, inputServiceName, clients, "Client");
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

   /**
    * write the subscribers to disk
    */
   public void writeObject() {
      ChunkedCharBuffer buf = new ChunkedCharBuffer();
      for (Map.Entry<String, Collection<User>> entry : getInstance().subscribers.asMap().entrySet()) {
         String serviceName = entry.getKey();
         Collection<User> logToIds = entry.getValue();
         if (logToIds != null && logToIds.size() > 0) {
            buf.append(serviceName);
            buf.append("|");
            for (User logToId : logToIds) {
               buf.append(logToId.getServiceId());
               buf.append(";");
            }
            buf.append("\n");
         }
      }
      for (Map.Entry<String, Collection<User>> entry : getInstance().clients.asMap().entrySet()) {
         String serviceName = entry.getKey();
         Collection<User> logToIds = entry.getValue();
         if (logToIds != null && logToIds.size() > 0) {
            buf.append(CLIENT);
            buf.append(serviceName);
            buf.append("|");
            for (User logToId : logToIds) {
               buf.append(logToId.getServiceId());
               buf.append(";");
            }
            buf.append("\n");
         }
      }
      if (buf.size() > 0) {
         try {
            OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(FILE, false)));
            buf.writeTo(out);
            out.flush();
            out.close();
         } catch (IOException e) {
            getInstance().serverLog("Error writing subscribers: " + e);
         }
      }
   }

   /**
    * read the subscribers from disk
    */
   public void readObject() {
      try {
         if (new File(FILE).exists()) {
            ChunkedCharBuffer buf = new ChunkedCharBuffer();
            buf.append(new InputStreamReader(new FileInputStream(FILE)));
            String contents = buf.toString();
            String[] lines = contents.split("[\n\r]+");
            for (String line : lines) {
               if (line.startsWith(CLIENT)) {
                  line = line.substring(CLIENT.length());
                  String[] splitLine = line.split("\\|");
                  if (splitLine.length > 1) {
                     String serviceName = splitLine[0];
                     String[] ids = splitLine[1].split(";");
                     for (String id : ids) {
                        if (StringUtil.isNotNullOrEmpty(id)) {
                           getInstance().addClient(id, serviceName);
                        }
                     }
                  }
               } else {
                  String[] splitLine = line.split("\\|");
                  if (splitLine.length > 1) {
                     String serviceName = splitLine[0];
                     String[] ids = splitLine[1].split(";");
                     for (String id : ids) {
                        if (StringUtil.isNotNullOrEmpty(id)) {
                           getInstance().subscribe(id, serviceName);
                        }
                     }
                  }
               }
            }
         }
      } catch (IOException e) {
         getInstance().serverLog("Error reading subscribers: " + e);
      }
   }

   private class QueuedMessage {
      public String message;
      public String who;
      public QueuedMessage(String message) {
         this(message, null);
      }
      public QueuedMessage(String message, String who) {
         this.message = message;
         this.who = who;
      }
   }

   private class SubscriberThread extends Thread {
      Map<String, User> userIdToUser = new HashMap<String, User>();
      public SubscriberThread() {
         this.setName("Subscriber Queue Consumer");
         this.start();
      }
      private String defend(String who, String message) {
         if (interlopers.contains(who.toLowerCase())) {
            int colonIndex = message.indexOf(":");
            if (colonIndex > 0) {
               String newMessage;
               if (message.startsWith("cron")) {
                  newMessage = "cron" + message.substring(colonIndex);
               } else {
                  String fakeUser = who;
                  if (fakeUsers != null && fakeUsers.length > 0) {
                     fakeUser = fakeUsers[Randomizer.getRandomizer().nextInt(fakeUsers.length)];
                  }
                  newMessage = fakeUser + message.substring(colonIndex);
                  return newMessage;
               }
            }
         }
         return message;
      }
      @Override
      public void run() {
         try {
            while (true) {
               Thread.sleep(100);
               //consume all current messages and send them all at once to the each person
               Map<User, String> messagesTo = new HashMap<User, String>();
               while (messageQueue.peek() != null) {
                  QueuedMessage message;
                  synchronized (mutex) {
                     message = messageQueue.poll();
                  }
                  if (message.who != null) {
                     for (Map.Entry<String, InputService> entry : services.entrySet()) {
                        InputService service = entry.getValue();
                        if (service.isAvailable(message.who)) {
                           addToMessages(messagesTo, message.who, service, message.message);
                        }
                     }
                  } else {
                     for (User logToId : subscribers.values()) {
                        addToMessages(messagesTo, logToId.getServiceId(), logToId.getServiceInstance(), message.message);
                     }
                  }
               }
               try {
                  Iterator<Map.Entry<User, String>> it = messagesTo.entrySet().iterator();
                  while (it.hasNext()) {
                     Map.Entry<User, String> entry = it.next();
                     User user = entry.getKey();
                     if (user != null && user.getServiceInstance() != null) {
                        user.getServiceInstance().send(user.getServiceId(), entry.getValue());
                     }
                     it.remove();
                  }
               } catch (Exception e) {
                  if (e instanceof InterruptedException) {
                     throw (InterruptedException)e;
                  } else {
                     serverLog(e);
                  }
               }
            }
         } catch (InterruptedException e) {
            serverLog("Stopping Subscriber Thread");
         }
      }

      /**
       * add a new message to the list
       */
      private void addToMessages(Map<User, String> messagesTo, String who, InputService service, String newMessage) {
         User user = userIdToUser.get(who);
         if (user == null) {
            user = new User(who, service);
            userIdToUser.put(who, user);
         }
         String messages = messagesTo.get(user);
         if (messages == null) {
            messages = "";
         } else {
            messages = messages + "\n";
         }
         messages = messages + defend(who, newMessage);
         messagesTo.put(user, messages);
      }
   }

}
