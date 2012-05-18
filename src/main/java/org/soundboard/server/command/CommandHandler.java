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
package org.soundboard.server.command;

import java.util.*;
import org.soundboard.server.*;
import org.soundboard.server.inputservice.*;
import org.soundboard.util.*;

public final class CommandHandler {

   public static final String HELP = "help";
   private static Map<String, Command> registry = new TreeMap<String, Command>();
   private static Map<String, String> shortCuts = new HashMap<String, String>();
   private static Map<String, String> reverseShortCutLookup = new HashMap<String, String>();
   private static Set<String> clientUsers = new HashSet<String>();
   
   /**
    * Constructor
    */
   public CommandHandler() {
   }
   
   /**
    * 
    */
   public static void registerClient(String client) {
      clientUsers.add(client);
   }
   
   /**
    * register all of the commands from the configuration
    */
   public static void registerCommands() {
      SoundboardConfiguration config = SoundboardConfiguration.config();
      String[] commands = config.getMultiValueProperty(SoundboardConfiguration.COMMAND, SoundboardConfiguration.NAMES);
      for (String commandName : commands) {
         String shortcut = config.getProperty(SoundboardConfiguration.COMMAND, commandName, SoundboardConfiguration.SHORTCUT);
         Command command = (Command)config.getClassProperty(SoundboardConfiguration.COMMAND, commandName, SoundboardConfiguration.CLASS_NAME);
         if (command != null) {
            LoggingService.getInstance().serverLog("Registering command [" + commandName + "]");
            register(commandName, shortcut, command);
         } else {
            LoggingService.getInstance().serverLog("*** Unable to register command [" + commandName + "] *** ");
         }
      }
   }
   
   /**
    * register commands to their handlers
    */
   public static void register(String commandName, Command command) {
      register(commandName, null, command);
   }
   
   /**
    * register commands to their handlers
    */
   public static void register(String commandName, String shortCut, Command command) {
      registry.put(commandName.toLowerCase(), command);
      if (StringUtil.isNotNullOrEmpty(shortCut)) {
         shortCuts.put(shortCut.toLowerCase(), commandName.toLowerCase());
         reverseShortCutLookup.put(commandName.toLowerCase(), shortCut.toLowerCase());
      }
   }
   
   /**
    * remove a command handler from the registry
    */
   public static void unregister(String commandName) {
      registry.remove(commandName.toLowerCase());
      LoggingService.getInstance().serverLog("UNREGISTERING Command [" + commandName +"]");
   }
   
   /**
    * get the command registry
    */
   public static Map<String, Command> getRegistry() {
      return Collections.unmodifiableMap(registry);
   }
   
   /**
    * get the command registry
    */
   public static String getShortcutFor(String commandInvocation) {
      return reverseShortCutLookup.get(commandInvocation.toLowerCase());
   }
   
   /**
    * get the command for the given string (could be a shortcut)
    */
   public Command getCommand(String commandInvokation) {
      Command command = registry.get(commandInvokation.toLowerCase());
      if (command == null) {
         String fullInvokationName = shortCuts.get(commandInvokation.toLowerCase());
         if (fullInvokationName != null) {
            command = registry.get(fullInvokationName.toLowerCase());
         }
      }
      return command;
   }
   
   /**
    * get a list of commands and their descriptions
    */
   public String getCommandHelpText() {
      StringBuilder buf = new StringBuilder();
      if (registry.size() > 0) {
         for (Map.Entry<String, Command> commands : registry.entrySet()) {
            String cmdInvokation = commands.getKey();
            Command cmd = commands.getValue();
            String helpText = cmd.getDescription();
            if (helpText != null) {
               if (buf.length() == 0) {
                  buf.append("Available Commands (shortcut in []):");
               }
               buf.append("\r\n");
               buf.append(cmdInvokation);
               String shortCut = reverseShortCutLookup.get(cmdInvokation);
               if (shortCut != null) {
                  buf.append("[");
                  buf.append(shortCut);
                  buf.append("]");
               }
               cmdInvokation.length();
               buf.append(" --> ");
               buf.append(helpText);
            }
         }
      }
      if (buf.length() <= 0) {
         buf.append("Sorry, no help is available");
      }
      return buf.toString();
   }

   /**
    * handle the commands
    */
   public String handleCommand(InputService inputService, String who, String[] commandAndArgs) {
      return handleCommand(inputService, who, commandAndArgs, false, false);
   }

   /**
    * handle the commands
    */
   public String handleCommand(InputService inputService, String who, String[] commandAndArgs, boolean isCron, boolean respondWithHtml) {
      StringBuffer out = new StringBuffer();
      if (commandAndArgs[0].charAt(0) == '?' || commandAndArgs[0].equalsIgnoreCase(HELP)) {
         SoundboardConfiguration config = SoundboardConfiguration.config();
         boolean helpEnabled = !Boolean.valueOf(config.getProperty(SoundboardConfiguration.COMMAND, HELP, SoundboardConfiguration.DISABLED)).booleanValue();
         if (helpEnabled) {
            out.append(getCommandHelpText());
         }
      } else {
         Command command = getCommand(commandAndArgs[0]);
         if (command != null) {
            //cron commands shouldn't count against your karma - change this if someone figures out this loophole
            if (!isCron && command.affectsKarma() && !Karma.getInstance().checkKarma(who)) {
               out.append("Sorry, you have bad Karma.  You will have to wait a few minutes to use the soundboard.\r\n");
               LoggingService.getInstance().serverLog("Bad Karma for " + who);
            } else {
               if (!clientUsers.contains(who)) {
                  LoggingService.getInstance().relay(StringUtil.join(commandAndArgs, " "));
               }
               String cmdInfo = command.execute(inputService, who, commandAndArgs, isCron, respondWithHtml);
               History.addHistory(who+"@"+inputService.getServiceName(), StringUtil.join(commandAndArgs, " "));
               if (cmdInfo != null && !cmdInfo.equals("")) {
                  out.append(cmdInfo);
                  out.append("\r\n");
               }
            }
         } else {
            LoggingService.getInstance().serverLog("Unknown command -> " + who + ": " + StringUtil.join(commandAndArgs, " "));
         }
      }
      return out.toString();
   }
   
}
