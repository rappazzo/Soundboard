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

import java.io.*;
import java.net.*;
import java.util.*;
import org.soundboard.*;
import org.soundboard.server.*;
import org.soundboard.server.command.*;
import org.soundboard.util.*;
import com.levelonelabs.aim.*;

public class TelnetService extends InputService {

   //config keys
   public static final String PORT = "port";
   public static final String NEW_CONNECTION_HANDLER_CLASS = "newConnectionHandlerClass";
   public static final String MAX_CONNECTIONS = "maxConnections";
   
   public static final String SERVICE_NAME = "telnet";
   
   protected int port = -1;
   protected static boolean running = true;
   protected NewConnectionHandler newConnectionHandler = null;
   protected Integer maxConnections = null;
   
   private static Map<String, Socket> sessions = new HashMap<String, Socket>();

   int listenAttempts = 0;
   final int MAX_ATTEMPTS = 10;
   
   public TelnetService() {
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
      port = SoundboardConfiguration.config().getIntProperty(SoundboardConfiguration.INPUT, getServiceName(), PORT);
      newConnectionHandler = (NewConnectionHandler)SoundboardConfiguration.config().getClassProperty(SoundboardConfiguration.INPUT, getServiceName(), NEW_CONNECTION_HANDLER_CLASS);
      if (newConnectionHandler == null) {
         newConnectionHandler = new DefaultNewConnectionHandler();
      }
      maxConnections = SoundboardConfiguration.config().getIntegerProperty(SoundboardConfiguration.INPUT, getServiceName(), MAX_CONNECTIONS);
      return port > 0;
   }
   
   /**
    * send a message back to the
    */
   @Override public void send(String to, String message) {
      try {
         if (sessions.get(to) != null) {
            new PrintStream(sessions.get(to).getOutputStream()).print(message);
         }
      } catch (IOException e) {
         LoggingService.getInstance().serverLog("Error sending message in Telnet Service.  Message: " + message);
      }
   }

   /**
    * check if the given user is currently available on this service
    */
   @Override public boolean isAvailable(String userName) {
      return (sessions.get(userName) != null);
   }
   
   public boolean isRunning() {
      return running;
   }
   
   public void stopRunning() {
      running = false;
   }
      
   @Override public void run() {
      try {
         if (listenAttempts >= MAX_ATTEMPTS) {
            return;
         }
         listenAttempts += 1;
         
         ServerSocket listener = new ServerSocket(port);
         
         while (isRunning()) {
            try {
               Socket session = listener.accept();
               
               OutputStream out = session.getOutputStream();
               //if the max # of connections has been reached, notify the new client connection of that
               if (maxConnections != null && sessions.size() >= maxConnections.intValue()) {
                  new PrintStream(out).print("Max number of connections (" + maxConnections + ") has been reached, you may not connect.  Sorry...\n\r\n\r");
                  sleep(1250);
                  session.close();
               } else {
                  newConnectionHandler.connectionEstablished(session);

                  CommandListener c = new CommandListener(session, this);
                  c.start();
                  
                  sessions.put(session.getInetAddress().getHostAddress(), session);
               }
            }  catch (SocketTimeoutException e) {
               //ignore, this is the normal case when in polling mode for connections
            }  catch (Exception e) {
               //Debug.debug("Error accepting connection  " + e, Debug.WARNING);
            }
         }
      }  catch (BindException e) {
         try {
            port = port + 1;
            try {
               synchronized(this) {
                  this.wait(500);
               }
            }  catch (InterruptedException ignored) {
            }  catch (RuntimeException ignored) {
            }
            run();
         } catch (Exception ee) {
            //try one more time and then give up
            if (listenAttempts < MAX_ATTEMPTS) {
               listenAttempts = MAX_ATTEMPTS - 1;
               run();
            }
         }
      }  catch (Exception e) {
         //no-op
      }
   }
   
   
   public class CommandListener extends Thread {
      Socket session = null;
      Stoppable stoppable;
      
      public CommandListener(Socket session, Stoppable stoppable) {
         this.setName("Telnet Command Listener");
         this.session = session;
         this.stoppable = stoppable;
      }

      @Override public void run() {
         try {
            String client = session.getInetAddress().getHostName();
            BufferedReader in = new BufferedReader(new InputStreamReader(session.getInputStream()));
            PrintStream out = new PrintStream(session.getOutputStream());
            CommandHandler handler = new CommandHandler();
            out.print("[" + client + "]> ");
            String command;
            while (stoppable.isRunning() && (command = in.readLine()) != null) {
               if (command.length() > 0) {
                  if (command.equalsIgnoreCase("quit")) {
                     this.session.close();
                  } else {
                     handler.handleCommand(TelnetService.this, session.getInetAddress().getHostAddress(), command.split("\\s"));
                  }
               }
               out.print("[" + client + "]> ");
            }
         } catch (Exception e) {
            //in.close();
         }
      }
   }
   
}
