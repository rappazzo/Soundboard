/***
 **  @(#) TradeCard.com 1.0
 **
 **  Copyright (c) 2013 TradeCard, Inc. All Rights Reserved.
 **
 **
 **  THIS COMPUTER SOFTWARE IS THE PROPERTY OF TradeCard, Inc.
 **
 **  Permission is granted to use this software as specified by the TradeCard
 **  COMMERCIAL LICENSE AGREEMENT.  You may use this software only for
 **  commercial purposes, as specified in the details of the license.
 **  TRADECARD SHALL NOT BE LIABLE FOR ANY  DAMAGES SUFFERED BY
 **  THE LICENSEE AS A RESULT OF USING OR MODIFYING THIS SOFTWARE IN ANY WAY.
 **
 **  YOU MAY NOT DISTRIBUTE ANY SOURCE CODE OR OBJECT CODE FROM THE TradeCard.com
 **  TOOLKIT AT ANY TIME. VIOLATORS WILL BE PROSECUTED TO THE FULLEST EXTENT
 **  OF UNITED STATES LAW.
 **
 **  @version 1.0
 **  @author Copyright (c) 2013 TradeCard, Inc. All Rights Reserved.
 **
 **/
package org.soundboard.server.servlet;

import java.io.*;
import java.net.*;
import javax.servlet.http.*;
import org.soundboard.exception.*;
import org.soundboard.util.*;

/**
 * 
 **/
public class FavIconServlet extends BaseServlet {

   @Override public boolean accept(HttpServletRequest servletRequest, Socket httpConnection) {
      if (servletRequest != null ) {
         String uri = servletRequest.getRequestURI();
         return uri != null && uri.toLowerCase().startsWith("/favicon");
      }
      return false;
   }
   
   @Override public void handle(HttpServletRequest servletRequest, PrintStream outStream, Socket httpConnection) {
      if (servletRequest != null) {
         String uri = servletRequest.getRequestURI();
         try {
            ChunkedByteBuffer in = new ChunkedByteBuffer();
            //read
            in.append(getClass().getClassLoader().getResourceAsStream(uri));
            //write
            in.writeTo(outStream);
         } catch (Exception e) {
            throw new UnservableException(HttpConstants.HTTP_NOT_FOUND);
         }
      }
   }
   
}
