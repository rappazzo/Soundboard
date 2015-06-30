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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;
import org.soundboard.server.command.CommandHandler;
import org.soundboard.server.command.CronCommand;
import org.soundboard.util.StringUtil;

public class CronJob {

   public static final Long NO_INTERVAL = new Long(0);

   String identifier;
   Long interval;
   Date startTime;
   String commandString;
   String user;
   String userService;

   /**
    * constructor for a repeating job
    */
   public CronJob(String identifier, Long interval, Date startTime, String commandString, String user, String userService) {
      this.identifier = identifier;
      this.interval = interval != null ? interval : NO_INTERVAL;
      this.startTime = startTime;
      this.commandString = commandString;
      this.user = user;
      this.userService = userService;
   }

   /**
    * isOneTime
    */
   public boolean isOneTime() {
      return interval.longValue() == NO_INTERVAL.longValue();
   }

   public String getIdentifier() {
      return identifier;
   }

   public String getCommandString() {
      return commandString;
   }

   public Date getStartTime() {
      return startTime;
   }

   public Long getInterval() {
      return interval;
   }

   public String getUser() {
      return user;
   }

   public String getUserService() {
      return userService;
   }

   public boolean isSchedulable() {
      if (getInterval() != null && getStartTime() != null && getCommandString() != null) {
         //it is only schedulable if the start time is after the current time (unless there is an interval)
         return (getInterval().longValue() > 0 || getStartTime().after(new Date()));
      }
      return false;
   }

   public void reschedule() {
      long now = System.currentTimeMillis();
      Date originalScheduledTime = getStartTime();
      long startTime = originalScheduledTime.getTime();
      long interval = getInterval().longValue();
      if (interval > 0) {
         while (startTime < now) {
            startTime = startTime + interval;
         }
         Date adjusted = new Date(startTime);
         //if the interval is divisible by a DAY, then adjust for daylight savings
         if (interval % CronCommand.DAY == 0) {
            //Next fix attempt, use TimeZone.getOffset()
            //in spring, this is > 0; in fall, this is < 0
            int dst = adjusted.getTimezoneOffset() - originalScheduledTime.getTimezoneOffset();
            if (dst != 0) {
               adjusted = new Date(startTime + (dst * 60 * 1000));
            }
         }
         this.startTime = adjusted;
      }
   }

   /**
    * get the job for this
    */
   public TimerTask getJob() {
      return new TimerTask() {
         @Override
         public void run() {
            new CommandHandler().handleCommand(Server.getInputService(userService), user, commandString.split(" "), true, false);
         }
      };
   }

   @Override
   public String toString() {
      StringBuilder job = new StringBuilder();
      if (identifier != null) {
         job.append(identifier);
         job.append(": ");
      }
      job.append("\"");
      job.append(commandString);
      job.append("\"");

      if (!isOneTime()) {
         job.append(" Every ");
         job.append(StringUtil.formatTimeAmount(interval));
         job.append(" Starting at ");
      } else {
         job.append(" At ");
      }
      job.append(new SimpleDateFormat().format(startTime));

      return job.toString();
   }

}
