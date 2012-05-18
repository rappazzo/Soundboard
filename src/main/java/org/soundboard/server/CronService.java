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

import java.io.*;
import java.util.*;
import org.soundboard.util.*;

public class CronService implements Stoppable {

   private static final String FILE = "crontab.txt";
   private Map<String, CronJob> events = new HashMap<String, CronJob>();
   private boolean isRunning = false;
   private Timer service = new Timer();
   private static CronService instance = null;
   
   private CronService() {
   }
   
   public static void startService() {
      readObject();
   }
   
   public static CronService getInstance() {
      if (instance == null) {
         instance = new CronService();
      }
      return instance;
   }
   
   public boolean isRunning() {
      return isRunning;
   }
   
   public void stopRunning() {
      this.service.cancel();
      this.service.purge();
      this.service = new Timer();
      this.isRunning = false;
   }
   
   public static String listJobs() {
      StringBuilder buf = new StringBuilder();
      for (Map.Entry<String, CronJob> jobInfo : getInstance().events.entrySet()) {
         CronJob event = jobInfo.getValue();
         if (event.isSchedulable()) {
            buf.append("\n");
            buf.append(event.toString());
         }
      }
      return buf.toString();
   }
   
   public static String cancelJob(String jobIdentifier) {
      CronService cron = getInstance();
      CronJob oldJob = cron.events.remove(jobIdentifier);
      if (oldJob != null) {
         cron.stopRunning();
         for (Map.Entry<String, CronJob> entry : getInstance().events.entrySet()) {
            scheduleJob(entry.getValue());
         }
         return "Cancelled [" + oldJob.toString() + "]";
      }
      return "No jobs matched identifier [" + jobIdentifier + "]";
   }
   
   public static String scheduleJob(CronJob event) {
      CronService cron = getInstance();
      if (cron.events.get(event.getIdentifier()) == null) {
         if (event.isSchedulable()) {
            cron.isRunning = true;
            if (event.isOneTime()) {
               cron.service.schedule(event.getJob(), event.getStartTime());
            } else {
               event.reschedule();
               cron.service.scheduleAtFixedRate(event.getJob(), event.getStartTime(), event.getInterval().longValue());
            }
            cron.events.put(event.getIdentifier(), event);
            LoggingService.getInstance().serverLog(event.getUser() + " (cron) " + "Scheduled [" + event.toString() + "]");
            return "Scheduled [" + event.toString() + "]";
         }
      } else {
         return "Identifier already used, please try again with a different identifier.";
      }
      return "Unable to schedule: missing data.";
   }
   
   /**
    * write the crontab to disk
    */
   public static void writeObject() {
      ChunkedCharBuffer buf = new ChunkedCharBuffer();
      for (Map.Entry<String, CronJob> entry : getInstance().events.entrySet()) {
         CronJob event = entry.getValue();
         buf.append(entry.getKey());
         buf.append("|");
         buf.append(String.valueOf(event.getInterval()));
         buf.append("|");
         buf.append(String.valueOf(event.getStartTime().getTime()));
         buf.append("|");
         buf.append(event.getCommandString());
         buf.append("|");
         buf.append(event.getUser());
         buf.append("|");
         buf.append(event.getUserService());
         buf.append("\n");
      }
      if (buf.size() > 0) {
         try {
            OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(FILE, false)));
            buf.writeTo(out);
            out.flush();
            out.close();
         } catch (Exception e) {
            LoggingService.getInstance().serverLog("Error writing statistics: " + e);
         }
      }
   }

   /**
    * read the crontab from disk
    */
   public static void readObject() {
      try {
         if (new File(FILE).exists()) {
            LoggingService.getInstance().serverLog("Reading cron table.");
            ChunkedCharBuffer buf = new ChunkedCharBuffer();
            buf.append(new InputStreamReader(new FileInputStream(FILE)));
            String contents = buf.toString();
            String[] lines = contents.split("[\n\r]+");
            for (String line : lines) {
               String[] splitLine = line.split("\\|");
               int index = 0;
               scheduleJob(
                  new CronJob(
                     splitLine[index++], //identifier
                     Long.valueOf(splitLine[index++]), //interval
                     new Date(Long.valueOf(splitLine[index++]).longValue()), //start time
                     splitLine[index++], //command string
                     splitLine[index++], //user
                     splitLine[index++]  //user service
                  )
               );
            }
         } else {
            LoggingService.getInstance().serverLog("No cron table");
         }
      } catch (Exception e) {
         LoggingService.getInstance().serverLog("Error reading cron table: " + e);
      }
   }

}
