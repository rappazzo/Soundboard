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

import java.math.*;
import org.soundboard.server.*;
import org.soundboard.server.command.*;
import com.lotus.sametime.community.*;
import com.lotus.sametime.core.comparch.*;
import com.lotus.sametime.core.constants.*;
import com.lotus.sametime.core.types.*;
import com.lotus.sametime.im.*;
import com.lotus.sametime.lookup.*;

public class SametimeInstantMessageService extends InputService implements LoginListener, Runnable, Stoppable {

   //config keys
   public static final String USERID = "userid";
   public static final String PASSWORD = "password";
   public static final String SERVER = "server";
   
   public static final String SERVICE_NAME = "sametime";

   STSession session;
   CommunityService community;
   String stUserId;
   String stPassword;
   String path;
   String stServerName;
   boolean isLoggedIn = false;
   InstantMessagingService imService;

   public SametimeInstantMessageService() {
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
      stUserId = SoundboardConfiguration.config().getProperty(SoundboardConfiguration.INPUT, getServiceName(), USERID);
      stPassword = SoundboardConfiguration.config().getProperty(SoundboardConfiguration.INPUT, getServiceName(), PASSWORD);
      stServerName = SoundboardConfiguration.config().getProperty(SoundboardConfiguration.INPUT, getServiceName(), SERVER);
      boolean canRun = stUserId != null && stPassword != null && stServerName != null;
      return canRun;
   }
   
   @Override
   public void send(String to, final String message) {
      LookupService lookupService = (LookupService)getSession().getCompApi(LookupService.COMP_NAME);;
      Resolver resolver = lookupService.createResolver(true, false, true, false);
      ResolveListener listener = new ResolveListener() {
         public void resolved(ResolveEvent re) {
            if (re.getResolved() instanceof STUser) {
               STUser user = ((STUser) re.getResolved());
               Im im = getImService().createIm(user, EncLevel.ENC_LEVEL_NONE, ImTypes.IM_TYPE_CHAT);
               im.sendText(true, message);
            }
         }
         public void resolveConflict(ResolveEvent arg0) {}
         public void resolveFailed(ResolveEvent arg0) {}
      };
      resolver.addResolveListener(listener);
      resolver.resolve(to);
      resolver.removeResolveListener(listener);
   }

   /**
    * check if the given user is currently available on this service
    */
   @Override public boolean isAvailable(String userName) {
      final BigDecimal available = new BigDecimal(-1);
      LookupService lookupService = (LookupService)getSession().getCompApi(LookupService.COMP_NAME);;
      Resolver resolver = lookupService.createResolver(true, false, true, false);
      ResolveListener listener = new ResolveListener() {
         public void resolved(ResolveEvent re) {
            if (re.getResolved() instanceof STUser) {
               STUser user = ((STUser) re.getResolved());
               available.negate();
            }
         }
         public void resolveConflict(ResolveEvent arg0) {}
         public void resolveFailed(ResolveEvent arg0) {}
      };
      resolver.addResolveListener(listener);
      resolver.resolve(userName);
      resolver.removeResolveListener(listener);
      return available.intValue() == 1;
   }
   
   /**
    *  Create a new session
    */
   public synchronized STSession getSession() {
      if (session == null) {
         try {
            STSession tmpSession = new STSession("Soundboard");
            tmpSession.loadComponents(new String[]{"com.lotus.sametime.community.STBase", "com.lotus.sametime.im.ImComp", "com.lotus.sametime.lookup.LookupComp"});
            tmpSession.start();
            session = tmpSession;
            getImService();
         } catch (DuplicateObjectException e) {
            // All ready exists so we are good
         }
      }
      return session;
   }

   public synchronized InstantMessagingService getImService() {
      if (imService == null) {
         imService = (InstantMessagingService)getSession().getCompApi(InstantMessagingService.COMP_NAME);
      }
      return imService;
   }

   public void login() {
      if (!isLoggedIn) {
         CommunityService community = (CommunityService)getSession().getCompApi(CommunityService.COMP_NAME);
         community.addLoginListener(this);
         community.loginByPassword(stServerName, stUserId, stPassword);
      }
   }
   
   /**
    * logout of the sametime server
    */
   public void logout() {
      try {
         community.logout();
      } catch (Exception e) {
      }

      try {
         session.stop();
      } catch (Exception e) {
      }

      try {
         session.unloadSession();
      } catch (Exception e) {
      }
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
      //List for incoming IMs
      try {
         while (isLoggedIn) {
            Thread.sleep(10000);
         }
      } catch (Exception e) {
         LoggingService.getInstance().serverLog("Error listening for messages");
      }
   }

   // LoginListener implementations
   public void loggedIn(LoginEvent loginEvent) {
      isLoggedIn = true;
      STUserInstance userInstance = loginEvent.getLogin().getMyUserInstance();
      STUserStatus status = loginEvent.getLogin().getMyStatus();

      imService.registerImType(ImTypes.IM_TYPE_CHAT);
      getImService().addImServiceListener(new Listener());
   }

   public void loggedOut(LoginEvent loginEvent) {
      isLoggedIn = false;
   }
   
   class Listener implements ImListener, ImServiceListener {
      public void dataReceived(ImEvent e) {}
      public void imOpened(ImEvent e) {}
      public void openImFailed(ImEvent e) {}
      
      public void imClosed(ImEvent e) {
//         e.getIm().removeImListener(this);
      }
      
      public void imReceived(ImEvent e) {
         e.getIm().addImListener(this);
      }
      
      public void textReceived(ImEvent event) {
         new MessageInterpreter(SametimeInstantMessageService.this, event.getIm().getPartner().getDisplayName(), event.getText());
      }
   
   }
   
}