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

import java.text.*;
import java.util.*;


public class StringUtil {

   private static final long MS = 1;
   private static final long SECOND = 1000 * MS;
   private static final long MINUTE = 60 * SECOND;
   private static final long HOUR = 60 * MINUTE;
   private static final long DAY = 24 * HOUR;
//   private static final long WEEK = 7 * DAY;
//   private static final long BIWEEK = 2 * WEEK;

   public static final String EMPTY_STRING = "";

   /**
    * Returns true if the passed string is not null or empty. The string is trimmed before
    * the empty string comparison.
    */
   public static boolean isNotNullOrEmpty(String string) {
      return isNotNullOrEmpty(string, true);
   }

   /**
    * Returns true if the passed string is not null or empty
    */
   public static boolean isNotNullOrEmpty(String string, boolean isTrimSpace) {
      if (string == null) {
         return false;
      }

      if (isTrimSpace) {
         if (string.trim().equals(EMPTY_STRING)) {
            return false;
         }
      } else {
         if (string.equals(EMPTY_STRING)) {
            return false;
         }
      }
      return true;
   }

   /**
    * Returns true if the passed string is null or empty. The string is trimmed before
    * the empty string comparison.
    */
   public static boolean isNullOrEmpty(String string) {
      return isNullOrEmpty(string, true);
   }

   /**
    * Returns true if the passed string is null or empty
    */
   public static boolean isNullOrEmpty(String string, boolean isTrimSpace) {
      if (string == null) {
         return true;
      }

      if (isTrimSpace) {
         if (StringUtil.trim(string).equals(EMPTY_STRING)) {
            return true;
         }
      } else {
         if (string.equals(EMPTY_STRING)) {
            return true;
         }
      }
      return false;
   }

   /**
    * A real trim method.  Trims all whitespace characters from the beginning and end of the given string
    * {@link String#trim()}
    * {@link Character#isWhitespace()}
    */
   public static String trim(String src) {
      if (src == null) {
         return null;
      }
      int endIndex = src.length();
      int startIndex = 0;

      while (startIndex < endIndex && Character.isWhitespace(src.charAt(startIndex))) {
         startIndex++;
      }
      while (endIndex > startIndex && Character.isWhitespace(src.charAt(endIndex - 1))) {
         endIndex--;
      }
      return ((startIndex > 0) || (endIndex < src.length())) ? src.substring(startIndex, endIndex) : src;
   }

   /**
    * format the time difference.  Typical output should look like: 3h 27m 34.749s
    */
   public static String formatTimeDifference(long startTimeInMilliseconds, long endTimeInMilliseconds) {
      return formatTimeAmount(endTimeInMilliseconds - startTimeInMilliseconds, false);
   }
   
   /**
    * format the time difference.  Typical output should look like: 3h 27m 34.749s
    */
   public static String formatTimeDifference(long startTimeInMilliseconds, long endTimeInMilliseconds, boolean showMS) {
      return formatTimeAmount(endTimeInMilliseconds - startTimeInMilliseconds, showMS);
   }

   /**
    * format the time difference
    */
   public static String formatTimeAmount(Number timeInMilliseconds) {
      return formatTimeAmount(timeInMilliseconds.longValue(), false);
   }
   
   /**
    * format the time difference
    */
   public static String formatTimeAmount(Number timeInMilliseconds, boolean showMS) {
      return formatTimeAmount(timeInMilliseconds.longValue(), showMS);
   }

   /**
    * format the time difference
    */
   public static String formatTimeAmount(long timeInMilliseconds) {
      return formatTimeAmount(timeInMilliseconds, false);
   }
   
