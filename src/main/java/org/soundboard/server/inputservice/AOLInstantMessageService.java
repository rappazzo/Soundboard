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

import org.soundboard.server.*;
import com.levelonelabs.aim.*;

public class AOLInstantMessageService extends InputService implements Runnable, Stoppable {

   //config keys
   public static final String USERID = "userid";
   public static final String PASSWORD = "password";
   
   public static final String SERVICE_NAME = "aol";

   String path;
   boolean isLoggedIn = true;
   AIMClient aimPrimaryClient = null;

   public AOLInstantMessageService() {
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
         aimPrimaryClient = new AIMClient(userid, password, "Soundboard", true);
      }
      return canRun;
   }
   
   @Override
   public void send(String to, String message) {
      AIMClient client = getConnectedClient();
      AIMBuddy buddy = client.getBuddy(to);
      if (buddy != null && buddy.isOnline()) {
         getConnectedClient().sendMessage(buddy, message);
      }
   }

   /**
    * check if the given user is currently available on this service
    */
   @Override public boolean isAvailable(String userName) {
      AIMClient client = getConnectedClient();
      return (client.getBuddy(userName) != null);
   }

   public AIMClient getConnectedClient() {
      return aimPrimaryClient;
   }

   public void login() {
      aimPrimaryClient.signOn();
      loggedIn();
   }
   
   /**
    * logout of the sametime server
    */
   public void logout() {
      aimPrimaryClient.signOff();
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
      aimPrimaryClient.addAIMListener(new Listener());
   }

   public void loggedOut() {
      isLoggedIn = false;
   }
   
   class Listener extends AIMAdapter {
      public Listener() {
      }
      @Override public void handleMessage(AIMBuddy buddy, String request) {
         new MessageInterpreter(AOLInstantMessageService.this, buddy.getName(), request);
      }
   }

}