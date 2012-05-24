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
import java.text.*;
import java.util.*;
import org.soundboard.server.*;

public class History {
   
   private static final int MAX_LIVE_HISTORY_SIZE = 10;
   private static final String FILE = "history.txt";
   
   private static LinkedList<HistoryEntry> history = new LinkedList<HistoryEntry>();
   private static Object mutex = new Object();
   static {
      readObject();
   }
   
   /**
    * increment the statistical count for the given item
    */
   public static void addHistory(final String who, final String command) {
      final String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
      new OfflineWorker(
         new OfflineTask() {
            @Override public void doOfflineWork() {
               synchronized(mutex) {
                  history.add(new HistoryEntry(who, now, command));
                  while (history.size() > MAX_LIVE_HISTORY_SIZE) {
                     HistoryEntry purged = history.removeFirst();
                     try {
                        OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(FILE, true)));
                        out.write(purged.toString());
                        out.write('\n');
                        out.flush();
                        out.close();
                     } catch (Exception e) {
                        LoggingService.getInstance().serverLog("Error writing history: " + e);
                     }
                  }
               }
            }
         }
      );
   }
   
   public static Iterable<HistoryEntry> getHistory() {
      return new Iterable<HistoryEntry>() {
         @Override public Iterator iterator() {
            return Collections.unmodifiableList(history).iterator();
         }
      };
   }

   private static final String RSS_ITEM_TEMPLATE =
      "      <item> " +
      "         <title>%s</title> " +
      "         <link>%s</link> " +
      "         <description>%s</description> " +
      "         <pubDate>%s</pubDate> " +
      "         <guid>%s</guid> " +
      "      </item> " +
      "";
   public static class HistoryEntry {
      final String who, when, what;
      final boolean hasSaved;
      HistoryEntry(String who, String when, String what) {
         this(who, when, what, false);
      }
      HistoryEntry(String who, String when, String what, boolean hasSaved) {
         this.who = who;
         this.when = when;
         this.what = what;
         this.hasSaved = hasSaved;
      }
      @Override public String toString() {
         return who+"|"+when+"|"+what;
      }
      public String toRssItem() {
         return String.format(RSS_ITEM_TEMPLATE, toString(), "http://soundboard/"+what.replace(' ', '/'), toString(), when, when);
      }
   }
   
   /**
    * write the subscribers to disk
    */
   public static void writeObject() {
      ChunkedCharBuffer buf = new ChunkedCharBuffer();
      for (HistoryEntry historyEntry : history) {
         if (!historyEntry.hasSaved) {
            buf.append(historyEntry.toString());
            buf.append("\n");
         }
      }
      if (buf.size() > 0) {
         try {
            OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(FILE, true)));
            buf.writeTo(out);
            out.flush();
            out.close();
         } catch (Exception e) {
            LoggingService.getInstance().serverLog("Error writing history: " + e);
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
            for (int lineNumber = lines.length > MAX_LIVE_HISTORY_SIZE ? lines.length - MAX_LIVE_HISTORY_SIZE : 0, n = lines.length; lineNumber < n; lineNumber++) {
               String[] data = lines[lineNumber].split("|");
               history.add(new HistoryEntry(data[0], data[1], data[2], true));
            }
         }
      } catch (Exception e) {
         LoggingService.getInstance().serverLog("Error reading subscribers: " + e);
      }
   }
   
}
