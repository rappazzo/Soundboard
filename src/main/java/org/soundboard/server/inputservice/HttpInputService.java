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
import javax.servlet.http.*;
import org.soundboard.server.*;
import org.soundboard.server.command.*;
import org.soundboard.server.servlet.*;
import org.soundboard.util.*;

public class HttpInputService extends InputService {

   public static final String SERVICE_NAME = "http";
   public static final String PORT = "port";
   public static final String MAX_CONNECTIONS = "maxConnections";

   static final byte[] EOL = {(byte)'\r', (byte)'\n'};

   protected int port = 80;
   protected int maxConnections = 5;
   protected static boolean running = true;
   private List<Worker> workerThreads = new ArrayList<Worker>();
   
   private List<Servlet> servlets = new ArrayList<Servlet>();
   private Servlet defaultServlet = new BaseServlet() {
      @Override protected ChunkedByteBuffer getResponseContents(HttpServletRequest servletRequest, Socket httpConnection) {
         ChunkedByteBuffer response = new ChunkedByteBuffer();
         StringBuilder buf = new StringBuilder();
         String[] split = servletRequest.getRequestURI().substring(1).split("[/\\ ]+");
         if (split != null && split.length > 0 && split[0].length() > 0) {
            String html = new CommandHandler().handleCommand(HttpInputService.this, servletRequest.getHeader(HttpConstants.HOST), split, false, true);
            if (html != null) {
               buf.append(html);
            }
         }
         if (buf.length() <= 0) {
            Map<String, Command> registry = CommandHandler.getRegistry();
            buf.append("<html>");
            buf.append("<body>");
            if (registry.size() > 0) {
               buf.append("<table><tr><td colspan=\"2\"><b>");
               buf.append("Available Commands");
               buf.append("</b></td></tr>");
               for (Map.Entry<String, Command> commands : registry.entrySet()) {
                  String cmdInvokation = commands.getKey();
                  Command cmd = commands.getValue();
                  if (cmd != null && cmd.isHttpCommand()) {
                     buf.append("<tr><td>");
                     buf.append("<a href=\"/");
                     buf.append(cmdInvokation);
                     buf.append("\">");
                     buf.append(cmdInvokation);
                     buf.append("</a>");
                     buf.append("</td><td>");
                     buf.append(cmd.getDescription());
                     buf.append("</td></tr>");
                  }
               }
               buf.append("</table>");
            }
            buf.append("</body>");
            buf.append("</html>");
         }
         return response.append(buf.toString().getBytes());
      }
   };

   @Override
   public String getServiceName() {
      return SERVICE_NAME;
   }

   @Override
   public boolean initialize() {
      Integer configPort = SoundboardConfiguration.config().getIntegerProperty(SoundboardConfiguration.INPUT, getServiceName(), PORT);
      if (configPort != null) {
         port = configPort.intValue();
         servlets.add(new RssServlet());
      }
      maxConnections = Math.max(maxConnections, SoundboardConfiguration.config().getIntProperty(SoundboardConfiguration.INPUT, getServiceName(), MAX_CONNECTIONS));
      return port > 0;
   }

   @Override
   public void send(String to, String message) {
      //no-op
      //NOT IMPLEMENTED
   }

   public boolean isRunning() {
      return running;
   }

   public void stopRunning() {
      running = false;
      synchronized (workerThreads) {
         for (Worker worker : workerThreads) {
            worker.interrupt();
         }
      }
   }

   @Override
   public boolean isAvailable(String userName) {
      return false;
      //NOT IMPLEMENTED
   }

   @Override public void run() {
      try {
         for (int i = 0; i < maxConnections; ++i) {
            Worker worker = new Worker();
            worker.setName("Http Worker #" + i);
            worker.start();
            workerThreads.add(worker);
         }
         ServerSocket ss = new ServerSocket(port);
         while (isRunning()) {
            Socket s = ss.accept();
            Worker w = null;
            synchronized (workerThreads) {
               if (workerThreads.isEmpty()) {
                  Worker ws = new Worker();
                  ws.setName("Additional Http Worker");
                  ws.setSocket(s);
                  ws.start();
               } else {
                  w = workerThreads.get(0);
                  workerThreads.remove(0);
                  w.setSocket(s);
               }
            }
         }
      } catch (IOException e) {
         LoggingService.getInstance().serverLog(e);
      }
   }

   class Worker extends Thread {
      final static int BUF_SIZE = 2048;

      private Socket listener;

      private Worker() {
         listener = null;
      }

      synchronized void setSocket(Socket s) {
         this.listener = s;
         notify();
      }

      @Override public synchronized void run() {
         while (isRunning()) {
            if (listener == null) {
               try {
                  wait(); //for a connection
               } catch (InterruptedException e) {
                  continue;
               }
            }
            try {
               handleClient();
            } catch (Exception e) {
               LoggingService.getInstance().serverLog(e);
            }
            /* go back in wait queue if there's fewer
             * than numHandler connections.
             */
            listener = null;
            synchronized (workerThreads) {
               if (workerThreads.size() >= maxConnections) {
                  /* too many threads, exit this one */
                  return;
               } else {
                  workerThreads.add(this);
               }
            }
         }
      }

      void handleClient() throws IOException {
         InputStream inStream = listener.getInputStream();
         PrintStream outStream = new PrintStream(listener.getOutputStream());
         /* we will only block in read for this many milliseconds
          * before we fail with java.io.InterruptedIOException,
          * at which point we will abandon the connection.
          */
         listener.setSoTimeout(0);
         listener.setTcpNoDelay(true);
         DefaultHttpServletRequest servletRequest = DefaultHttpServletRequest.create(inStream, listener.getInetAddress());
         try {
            boolean servletRun = false;
            for (Servlet servlet : servlets) {
               if (servlet.accept(servletRequest, listener)) {
                  servletRun = true;
                  servlet.handle(servletRequest, outStream, listener);
                  break;
               }
            }
            if (!servletRun) {
               defaultServlet.accept(servletRequest, listener);
               defaultServlet.handle(servletRequest, outStream, listener);
            }
         } finally {
            listener.close();
         }
      }

   }

}
