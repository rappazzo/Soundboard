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
import java.security.*;
import java.text.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.soundboard.util.*;
import com.google.common.collect.*;

/**
 * @author mrappazz
 *
 *
 **/
public class DefaultHttpServletRequest implements HttpServletRequest {

   ChunkedByteBuffer requestBytes = new ChunkedByteBuffer();

   private Multimap<String, String> parameterMap = HashMultimap.create();
   private Map<String, MultipartData> multipartParameterMap = new HashMap<String, MultipartData>();
   private String method = null;
   private String requestedURI = null;
   private String queryString = null;
   private String contentType = null;
   private Map<String, String> headerMap = new HashMap<String, String>();

   private InetAddress remoteIP;

   public static DefaultHttpServletRequest create(InputStream inStream, InetAddress address) {
      try {
         DefaultHttpServletRequest request = new DefaultHttpServletRequest();
         //should we check for inStream.available()?
         request.requestBytes.append(inStream, true);
         if (request.requestBytes.size() == 0) {
            try {
               Thread.sleep(50);
            } catch (Exception e) {
            }
            request.requestBytes.append(inStream, true);
            if (request.requestBytes.size() == 0) {
               return null;
            }
         }
         request.remoteIP = address;
         ServletInputStream in = request.getInputStream();
         byte[] buffer = new byte[1024];

         int bytesRead = in.readLine(buffer, 0, buffer.length);
         if (bytesRead > 0 && in.available() > 0) { //while? ignore (return;)
            bytesRead = in.readLine(buffer, 0, buffer.length);
         }

         String initialLine = bytesRead > 0 ? new String(buffer, 0, bytesRead) : "GET / HTTP/1.1";
         String[] initialLineParts = initialLine.split("\\s+");
         request.method = initialLineParts[0];
         request.requestedURI = initialLineParts[1];
         try {
            request.requestedURI = URLDecoder.decode(request.requestedURI, CharsetConstants.UTF8.name());
         } catch (UnsupportedEncodingException e) {
            request.requestedURI = URLDecoder.decode(request.requestedURI);
            if (request.requestedURI == null) {
               request.requestedURI = initialLineParts[1];
            }
         }
         String[] parts = request.requestedURI.split("\\?", 2);
         if (parts.length > 1) {
            request.requestedURI = parts[0];
            request.queryString = parts[1];
            String[] paramKV = parts[1].split("&");
            for (String kv : paramKV) {
               String[] kAndV = kv.split("=");
               request.parameterMap.put(kAndV[0], kAndV.length > 1 ? kAndV[1] : null);
            }
         }

         String prevHeaderKey = null;
         while ((bytesRead = in.readLine(buffer, 0, buffer.length)) != -1) {
            String headerElement = new String(buffer, 0, bytesRead).trim();
            if (headerElement == null || headerElement.length() == 0) {
               //an empty line signifies the end of the header
               break;
            }
            String[] headerParts = headerElement.split(":", 2);
            if (headerParts.length == 2) {
               String key = URLDecoder.decode(headerParts[0].trim(), CharsetConstants.UTF8.name());
               prevHeaderKey = key;
               String value = URLDecoder.decode(headerParts[1].trim(), CharsetConstants.UTF8.name());
               request.headerMap.put(key, value);
            } else if (prevHeaderKey != null) {
               //part of the previous header
               String value = URLDecoder.decode(headerParts[0].trim(), CharsetConstants.UTF8.name());
               request.headerMap.put(prevHeaderKey, request.headerMap.get(prevHeaderKey) + value);
            }
         }

         //do special processing for POST form data with multipart encoding
         String contentType = request.getHeader(HttpConstants.CONTENT_TYPE);
         if (HttpConstants.POST.equalsIgnoreCase(request.getMethod()) && contentType != null && contentType.startsWith(HttpConstants.MULTIPART)) {
            //seek the boundary marker
            String boundaryMarker = null;
            for (String contentTypeData : contentType.split("; ?")) {
               if (contentTypeData.startsWith(HttpConstants.BOUNDARY)) {
                  boundaryMarker = contentTypeData.split("=")[1];
               }
            }
            while ((bytesRead = in.readLine(buffer, 0, buffer.length)) != -1) {
               if (new String(buffer, 0, bytesRead).trim().endsWith(boundaryMarker)) {
                  break;
               }
            }
            while (in.available() > 0) {
               Map<String, String> multipartDataHeader = new HashMap<String, String>();
               //we are now at the beginning of the multipart data
               while ((bytesRead = in.readLine(buffer, 0, buffer.length)) != -1) {
                  String multipartElement = new String(buffer, 0, bytesRead).trim();
                  if (multipartElement == null || multipartElement.length() == 0) {
                     //an empty line signifies the end of the multipartContentHeader
                     break;
                  }
                  int splitIndex = multipartElement.indexOf(":");
                  if (splitIndex >= 0) {
                     String key = URLDecoder.decode(multipartElement.substring(0, splitIndex), CharsetConstants.UTF8.name());
                     String value = URLDecoder.decode(multipartElement.substring(splitIndex + 2), CharsetConstants.UTF8.name());
                     multipartDataHeader.put(key, value);
                  }
               }
               //now we are at the data part of the data
               CircularByteBuffer endBoundaryMarker = new CircularByteBuffer(boundaryMarker.length());
               ChunkedByteBuffer multipartBinaryData = new ChunkedByteBuffer();
               byte[] boundaryMarkerBytes = boundaryMarker.getBytes();
               int readByte;
               while ((readByte = in.read()) != -1) {
                  endBoundaryMarker.append((byte)readByte);
                  multipartBinaryData.append((byte)readByte);
                  if (endBoundaryMarker.isEqualTo(boundaryMarkerBytes)) {
                     break;
                  }
               }
               //read to the end of the line
               in.readLine(buffer, 0, buffer.length);

               //"unread" the boundary marker from the data
               MultipartData multipart = new MultipartData(multipartDataHeader, multipartBinaryData);
               String dataKey = multipart.getSubHeaderValue(HttpConstants.CONTENT_DISPOSITION, "name");
               request.multipartParameterMap.put(dataKey, multipart);
               //if the multipart has no content type, then it is likely just normal, form data.  Try to put it in the regular parameter map.
               if (multipart.getHeader(HttpConstants.CONTENT_TYPE) == null) {
                  request.parameterMap.put(dataKey, multipartBinaryData.toChunkedCharBuffer().toString());
               }
            }
         } else {
            String parameterString = null;
            if ((bytesRead = in.readLine(buffer, 0, buffer.length)) != -1) {
               parameterString = new String(buffer, 0, bytesRead).trim();
               while ((bytesRead = in.readLine(buffer, 0, buffer.length)) != -1 && (parameterString == null || parameterString.length() == 0)) {
                  parameterString = new String(buffer, 0, bytesRead).trim();
               }
            }
            if (parameterString != null && parameterString.length() > 0) {
               request.parseParametersFromRequest(parameterString);
            }
         }
         return request;
      } catch (IOException e) {
         e.printStackTrace();
         return null;
      }
   }

