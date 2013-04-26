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

public class SoundboardConfiguration {
   static SoundboardConfiguration INSTANCE = null;

   public static final String SEPARATOR = ".";
   public static final String JOIN_SEPARATOR = "\\s*,\\s*";
   
   //Config keys
   public static final String IS_CLIENT = "isclient";
   public static final String SERVER = "server";
   public static final String USERID = "userid";
   public static final String AUDIO_PLAYBACK_DEVICE = "audio.playback.device";
   public static final String LIBRARY = "library";
   public static final String NAMES = "names";
   public static final String SOURCE = "source";
   public static final String INPUT = "input";
   public static final String KARMA = "karma";
   public static final String CLASS_NAME = "className";
   public static final String QUIT_LISTENER_ACCEPTED_ADDRESSES = "quit.listener.accepted.addresses";
   public static final String AUTOSAVE_INTERVAL = "autosave.interval";
   public static final String COMMAND = "command";
   public static final String DISABLED = "disabled";
   public static final String SHORTCUT = "shortcut";

   public static final String SHUTDOWN_PASSWORD = "shutdown.password";
   
   Properties config;

   static SoundboardConfiguration create(String configFileName) {
      INSTANCE = new SoundboardConfiguration();
      try {
         INSTANCE.config = new Properties();
         INSTANCE.config.load(new FileInputStream(configFileName));
      } catch (IOException e) {
         throw new IllegalStateException("Error loading Config File", e);
      }
      return INSTANCE;
   }
   
   public static SoundboardConfiguration config() {
      return INSTANCE;
   }
   
   /**
    * return a property from the config - empty strings are resolved to null
    */
   public String getProperty(String... key) {
      String value = config.getProperty(buildPropertyKey(key));
      if ("".equals(value)) {
         value = null;
      }
      return value;
   }
   
   /**
    * build a property key from the given variable key arguements
    */
   public String buildPropertyKey(String... keys) {
      if (keys != null) {
         StringBuilder propertyKey = new StringBuilder();
         for (String key : keys) {
            if (propertyKey.length() > 0) {
               propertyKey.append(SEPARATOR);
            }
            propertyKey.append(key);
         }
         return propertyKey.toString();
      }
      return null;
   }
   
   /**
    * return an Integer property from the config
    */
   public Integer getIntegerProperty(String... key) {
      String stringProp = getProperty(buildPropertyKey(key));
      if (stringProp != null) {
         try {
            return Integer.valueOf(stringProp);
         } catch (NumberFormatException e) {
            //ignore
         }
      }
      return null;
   }
   
   /**
    * return an instantiated class from the class name in the config key
    */
   public Object getClassProperty(String... key) {
      String className = getProperty(buildPropertyKey(key));
      if (className != null) {
         try {
            Class clazz = Class.forName(className);
            return clazz.newInstance();
         } catch (Exception e) {
         }
      }
      return null;
   }
   
   /**
    * return a primitive int property from the config
    */
   public int getIntProperty(String... key) {
      Integer integerProp = getIntegerProperty(buildPropertyKey(key));
      return integerProp != null ? integerProp.intValue() : -1;
   }
   
   /**
    * return an array of properties from the config
    */
   public String[] getMultiValueProperty(String... key) {
      String joined = getProperty(buildPropertyKey(key));
      return joined != null ? joined.split(JOIN_SEPARATOR) : null;
   }
   
   /**
    * return a list of properties from the config
    */
   public List<String> getMultiValuePropertyAsList(String... key) {
      String[] array = getMultiValueProperty(buildPropertyKey(key));
      return array != null ? Arrays.asList(array) : null;
   }
   
}
