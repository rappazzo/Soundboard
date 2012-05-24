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
package org.soundboard.server.servlet;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.http.*;
import org.soundboard.server.*;
import org.soundboard.util.*;

/**
 * @author mrappazz
 *
 *
 **/
public class BaseServlet implements Servlet {
   
   private static final String EOL = "\r\n";
   private String servletAcceptKey;
   
   public BaseServlet() {
      this(null);
   }
   
   public BaseServlet(String servletAcceptKey) {
      this.servletAcceptKey = servletAcceptKey;
   }
   
   public String getServletAcceptKey() {
      return servletAcceptKey;
   }
   
   protected int getMinArguments() {
      return 1;
   }
   
   /**
    * this servlet accepts requests of the form "/servletAcceptKey/relayerType"
    */
   @Override public boolean accept(HttpServletRequest servletRequest, Socket httpConnection) {
      if (servletRequest != null ) {
         String uri = servletRequest.getRequestURI();
         String[] uriParts = uri.substring(1).split("[/\\ ]+");
         String servletAcceptKey = getServletAcceptKey();
         return (
            servletAcceptKey == null ||
            (
               uriParts != null &&
               uriParts.length >= getMinArguments() &&
               servletAcceptKey.equalsIgnoreCase(uriParts[0])
            )
         );
      }
      return false;
   }
   
   @Override public void handle(HttpServletRequest servletRequest, PrintStream outStream, Socket httpConnection) {
      LoggingService.getInstance().serverLog("From " + httpConnection.getInetAddress().getHostAddress() + ": " + servletRequest.getRequestURI());
      ChunkedByteBuffer responseContent = getResponseContents(servletRequest, httpConnection);
      try {
         if (responseContent.length() > 0) {
            createResponseHeader(responseContent.length()).writeTo(outStream);
            responseContent.writeTo(outStream);
         }
      } catch (IOException e) {
         LoggingService.getInstance().serverLog(e);
      }
      LoggingService.getInstance().serverLog("---------------------------------------------");
   }
   
   protected final ChunkedByteBuffer createResponseHeader(int size) {
      ChunkedByteBuffer responseHeader = new ChunkedByteBuffer();
      try {
         //send back the results
         responseHeader.append("HTTP/1.0 " + HttpConstants.HTTP_OK + " OK");
         responseHeader.append(EOL);
         responseHeader.append("Server: Soundboard Server");
         responseHeader.append(EOL);
         responseHeader.append("Date: " + (new Date()));
         responseHeader.append(EOL);
         
         responseHeader.append("Content-length: " + size);
         responseHeader.append(EOL);
         responseHeader.append("Last Modified: " + (new Date()));
         responseHeader.append(EOL);
         responseHeader.append("Content-type: text/html");
         responseHeader.append(EOL);
         responseHeader.append(EOL);

      } catch (IOException e) {
         LoggingService.getInstance().serverLog(e);
      }
      return responseHeader;
   }
   
   protected ChunkedByteBuffer getResponseContents(HttpServletRequest servletRequest, Socket httpConnection) {
      ChunkedByteBuffer response = new ChunkedByteBuffer();
      try {
         response.append("<html>");
         response.append("<head></head>");
         response.append("<body>");
         response.append("Hello");
         response.append("</body>");
         response.append("</html>");
      } catch (IOException e) {
         LoggingService.getInstance().serverLog(e);
      }
      return response;
   }

}
