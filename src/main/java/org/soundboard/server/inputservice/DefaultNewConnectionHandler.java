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
import org.soundboard.server.*;

public class DefaultNewConnectionHandler implements NewConnectionHandler {

   /**
    * do something when a new connection is established
    */
   public void connectionEstablished(Socket session) {
      try {
         PrintStream out = new PrintStream(session.getOutputStream()); 
         out.println("Welcome to the Soundboard!!  Type 'help' for a list of commands.");
      } catch (IOException e) {
         //ignore - no welcome message
      }
   }
   
}