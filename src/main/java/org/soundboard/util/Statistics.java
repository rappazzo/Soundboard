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
package org.soundboard.util;

import java.io.*;
import java.util.*;
import org.soundboard.server.*;

public class Statistics {
   
   private static final String FILE = "statistics.txt";
   
   private static Map<String, Map<String, Integer>> stats = new TreeMap<String, Map<String, Integer>>();
   private static Object mutex = new Object();
   static {
      readObject();
   }


   /**
    * increment the statistical count for the given item
    */
   public static void incrementCount(final String item, final String who) {
      new OfflineWorker(
         new OfflineTask() {
            public void doOfflineWork() {
               if (item != null) {
                  synchronized (mutex) {
                     Map<String, Integer> uses = stats.get(item);
                     if (uses == null) {
                        uses = new TreeMap<String, Integer>();
                        stats.put(item, uses);
                     }
                     Integer count = uses.get(who);
                     if (count == null) {
                        count = new Integer(1);
                     } else {
                        count = new Integer(count.intValue() + 1);
                     }
                     uses.put(who, count);
                  }
               }
            }
         }
      );
   }
   
   /**
    * get the statistics
    */
   public static String getCount(String... items) {
      StringBuilder buf = new StringBuilder();
      buf.append("Current Statistics: \r\n");
      Set<String> itemSet = new TreeSet<String>();
      if (items != null && items.length > 0) {
         for (String item : items) {
            itemSet.add(item);
         }
      }
      Map<String, Integer> userTotals = new TreeMap<String, Integer>();
      for (Map.Entry<String, Map<String, Integer>> entry : stats.entrySet()) {
         String item = entry.getKey();
         if (items == null || itemSet.contains(item)) {
            int total = 0;
            buf.append(item);
            buf.append(" --> ");
            Map<String, Integer> uses = entry.getValue();
            for (Map.Entry<String, Integer> use : uses.entrySet()) {
               String who = use.getKey();
               buf.append(who);
               buf.append(": ");
               int count = use.getValue().intValue();
               total += count;
               buf.append(count);
               buf.append(", ");

               Integer userCount = userTotals.get(use.getKey());
               if (userCount == null) {
                  userCount = use.getValue();
               } else {
                  userCount = new Integer(userCount.intValue() + use.getValue().intValue());
               }
               userTotals.put(who, userCount);
            }
            buf.append("Total: ");
            buf.append(total);
            buf.append("\r\n");
         }
      }
      buf.append("--- User Totals ---\r\n");
      for (Map.Entry<String, Integer> userTotal : userTotals.entrySet()) {
         buf.append("    ");
         buf.append(userTotal.getKey());
         buf.append(" = ");
         buf.append(userTotal.getValue());
         buf.append("\r\n");
      }
      return buf.toString();
   }
   
   /**
    * get the statistics
    */
   public static String getCountAsHtml(String... items) {
      StringBuilder buf = new StringBuilder();
      buf.append("<table><tr><td colspan=\"2\"><b>");
      buf.append("Current Statistics: ");
      buf.append("</b></td></tr>");
      Set<String> itemSet = new TreeSet<String>();
      if (items != null && items.length > 0) {
         for (String item : items) {
            itemSet.add(item);
         }
      }
      Map<String, Integer> userTotals = new TreeMap<String, Integer>();
      for (Map.Entry<String, Map<String, Integer>> entry : stats.entrySet()) {
         String item = entry.getKey();
         if (items == null || itemSet.contains(item)) {
            int total = 0;
            buf.append("<tr><td>");
            buf.append(item);
            buf.append("</td><td>");
            Map<String, Integer> uses = entry.getValue();
            for (Map.Entry<String, Integer> use : uses.entrySet()) {
               String who = use.getKey();
               buf.append(who);
               buf.append(": ");
               int count = use.getValue().intValue();
               total += count;
               buf.append(count);
               buf.append(", ");

               Integer userCount = userTotals.get(use.getKey());
               if (userCount == null) {
                  userCount = use.getValue();
               } else {
                  userCount = new Integer(userCount.intValue() + use.getValue().intValue());
               }
               userTotals.put(who, userCount);
            }
            buf.append("Total: ");
            buf.append(total);
            buf.append("</td></tr>");
         }
      }
      buf.append("</table>");
      buf.append("<table>");
      buf.append("<tr><td colspan=\"3\"><hr/></td></tr>");
      buf.append("<tr><td colspan=\"3\"><b>User Totals</b></td></tr>");
      buf.append("<tr><td colspan=\"3\"><hr/></td></tr>");
      for (Map.Entry<String, Integer> userTotal : userTotals.entrySet()) {
         buf.append("<tr><td>&nbsp;</td><td>");
         buf.append(userTotal.getKey());
         buf.append("</td><td>");
         buf.append(userTotal.getValue());
         buf.append("</td></tr>");
      }
      buf.append("</table>");
      return buf.toString();
   }
   
   /**
    * write the subscribers to disk
    */
   public static void writeObject() {
      ChunkedCharBuffer buf = new ChunkedCharBuffer();
      for (Map.Entry<String, Map<String, Integer>> entry : stats.entrySet()) {
         String item = entry.getKey();
         buf.append(item);
         buf.append("|");
         Map<String, Integer> uses = entry.getValue();
         for (Map.Entry<String, Integer> use : uses.entrySet()) {
            buf.append(use.getKey());
            buf.append("=");
            buf.append(use.getValue().intValue());
            buf.append(";");
         }
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
    * read the subscribers from disk
    */
   public static void readObject() {
      try {
         if (new File(FILE).exists()) {
            ChunkedCharBuffer buf = new ChunkedCharBuffer();
            buf.append(new InputStreamReader(new FileInputStream(FILE)));
            String contents = buf.toString();
            String[] lines = contents.split("[\n\r]+");
            for (String line : lines) {
               String[] splitLine = line.split("\\|");
               String item = splitLine[0];
               Map<String, Integer> statUses = new TreeMap<String, Integer>();
               stats.put(item, statUses);
               
               if (splitLine.length > 1) {
                  String[] uses = splitLine[1].split(";");
                  for (String use : uses) {
                     if (StringUtil.isNotNullOrEmpty(use)) {
                        String[] stat = use.split("=");
                        statUses.put(stat[0], new Integer(stat[1]));
                     }
                  }
               }
            }
         }
      } catch (Exception e) {
         LoggingService.getInstance().serverLog("Error reading subscribers: " + e);
      }
   }
   
}