   private DefaultHttpServletRequest() {
   }

   public ChunkedByteBuffer getRequestBytes() {
      return requestBytes;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getAuthType()
    */
   @Override public String getAuthType() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getContextPath()
    */
   @Override public String getContextPath() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getCookies()
    */
   @Override public Cookie[] getCookies() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getDateHeader(java.lang.String)
    */
   @Override public long getDateHeader(String headerKey) {
      try {
         String header = getHeader(headerKey);
         try {
            Date date = new SimpleDateFormat().parse(header);
            return date.getTime();
         } catch (ParseException e) {
            return Long.valueOf(header).longValue();
         }
      } catch (Exception e) {
         return -1;
      }
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getHeader(java.lang.String)
    */
   @Override public String getHeader(String headerKey) {
      return headerMap.get(headerKey);
   }

   /**
    * get the multipart data
    */
   public MultipartData getMultipart(String key) {
      return multipartParameterMap.get(key);
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
    */
   @Override public Enumeration getHeaderNames() {
      return Collections.enumeration(headerMap.keySet());
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getHeaders(java.lang.String)
    */
   @Override public Enumeration getHeaders(String headerKey) {
      String header = getHeader(headerKey);
      return Collections.enumeration(Arrays.asList(header.split(",")));
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getIntHeader(java.lang.String)
    */
   @Override public int getIntHeader(String headerKey) {
      try {
         return Integer.valueOf(getHeader(headerKey)).intValue();
      } catch (Exception e) {
         return -1;
      }
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getMethod()
    */
   @Override public String getMethod() {
      return method;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getPathInfo()
    */
   @Override public String getPathInfo() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getPathTranslated()
    */
   @Override public String getPathTranslated() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getQueryString()
    */
   @Override public String getQueryString() {
      return queryString;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
    */
   @Override public String getRemoteUser() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getRequestURI()
    */
   @Override public String getRequestURI() {
      return requestedURI;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getRequestURL()
    */
   @Override public StringBuffer getRequestURL() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getRequestedSessionId()
    */
   @Override public String getRequestedSessionId() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getServletPath()
    */
   @Override public String getServletPath() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getSession()
    */
   @Override public HttpSession getSession() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getSession(boolean)
    */
   @Override public HttpSession getSession(boolean arg0) {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
    */
   @Override public Principal getUserPrincipal() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
    */
   @Override public boolean isRequestedSessionIdFromCookie() {
      return false;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
    */
   @Override public boolean isRequestedSessionIdFromURL() {
      return false;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromUrl()
    */
   @Override public boolean isRequestedSessionIdFromUrl() {
      return false;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
    */
   @Override public boolean isRequestedSessionIdValid() {
      return false;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)
    */
   @Override public boolean isUserInRole(String arg0) {
      return false;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getAttribute(java.lang.String)
    */
   @Override public Object getAttribute(String arg0) {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getAttributeNames()
    */
   @Override public Enumeration getAttributeNames() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getCharacterEncoding()
    */
   @Override public String getCharacterEncoding() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getContentLength()
    */
   @Override public int getContentLength() {
      return 0;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getContentType()
    */
   @Override public String getContentType() {
      return contentType != null ? contentType : "text/html";
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getInputStream()
    */
   @Override public ServletInputStream getInputStream() throws IOException {
      return new ServletInputStream() {
         InputStream in = requestBytes.toInputStream();

         @Override public int available() throws IOException {
            return in.available();
         }

         @Override public int read() throws IOException {
            return in.read();
         }
      };
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getLocale()
    */
   @Override public Locale getLocale() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getLocales()
    */
   @Override public Enumeration getLocales() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
    */
   @Override public String getParameter(String key) {
      String value = null;
      Collection<String> valueSet = parameterMap.get(key);
      if (valueSet != null && valueSet.size() > 0) {
         value = valueSet.iterator().next();
      }
      return value;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getParameterMap()
    */
   @Override public Map getParameterMap() {
      return parameterMap.asMap();
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getParameterNames()
    */
   @Override public Enumeration getParameterNames() {
      return Collections.enumeration(parameterMap.keySet());
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getParameterValues(java.lang.String)
    */
   @Override public String[] getParameterValues(String key) {
      String[] values = null;
      Collection<String> valueSet = parameterMap.get(key);
      if (valueSet != null) {
         values = valueSet.toArray(new String[valueSet.size()]);
      }
      return values;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getProtocol()
    */
   @Override public String getProtocol() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getReader()
    */
   @Override public BufferedReader getReader() throws IOException {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
    */
   @Override public String getRealPath(String arg0) {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getRemoteAddr()
    */
   @Override public String getRemoteAddr() {
      return remoteIP.getHostAddress();
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getRemoteHost()
    */
   @Override public String getRemoteHost() {
      return remoteIP.getHostName();
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
    */
   @Override public RequestDispatcher getRequestDispatcher(String arg0) {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getScheme()
    */
   @Override public String getScheme() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getServerName()
    */
   @Override public String getServerName() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getServerPort()
    */
   @Override public int getServerPort() {
      return 0;
   }
   
   @Override public String getLocalAddr() {
      return null;
   }
   
   @Override public String getLocalName() {
      return null;
   }
   
   @Override public int getLocalPort() {
      return 0;
   }
   
   @Override public int getRemotePort() {
      return 0;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#isSecure()
    */
   @Override public boolean isSecure() {
      return false;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#removeAttribute(java.lang.String)
    */
   @Override public void removeAttribute(String arg0) {

   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#setAttribute(java.lang.String, java.lang.Object)
    */
   @Override public void setAttribute(String arg0, Object arg1) {

   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#setCharacterEncoding(java.lang.String)
    */
   @Override public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {

   }

   private void parseParametersFromRequest(String paramString) {
      //the last line of the request should have the parameters
      if (paramString != null && paramString.length() > 0) {
         String[] keyValuePairs = paramString.split("&");
         for (String keyValuePair : keyValuePairs) {
            String[] keyAndValue = keyValuePair.split("=");
            String key = null;
            try {
               key = URLDecoder.decode(keyAndValue[0], CharsetConstants.UTF8.name());
            } catch (UnsupportedEncodingException e) {
               key = URLDecoder.decode(keyAndValue[0]);
               if (key == null) {
                  key = keyAndValue[0];
               }
            }
            ;
            String value = null;
            try {
               value = keyAndValue.length >= 2 ? URLDecoder.decode(keyAndValue[1], CharsetConstants.UTF8.name()) : null;
            } catch (UnsupportedEncodingException e) {
               value = URLDecoder.decode(keyAndValue[1]);
               if (value == null) {
                  value = keyAndValue[1];
               }
            }
            parameterMap.put(key, value);
         }
      }
   }


   public static class MultipartData {
      public Map<String, String> header = null;
      public ChunkedByteBuffer data = null;

      public MultipartData(Map<String, String> header, ChunkedByteBuffer data) {
         this.header = header;
         this.data = data;
      }

      public ChunkedByteBuffer getData() {
         return data;
      }

      public String getSubHeaderValue(String headerKey, String subHeaderKey) {
         String headerValue = getHeader(headerKey);
         if (headerValue != null) {
            String[] subHeaders = headerValue.split("; ?");
            for (String subHeaderChunk : subHeaders) {
               String[] subHeader = subHeaderChunk.split("=");
               if (subHeader.length > 0 && subHeader[0].equals(subHeaderKey)) {
                  return subHeader.length > 1 ? subHeader[1].substring(1, subHeader[1].length() - 1) : "";
               }
            }
         }
         return null;
      }

      public long getDateHeader(String headerKey) {
         try {
            String header = getHeader(headerKey);
            try {
               Date date = new SimpleDateFormat().parse(header);
               return date.getTime();
            } catch (ParseException e) {
               return Long.valueOf(header).longValue();
            }
         } catch (Exception e) {
            return -1;
         }
      }

      public String getHeader(String headerKey) {
         return header.get(headerKey);
      }

      public Enumeration getHeaderNames() {
         return Collections.enumeration(header.keySet());
      }

      public Enumeration getHeaders(String headerKey) {
         String header = getHeader(headerKey);
         return Collections.enumeration(Arrays.asList(header.split(",")));
      }

      public int getIntHeader(String headerKey) {
         try {
            return Integer.valueOf(getHeader(headerKey)).intValue();
         } catch (Exception e) {
            return -1;
         }
      }
   }

}