   /**
    * format the time difference
    */
   public static String formatTimeAmount(long timeInMilliseconds, boolean showMS) {
      long timeInSeconds = timeInMilliseconds / 1000;
      long millis = timeInMilliseconds % 1000;

      StringBuilder buffer = new StringBuilder();
      if (timeInMilliseconds < 1000) {
         if (showMS) {
            buffer.append(millis);
            buffer.append("ms ");
         } else {
            buffer.append("< 1s ");
         }
      } else {
         long numDays = 0;
         if (timeInSeconds > DAY) {
            numDays = timeInSeconds / DAY;
            timeInSeconds = timeInSeconds % DAY;
         }
         long numWeeks = 0;
         if (numDays > 7) {
            numWeeks = numDays / 7;
            numDays = numDays % 7;
         }
         if (numWeeks > 0) {
            buffer.append(numWeeks);
            buffer.append("w ");
         }
         if (numWeeks > 0 || numDays > 0) {
            buffer.append(numDays);
            buffer.append("d ");
         }
         //hours
         if (timeInSeconds / 3600 > 0) {
            buffer.append(timeInSeconds / 3600);
            buffer.append("h ");
         }
         //minutes
         if ((timeInSeconds % 3600) / 60 > 0) {
            buffer.append((timeInSeconds % 3600) / 60);
            buffer.append("m ");
         }
         //seconds
         if ((timeInSeconds % 3600) % 60 > 0) {
            buffer.append((timeInSeconds % 3600) % 60);
            if (showMS) {
               buffer.append(".");
               if (millis < 100) buffer.append("0");
               if (millis < 10) buffer.append("0");
               if (millis < 0) {
                  buffer.append("0");
               } else {
                  buffer.append(millis);
               }
            }
            buffer.append("s ");
         }
      }
      return buffer.toString();
   }

   /**
    * Construct a Date object from this string formatted
    * in 'yyyy-MM-dd hh:mm:ss.sss zzz' format to represent this
    * (other date delimiters allowed: -, /, +, .)
    */
   public static Calendar parseDate(String dateString) throws ParseException {
      int MIN_POSSIBLE_DATE_SIZE = 6;
      StringTokenizer tokens = new StringTokenizer(dateString);
      String dateSection = tokens.nextToken();
      String timeSection = null;
      String timezone = null;
      //check for white-space date delimiter (min date size ~ 03-3-3 so 6) or a time only string
      if (dateSection.indexOf(":") >= 0) {
         timeSection = new String(dateSection);
         dateSection = null;
      } else if (dateSection.length() < MIN_POSSIBLE_DATE_SIZE) {
         try {
            dateSection = dateSection + "-" + tokens.nextToken() + "-" + tokens.nextToken();
         } catch (Exception e) {
            throw new ParseException("Date String [" + dateString + "] in an unreadable format", 0);
         }
      }
      if (timeSection == null && tokens.hasMoreTokens()) {
         timeSection = tokens.nextToken();
      }
      if (tokens.hasMoreTokens()) {
         timezone = tokens.nextToken();
      }
      Calendar date = Calendar.getInstance(timezone != null ? TimeZone.getTimeZone(timezone) : TimeZone.getDefault());
      if (dateSection != null) {
         //common date delimiters
         StringTokenizer datePart = new StringTokenizer(dateSection, "-/+.");
         Integer year = null;
         Integer month = null;
         Integer day = null;
         try {
            //if the first token is 4 digits, assume it is YEAR-MONTH-DAY, otherwise assume it is MONTH-DAY-YEAR
            //there doesn't seem to be an easy way to determine if the date is in the standard european format (DAY-MONTH-YEAR)
            String firstToken = datePart.nextToken();
            if (firstToken.length() == 4) {
               year = new Integer(firstToken);
               month = new Integer(datePart.nextToken());
               day = new Integer(datePart.nextToken());
            } else {
               month = new Integer(firstToken);
               day = new Integer(datePart.nextToken());
               if (datePart.hasMoreTokens()) {
                  year = new Integer(datePart.nextToken());
               } else {
                  //no year provided, assume the current year
                  year = new Integer(new Date().getYear());
               }
            }
         } catch (Exception e) {
            throw new ParseException("Date String [" + dateSection + "] in an unreadable format", 0);
         }
         date.set(Calendar.YEAR, year.intValue());
         date.set(Calendar.MONTH, month.intValue() - 1);
         date.set(Calendar.DAY_OF_MONTH, day.intValue());
         if (!checkDate(date)) {
            throw new ParseException("Date [" + dateSection + "] is out of the normal range", 0);
         }
      }
      if (timeSection != null) {
         //Common time delimiters
         StringTokenizer timePart = new StringTokenizer(timeSection, ":");
         Integer hour = null;
         Integer minute = null;
         Integer second = null;
         try {
            hour = new Integer(timePart.nextToken());
            minute = new Integer(timePart.nextToken());
            if (timePart.hasMoreTokens()) {
               second = new Integer(timePart.nextToken());
            } else {
               second = new Integer(0);
            }
         } catch (Exception e) {
            throw new ParseException("Time String [" + timeSection + "] in an unreadable format", 0);
         }
         date.set(Calendar.HOUR, hour.intValue());
         date.set(Calendar.MINUTE, minute.intValue());
         date.set(Calendar.SECOND, second.intValue());
         if (!checkTime(date)) {
            throw new ParseException("Date [" + timeSection + "] is out of the normal range", 0);
         }
      }
      return date;
   }

