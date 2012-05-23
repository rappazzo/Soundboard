/***
 **  @(#) TradeCard.com 1.0
 **
 **  Copyright (c) 2009 TradeCard, Inc. All Rights Reserved.
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
 **  @author Copyright (c) 2009 TradeCard, Inc. All Rights Reserved.
 **
 **/
package org.soundboard.server.inputservice;

import java.util.*;
import org.soundboard.server.command.*;


/**
 * @author mrappazzo
 *
 *
 **/
class MessageInterpreter extends Thread {

   private InputService service;
   private String from;
   private String request;
   private CommandHandler commandHandler = new CommandHandler();

   public MessageInterpreter(InputService service, String from, String request) {
      this.setName(service.getServiceName() + " Message Interpreter");
      this.service = service;
      this.from = from;
      this.request = request;
      this.start();
   }

   @Override public void run() {
      String results = commandHandler.handleCommand(service, from, request.split("\\s"));
      if (results != null && !results.equals("")) {
         String[] textToSend = new String[] {results};
         if (results.length() > 1024) {
            //split the message if it is too large...Consider moving this to the Client Class
            List<String> splitList = new ArrayList<String>();
            
            String working = new String(results);

            int index = working.lastIndexOf("\n", 1024);
            if (index < 0) { //should we disregard low numbers?
               index = 1024;
            }
            //seek a logic split point
            while (working.length() > 0) {
               splitList.add(working.substring(0, index));
               if (working.length() > index) {
                  working = working.substring(index + 1);
                  index = working.lastIndexOf("\n", Math.min(1024, working.length()));
                  if (index < 0) { //should we disregard low numbers?
                     index = Math.min(1024, working.length());
                  }
               } else {
                  working = "";
               }
            }
            textToSend = splitList.toArray(textToSend);
         }
         for (String text : textToSend) {
            service.send(from, text);
         }
      }
   }
}
