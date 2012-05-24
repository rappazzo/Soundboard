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
import java.util.concurrent.*;
import javax.servlet.http.*;
import org.soundboard.server.*;
import org.soundboard.server.command.*;
import org.soundboard.server.servlet.*;
import org.soundboard.util.*;

public class HttpInputService extends InputService {

   public static final String SERVICE_NAME = "http";
   public static final String PORT = "port";
   public static final String MAX_CONNECTIONS = "maxConnections";
   
   private static ExecutorService WORKER_POOL = null;

   static final byte[] EOL = {(byte)'\r', (byte)'\n'};

   protected int port = 80;
   private int maxConnections;
   
   
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
      maxConnections = Math.max(5, SoundboardConfiguration.config().getIntProperty(SoundboardConfiguration.INPUT, getServiceName(), MAX_CONNECTIONS));
      WORKER_POOL = new ThreadPoolExecutor(0, maxConnections,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(),
            new NamedThreadFactory("HttpListener", true));
      return port > 0;
   }

   @Override
   public void send(String to, String message) {
      //no-op
      //NOT IMPLEMENTED
   }

   @Override public boolean isRunning() {
      return WORKER_POOL.isTerminated();
   }

   @Override public void stopRunning() {
      try {
         WORKER_POOL.shutdownNow();
         WORKER_POOL.awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
         //ahh!!
      }
   }

   @Override
   public boolean isAvailable(String userName) {
      return false;
      //NOT IMPLEMENTED
   }

   @Override public void run() {
      try {
         ServerSocket ss = new ServerSocket(port);
         while (isRunning()) {
            Socket s = ss.accept();
            WORKER_POOL.submit(new Worker(s));
         }
      } catch (IOException e) {
         LoggingService.getInstance().serverLog(e);
      }
   }

   class Worker implements Runnable {
      final static int BUF_SIZE = 2048;

      private Socket listener;

      private Worker(Socket s) {
         listener = s;
      }

      @Override public void run() {
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
