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

import org.soundboard.server.*;

public abstract class InputService extends Thread implements Stoppable {
   
   /**
    * Constructor
    */
   public InputService() {
      this.setName(this.getClass().getSimpleName() + " Input Service");
   }
   
   /**
    * provide the service name
    */
   public abstract String getServiceName();
   
   /**
    * initialize this service
    * @return if this service can be run in it's current initialization state (ie, if there is enough info)
    *    For example if this service requires a user name and password, but one of them has not been
    * provided, this method will return false.
    */
   public abstract boolean initialize();
   
   /**
    * send a message back to the
    */
   public abstract void send(String to, String message);

   /**
    * check if the given user is currently available on this service
    */
   public abstract boolean isAvailable(String userName);
   
}
