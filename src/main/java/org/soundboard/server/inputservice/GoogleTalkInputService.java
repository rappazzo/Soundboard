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
package org.soundboard.server.inputservice;

import java.util.*;


import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.soundboard.server.*;
import org.soundboard.server.command.*;

public class GoogleTalkInputService extends InputService implements Runnable, Stoppable {

   //config keys
   public static final String USERID = "userid";
   public static final String PASSWORD = "password";
   
   public static final String SERVICE_NAME = "google";

   String path;
   boolean isLoggedIn = true;
   XMPPConnection connection = null;

   public GoogleTalkInputService() {
      super();
   }
   
   /**
    * provide the service name
    */
   @Override public String getServiceName() {
      return SERVICE_NAME;
   }

   /**
    * initialize this service using the given configuration data
    * @return if this service can be run in it's current initialization state (ie, if there is enough info)
    *    For example if this service requires a user name and password, but one of them has not been
    * provided, this method will return false.
    */
   @Override public boolean initialize() {
      String userid = SoundboardConfiguration.config().getProperty(SoundboardConfiguration.INPUT, getServiceName(), USERID);
      String password = SoundboardConfiguration.config().getProperty(SoundboardConfiguration.INPUT, getServiceName(), PASSWORD);
      boolean canRun = userid != null && password != null;
      if (canRun) {
         connection = new XMPPConnection(new ConnectionConfiguration("talk.google.com", 5222, "gmail.com"));
         try {
            connection.connect();
            connection.login(userid, password);
         } catch (XMPPException e) {
            System.out.println("Error logging into Google Talk: ");
            e.printStackTrace();
         }
      }
      return canRun;
   }
   
   @Override
   public void send(String to, String message) {
      Message msg = new Message(to);
      msg.setBody(message);
      getClient().sendPacket(msg);
   }

   /**
    * check if the given user is currently available on this service
    */
   @Override public boolean isAvailable(String userName) {
      Roster list = getClient().getRoster();
      if (list.contains(userName)) {
         return list.getPresence(userName).isAvailable();
      } else {
         try {
            list.createEntry(userName, userName, null);
         } catch (XMPPException e) {
            System.out.println("Error adding "+ userName + " to Google Talk user list: ");
            e.printStackTrace();
         }
      }
      return false;
   }

   public XMPPConnection getClient() {
      return connection;
   }

   public void login() {
      try {
         getClient().connect();
         loggedIn();
      } catch (XMPPException e) {
         System.out.println("Error re-connecting to Google Talk: ");
         e.printStackTrace();
      }
   }
   
   /**
    * logout of the sametime server
    */
   public void logout() {
      connection.disconnect();
      loggedOut();
   }
   
   /**
    * return if this is running
    */
   public boolean isRunning() {
      return isLoggedIn;
   }
   
   /**
    * stop running
    */
   public void stopRunning() {
      logout();
   }

   /**
    *  run this thread
    */
   @Override public void run() {
      login();
      //Listen for incoming IMs
      try {
         while (isLoggedIn) {
            Thread.sleep(10000);
         }
      } catch (Exception e) {
         LoggingService.getInstance().serverLog("Error listening for messages");
      }
   }

   // LoginListener implementations
   public void loggedIn() {
      isLoggedIn = true;
      getClient().addPacketListener(new Listener(), null);
   }

   public void loggedOut() {
      isLoggedIn = false;
   }
   
   class Listener implements PacketListener {
      public Listener() {
      }
      public void processPacket(Packet packet) {
         if (packet instanceof Message) {
             Message msg = (Message) packet;
             new MessageInterpreter(GoogleTalkInputService.this, msg.getFrom(), msg.getBody());
         }
      }
   }
   
}