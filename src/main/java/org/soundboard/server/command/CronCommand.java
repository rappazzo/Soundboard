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

import java.text.*;
import java.util.*;
import org.soundboard.server.*;
import org.soundboard.server.inputservice.*;
import org.soundboard.util.*;

public class CronCommand extends Command {

   public static final long MINUTE = 60 * 1000;
   public static final long HOUR = 60 * MINUTE;
   public static final long DAY = 24 * HOUR;
   public static final long WEEK = 7 * DAY;
   public static final long BIWEEK = 2 * WEEK;
   
   //make this a property in the future
   public static final long MIN_INTERVAL = 10 * MINUTE;
   
   //interval words
   public static final String ONCE = "once";
   public static final String HOURLY = "hourly";
   public static final String DAILY = "daily";
   public static final String WEEKLY = "weekly";
   public static final String BIWEEKLY = "biweekly";
   
   //time words
   public static final String NOW = "now";
   
   //parsing separators
   public static final String ID = ":";
   public static final String AT = "@";
   
   /**
    * return if this command is cronable
    */
   @Override public boolean isCronable() {
      return false;
   }
   
   /**
    * return if this command is able to be used in http
    */
   @Override public boolean isHttpCommand() {
      return false;
   }
   
   /**
    * get the command description
    */
   @Override public String getDescription() {
      return
         "Informs the hampsters living inside the soundboard to issue commands on your behalf.  " +
         "Usage =>  label: soundboard_command  {once/hourly/daily/weekly/biweekly | # of minutes} @ {MM/DD/YYYY HH:MM:SS | now | +/-minutes from now} " +
         "Examples:  \"schedule hourlyfart: fart 60 @ now\", " +
         "\"schedule schoobell: play schoolbell daily @ 12:00\"" +
         "";
   }

   /**
    * execute the command with the given arguements.  Note that args[0] is the name of the command.
    */
   @Override public String execute(InputService inputService, String who, String[] args, boolean isCron, boolean respondWithHtml) {
      StringBuffer out = new StringBuffer();
      StringBuffer command = new StringBuffer();
      String commandType = null;
      String identifier = null;
      String intervalString = null;
      boolean commandParsed = false;
      try {
         int i = 1;
         for (; !commandParsed; i++) {
            if (args[i+1].equals(AT)) {
               intervalString = args[i];
               commandParsed = true;
            } else if (args[i].endsWith(ID)) {
               identifier = args[i];
               //strip off the ID indicator
               identifier = identifier.substring(0, identifier.length() - 1);
            } else {
               if (commandType == null) {
                  commandType = args[i];
               }
               if (command.length() > 0) {
                  command.append(" ");
               } else {
                  //look for funny business (scheduling a cron to set a cron)
                  if (args[i].equals(args[0])) {
                     LoggingService.getInstance().serverLog(" ==> " + who + " is trying to cron a cron <== ");
                     throw new IllegalStateException("You cannot schedule a command to schedule a command.  Are you trying to break the system?  Your break attempt has been noted in the logs.");
                  }
               }
               command.append(args[i]);
            }
         }
         Command cmdObject = new CommandHandler().getCommand(commandType);
         if (cmdObject != null && cmdObject.isCronable()) {
            if (identifier == null) {
               identifier = who + ":" + new SimpleDateFormat("yyyy-MM-dd@hh:mm:ss.SSS").format(new Date());
            }
            Long interval = null;
            if (ONCE.equals(intervalString)) {
               interval = new Long(0);
            } else if (HOURLY.equals(intervalString)) {
               interval = new Long(HOUR);
            } else if (DAILY.equals(intervalString)) {
               interval = new Long(DAY);
            } else if (WEEKLY.equals(intervalString)) {
               interval = new Long(WEEK);
            } else if (BIWEEKLY.equals(intervalString)) {
               interval = new Long(BIWEEK);
            } else {
               //assume that a number is passed
               try {
                  long parsedInterval = Long.valueOf(intervalString).longValue();
                  //convert the interval to minutes
                  interval = new Long(parsedInterval * MINUTE);
                  if (interval.longValue() < MIN_INTERVAL) {
                     LoggingService.getInstance().serverLog(" ==> " + who + " is trying to schedule an event every " + StringUtil.formatTimeAmount(interval) + " <== ");
                     throw new IllegalStateException("That interval seems awefully low, please try more time between events.  Remember that the interval is specified in milliseconds");
                  }
               } catch (NumberFormatException e) {
                  out.append("Unable to determine the interval, defaulting to " + ONCE + ".  ");
                  interval = new Long(0);
               }
            }
            i++; //move past the '@'
            StringBuilder timeString = new StringBuilder();
            for (; i < args.length; i++) {
               //look for the special case "now"
               if (i + 1 == args.length && NOW.equals(args[i])) {
                  timeString.append("+0");
               } else {
                  timeString.append(args[i]);
                  if (i + 1 < args.length) {
                     timeString.append(" ");
                  }
               }
            }
            Date time;
            if (timeString.charAt(0) == '+' || timeString.charAt(0) == '-') {
               long offset = 0;
               try {
                  offset = new Long(timeString.substring(1)).longValue();
                  if (timeString.charAt(0) == '-') {
                     if (interval.longValue() == 0) {
                        throw new IllegalStateException("You cannot set a job to run in the past with no interval.");
                     }
                     offset *= -1;
                  }
                  offset = offset == 0 ? 5000 : offset * MINUTE;
               } catch (NumberFormatException e) {
                  //ignore - default to 0
               }
               time = new Date(System.currentTimeMillis() + offset);
            } else {
               try {
                  time = StringUtil.parseDate(timeString.toString()).getTime();
               } catch (ParseException e) {
                  throw new IllegalStateException("Unable to determine when to schedule the event");
               }
            }
            
            CronJob job = new CronJob(identifier, interval, time, command.toString(), who, inputService.getClass().getName());
            out.append(CronService.scheduleJob(job));
         } else {
            out.append("\"" + command.toString() + "\" cannot be scheduled.");
         }
      } catch (IndexOutOfBoundsException e) {
         out.append("No time specified, or time indicator, \"@\", missing.");
      } catch (IllegalStateException e) {
         out.append(e.getMessage());
      }
      return out.toString();
   }
   
}