   /**
    * Check date to insure it is syntactically correct
    */
   public static boolean checkDate(Calendar date) {
      boolean result = true;
      Calendar strictCalendar = Calendar.getInstance(date.getTimeZone());
      try {
         strictCalendar.setLenient(false);
         strictCalendar.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
         //call to run the check with calendar
         strictCalendar.getTime();
      } catch (Exception e) {
         result = false;
      }
      return result;
   }

   /**
    * Check the Time to ensure it is syntactically correct
    */
   public static boolean checkTime(Calendar date) {
      boolean result = true;
      Calendar strictCalendar = Calendar.getInstance(date.getTimeZone());
      try {
         strictCalendar.setLenient(false);
         strictCalendar.set(1971, Calendar.JANUARY, 1, date.get(Calendar.HOUR), date.get(Calendar.MINUTE), date.get(Calendar.SECOND));
         //call to run the check with calendar
         strictCalendar.getTime();
      } catch (Exception e) {
         result = false;
      }
      return result;
   }

   /**
    * join the given string array with the given delimiter
    */
   public static String join(Iterator it, String delim) {
      StringBuilder joined = new StringBuilder();
      if (it != null) {
         while (it.hasNext()) {
            joined.append(it.next());
            if (it.hasNext()) {
               joined.append(delim);
            }
         }
      }
      return joined.toString();
   }

   /**
    * join the given string array with the given delimiter
    */
   public static String join(Object[] array, String delim) {
      return join(array, 0, array.length, delim);
   }

   /**
    * join the given string array with the given delimiter
    */
   public static String join(Object[] array, int start, int end, String delim) {
      if (array != null) {
         if (start > array.length || end < start) {
            return null;
         }
         if (start < 0) {
            start = 0;
         }
         if (end > array.length) {
            end = array.length;
         }
         StringBuilder joined = new StringBuilder();
         for (int i=start; i < end; i++) {
            joined.append(array[i]);
            if ((i+1) < end) {
               joined.append(delim);
            }
         }
         return joined.toString();
      } else {
         return null;
      }
   }

   /**
    * join the given string array with the given delimiter
    */
   public static String join(Iterable iterable, String delim) {
      return iterable != null ? join(iterable.iterator(), delim) : null;
   }
   
   public static String[] deQuoteStringArray(String... quoted) {
      for (int i = 0; i < quoted.length; i++) {
         quoted[i] = quoted[i].substring(1, quoted[i].length() - 1);
      }
      return quoted;
   }
   
   public static String deQuote(String quoted) {
      return deQuoteStringArray(quoted)[0];
   }
   
}
