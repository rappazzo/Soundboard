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
import java.nio.charset.*;
import java.security.*;
import java.text.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.soundboard.util.*;

/**
 * @author mrappazz
 *
 *
 **/
public class DefaultHttpServletRequest implements HttpServletRequest {

   ChunkedByteBuffer requestBytes = new ChunkedByteBuffer();

   private Map<String, Set<String>> parameterMap = new HashMap<String, Set<String>>();
   private Map<String, MultipartData> multipartParameterMap = new HashMap<String, MultipartData>();
   private String method = null;
   private String requestedURI = null;
   private String httpVersionInfo;
   private Map<String, String> headerMap = new HashMap<String, String>();

   public DefaultHttpServletRequest(InputStream inStream) {
      try {
         requestBytes.append(inStream, true);
         ServletInputStream in = getInputStream();
         byte[] buffer = new byte[1024];

         int bytesRead = in.readLine(buffer, 0, buffer.length);
         if (bytesRead > 0 && in.available() > 0) { //while? ignore (return;)
            bytesRead = in.readLine(buffer, 0, buffer.length);
         }

         String initialLine = bytesRead > 0 ? StringUtil.trim(new String(buffer, 0, bytesRead)) : "GET / HTTP/1.1";
         String[] initialLineParts = initialLine.split("\\s+");
         method = initialLineParts[0];
         requestedURI = initialLineParts[1];
         try {
            requestedURI = URLDecoder.decode(requestedURI, CharsetConstants.UTF8.name());
         } catch (UnsupportedEncodingException e) {
            requestedURI = URLDecoder.decode(requestedURI);
            if (requestedURI == null) {
               requestedURI = initialLineParts[1];
            }
         }
         httpVersionInfo = initialLineParts[2];

         while ((bytesRead = in.readLine(buffer, 0, buffer.length)) != -1) {
            String headerElement = StringUtil.trim(new String(buffer, 0, bytesRead));
            if (StringUtil.isNullOrEmpty(headerElement)) {
               //an empty line signifies the end of the header
               break;
            }
            int splitIndex = headerElement.indexOf(":");
            if (splitIndex >= 0) {
               String key = URLDecoder.decode(headerElement.substring(0, splitIndex), CharsetConstants.UTF8.name());
               String value = URLDecoder.decode(headerElement.substring(splitIndex + 2), CharsetConstants.UTF8.name());
               headerMap.put(key, value);
            }
         }

         //do special processing for POST form data with multipart encoding
         String contentType = getHeader(HttpConstants.CONTENT_TYPE);
         if (HttpConstants.POST.equalsIgnoreCase(getMethod()) && contentType.startsWith(HttpConstants.MULTIPART)) {
            //seek the boundary marker
            String boundaryMarker = null;
            for (String contentTypeData : contentType.split("; ?")) {
               if (contentTypeData.startsWith(HttpConstants.BOUNDARY)) {
                  boundaryMarker = contentTypeData.split("=")[1];
               }
            }
            while ((bytesRead = in.readLine(buffer, 0, buffer.length)) != -1) {
               if (StringUtil.trim(new String(buffer, 0, bytesRead)).endsWith(boundaryMarker)) {
                  break;
               }
            }
            while (in.available() > 0) {
               Map<String, String> multipartDataHeader = new HashMap<String, String>();
               //we are now at the beginning of the multipart data
               while ((bytesRead = in.readLine(buffer, 0, buffer.length)) != -1) {
                  String multipartElement = StringUtil.trim(new String(buffer, 0, bytesRead));
                  if (StringUtil.isNullOrEmpty(multipartElement)) {
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
               String boundaryMatch = new String(multipartBinaryData.unappend(boundaryMarker.length() + 4));
               MultipartData multipart = new MultipartData(multipartDataHeader, multipartBinaryData);
               String dataKey = multipart.getSubHeaderValue(HttpConstants.CONTENT_DISPOSITION, "name");
               multipartParameterMap.put(dataKey, multipart);
               //if the multipart has no content type, then it is likely just normal, form data.  Try to put it in the regular parameter map.
               if (multipart.getHeader(HttpConstants.CONTENT_TYPE) == null) {
                  Set<String> values = parameterMap.get(dataKey);
                  if (values == null) {
                     values = new HashSet<String>();
                     parameterMap.put(dataKey, values);
                  }
                  values.add(multipartBinaryData.toChunkedCharBuffer().toString());
               }
            }
         } else {
            String parameterString = null;
            if ((bytesRead = in.readLine(buffer, 0, buffer.length)) != -1) {
               parameterString = StringUtil.trim(new String(buffer, 0, bytesRead));
               while ((bytesRead = in.readLine(buffer, 0, buffer.length)) != -1 && StringUtil.isNullOrEmpty(parameterString)) {
                  parameterString = StringUtil.trim(new String(buffer, 0, bytesRead));
               }
            }
            if (StringUtil.isNotNullOrEmpty(parameterString)) {
               parseParametersFromRequest(parameterString);
            }
         }
      } catch (IOException e) {
         //ignore
      }
   }

   public ChunkedByteBuffer getRequestBytes() {
      return requestBytes;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getAuthType()
    */
   public String getAuthType() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getContextPath()
    */
   public String getContextPath() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getCookies()
    */
   public Cookie[] getCookies() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getDateHeader(java.lang.String)
    */
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

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getHeader(java.lang.String)
    */
   public String getHeader(String headerKey) {
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
   public Enumeration getHeaderNames() {
      return Collections.enumeration(headerMap.keySet());
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getHeaders(java.lang.String)
    */
   public Enumeration getHeaders(String headerKey) {
      String header = getHeader(headerKey);
      return Collections.enumeration(Arrays.asList(header.split(",")));
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getIntHeader(java.lang.String)
    */
   public int getIntHeader(String headerKey) {
      try {
         return Integer.valueOf(getHeader(headerKey)).intValue();
      } catch (Exception e) {
         return -1;
      }
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getMethod()
    */
   public String getMethod() {
      return method;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getPathInfo()
    */
   public String getPathInfo() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getPathTranslated()
    */
   public String getPathTranslated() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getQueryString()
    */
   public String getQueryString() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
    */
   public String getRemoteUser() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getRequestURI()
    */
   public String getRequestURI() {
      return requestedURI;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getRequestURL()
    */
   public StringBuffer getRequestURL() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getRequestedSessionId()
    */
   public String getRequestedSessionId() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getServletPath()
    */
   public String getServletPath() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getSession()
    */
   public HttpSession getSession() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getSession(boolean)
    */
   public HttpSession getSession(boolean arg0) {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
    */
   public Principal getUserPrincipal() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
    */
   public boolean isRequestedSessionIdFromCookie() {
      return false;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
    */
   public boolean isRequestedSessionIdFromURL() {
      return false;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromUrl()
    */
   public boolean isRequestedSessionIdFromUrl() {
      return false;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
    */
   public boolean isRequestedSessionIdValid() {
      return false;
   }

   /* (non-Javadoc)
    * @see javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)
    */
   public boolean isUserInRole(String arg0) {
      return false;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getAttribute(java.lang.String)
    */
   public Object getAttribute(String arg0) {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getAttributeNames()
    */
   public Enumeration getAttributeNames() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getCharacterEncoding()
    */
   public String getCharacterEncoding() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getContentLength()
    */
   public int getContentLength() {
      return 0;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getContentType()
    */
   public String getContentType() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getInputStream()
    */
   public ServletInputStream getInputStream() throws IOException {
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
   public Locale getLocale() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getLocales()
    */
   public Enumeration getLocales() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
    */
   public String getParameter(String key) {
      String value = null;
      Set<String> valueSet = parameterMap.get(key);
      if (valueSet != null) {
         value = valueSet.iterator().next();
      }
      return value;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getParameterMap()
    */
   public Map getParameterMap() {
      return parameterMap;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getParameterNames()
    */
   public Enumeration getParameterNames() {
      return Collections.enumeration(parameterMap.keySet());
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getParameterValues(java.lang.String)
    */
   public String[] getParameterValues(String key) {
      String[] values = null;
      Set<String> valueSet = parameterMap.get(key);
      if (valueSet != null) {
         values = valueSet.toArray(new String[valueSet.size()]);
      }
      return values;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getProtocol()
    */
   public String getProtocol() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getReader()
    */
   public BufferedReader getReader() throws IOException {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
    */
   public String getRealPath(String arg0) {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getRemoteAddr()
    */
   public String getRemoteAddr() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getRemoteHost()
    */
   public String getRemoteHost() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
    */
   public RequestDispatcher getRequestDispatcher(String arg0) {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getScheme()
    */
   public String getScheme() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getServerName()
    */
   public String getServerName() {
      return null;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#getServerPort()
    */
   public int getServerPort() {
      return 0;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#isSecure()
    */
   public boolean isSecure() {
      return false;
   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#removeAttribute(java.lang.String)
    */
   public void removeAttribute(String arg0) {

   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#setAttribute(java.lang.String, java.lang.Object)
    */
   public void setAttribute(String arg0, Object arg1) {

   }

   /* (non-Javadoc)
    * @see javax.servlet.ServletRequest#setCharacterEncoding(java.lang.String)
    */
   public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {

   }

   private void parseParametersFromRequest(String paramString) {
      //the last line of the request should have the parameters
      if (StringUtil.isNotNullOrEmpty(paramString)) {
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
            };
            String value = null;
            try {
               value = keyAndValue.length >= 2 ? URLDecoder.decode(keyAndValue[1], CharsetConstants.UTF8.name()) : null;
            } catch (UnsupportedEncodingException e) {
               value = URLDecoder.decode(keyAndValue[1]);
               if (value == null) {
                  value = keyAndValue[1];
               }
            }
            Set<String> values = parameterMap.get(key);
            if (values == null) {
               values = new HashSet<String>();
               parameterMap.put(key, values);
            }
            values.add(value);
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
                  return subHeader.length > 1 ? StringUtil.deQuote(subHeader[1]) : "";
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
