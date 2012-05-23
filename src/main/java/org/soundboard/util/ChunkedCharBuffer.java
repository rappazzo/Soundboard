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

package org.soundboard.util;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

/**
 * A ChunkedCharBuffer operates simlarly to a java.lang.StringBuffer. However,
 * it requires less contiguous memory to manage large datasets by aligning
 * chunks of contiguous memory to maintain the whole buffer. ChunkedCharBuffers
 * are NOT THREAD SAFE, for performance reasons the buffer assumes that it is
 * being operated on with a single thread
 */
public final class ChunkedCharBuffer implements CharSequence, Serializable {

   public static final int DEFAULT_CHUNK_SIZE = 1024;
   public static final int DEFAULT_NUMBER_OF_CHUNKS = 16;
   public static final int MIN_CHUNK_SIZE = 64;
   public static final int MIN_NUMBER_OF_CHUNKS = 1;
   
   int chunkSize;
   char chunks[][];
   int startingChunks;
   int lastChunk;
   int firstFree;
   String charsetName = CharsetConstants.UTF8.name(); // String for serialization

   /**
    * Create a new ChunkedCharBuffer with the passed incremental chunkSize and
    * number of starting chunks. The number of starting chunks is mearly the
    * size of the chunk holder array - which will have to grow whenever
    * chunkSize * startingChunks of data is presented. At that time the growth
    * is simply of the main array
    */
   public ChunkedCharBuffer(int chunkSize, int startingChunks) {
      //rather than assertions just fix bad arguments
      startingChunks = Math.max(startingChunks, MIN_NUMBER_OF_CHUNKS);
      chunkSize = Math.max(chunkSize, MIN_CHUNK_SIZE);
      lastChunk = 0;
      firstFree = 0;
      chunks = new char[startingChunks][];
      this.chunkSize = chunkSize;
      this.startingChunks = startingChunks;
      this.chunks[0] = new char[chunkSize];
   }

   public ChunkedCharBuffer(int chunkSize) {
      this(chunkSize, DEFAULT_NUMBER_OF_CHUNKS);
   }

   public ChunkedCharBuffer() {
      this(DEFAULT_CHUNK_SIZE, DEFAULT_NUMBER_OF_CHUNKS);
   }

   public ChunkedCharBuffer(String startsWith) {
      this(Math.min(startsWith != null ? startsWith.length() : DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_SIZE * 2), Math.max(startsWith != null ? startsWith.length() / DEFAULT_NUMBER_OF_CHUNKS : DEFAULT_NUMBER_OF_CHUNKS, DEFAULT_NUMBER_OF_CHUNKS));
      append(startsWith);
   }

   public int size() {
      return length();
   }

   public int length() {
      return (lastChunk * chunkSize) + firstFree;
   }

   public int getChunkSize() {
      return chunkSize;
   }

   public Charset getCharset() {
      return Charset.forName(charsetName);
   }

   public void setCharset(Charset charset) {
      if (charset != null) {
         this.charsetName = charset.name();
      }
   }

   /**
    * Append a single char to the buffer - if there is space on the current
    * chunk then whoopee this is easy - else regrow to make room
    */
   public ChunkedCharBuffer append(char value) {
      ensureCapacity();
      chunks[lastChunk][firstFree++] = value;
      return this;
   }

   public ChunkedCharBuffer append(int i) {
      return append(String.valueOf(i));
   }

   /**
    * Convenience method to append a string buffer to the buffer,
    * though this looks inefficient, it is actually pretty good,
    * the toString() method o(n StringBuffer shares the data array
    * between the string and the buffer, and the append(String)
    * method on this buffer, copies directly from the String array
    * into the buffer arrays without an extra copy
    */
   public ChunkedCharBuffer append(StringBuffer buf) {
      if (buf != null) {
         return append(buf.toString());
      } else {
         return this;
      }
   }

   /**
    * Convenience method to append a java.nio CharBuffer to the buffer
    */
   public ChunkedCharBuffer append(CharBuffer buf) {
      if (buf != null) {
         int remaining = buf.length();
         int available = 0;
         while (remaining > 0) {
            available = ensureCapacity();
            if (available > remaining) {
               available = remaining;
            }
            buf.get(chunks[lastChunk], firstFree, available);
            remaining -= available;
            firstFree += available;
         }
      }
      return this;
   }

   /**
    * Append a string to the buffer - try to disassemble the string in the most
    * optimal size (==chunkSize) and reassemble onto the buffer
    */
   public ChunkedCharBuffer append(String value) {
      if (value != null && value.length() > 0) {
         return append(value, 0, value.length());
      } else {
         return this;
      }
   }

   /**
    * Append a string to the buffer - try to disassemble the string in the most
    * optimal size (==chunkSize) and reassemble onto the buffer
    */
   public ChunkedCharBuffer append(String value, int copyfrom, int strlen) {
      if (value != null && value.length() > 0) {
         int available = 0;
         while (strlen > 0) {
            available = ensureCapacity();
            if (available > strlen) {
               available = strlen;
            }
            value.getChars(copyfrom, copyfrom + available, chunks[lastChunk], firstFree);
            strlen -= available;
            copyfrom += available;
            firstFree += available;
         }
      }
      return this;
   }

   /**
    * Convenience method to append an entire char[] area to the buffer
    */
   public ChunkedCharBuffer append(char chars[]) {
      if (chars != null && chars.length > 0) {
         return append(chars, 0, chars.length);
      } else {
         return this;
      }
   }

   /**
    * Append a char[] area to the buffer - using System.arrayCopy we can move
    * the array into the chunks directly
    */
   public ChunkedCharBuffer append(char chars[], int start, int length) {
      if (chars != null && chars.length > 0) {
         int copyfrom = start;
         int available = 0;
         while (length > 0) {
            available = ensureCapacity();
            if (available > length) {
               available = length;
            }
            System.arraycopy(chars, copyfrom, chunks[lastChunk], firstFree, available);
            length -= available;
            copyfrom += available;
            firstFree += available;
         }
      }
      return this;
   }

   /**
    * Read the contents of the reader into the buffer
    */
   public ChunkedCharBuffer append(Reader reader) throws IOException {
      if (reader != null) {
         int charsRead = 0;
         int available = 0;
         while (charsRead != -1) {
            available = ensureCapacity();
            charsRead = reader.read(chunks[lastChunk], firstFree, available);
            if (charsRead > 0) {
               firstFree += charsRead;
            }
         }
      }
      return this;
   }

   /**
    * Read the contents of ChunkedCharBuffer (using the ChunkedCharBuffer's
    * reader) into the buffer
    */
   public ChunkedCharBuffer append(ChunkedCharBuffer buffer) {
      if (buffer != null) {
         try {
            return append(buffer.toReader());
         } catch (IOException ioe) {
            throw new RuntimeException("Error reading from passed ChunkedCharBuffer.toReader() source in to current chunkedbuffer.", ioe);
         }
      } else {
         return this;
      }
   }

   /**
    * @deprecated no support for unicode characters - do not use this
    * Convenience method to read all of the contents of the byte array into the buffer
    */
   @Deprecated public ChunkedCharBuffer append(byte[] bytes) {
      if (bytes != null) {
         return append(bytes, 0, bytes.length);
      } else {
         return this;
      }
   }

   /**
    * @deprecated no support for unicode characters - do not use this
    * Read the contents of the byte array into the buffer
    */
   @Deprecated public ChunkedCharBuffer append(byte[] bytes, int offset, int len) {
      if (bytes != null) {
         try {
            return append(new InputStreamReader(new ByteArrayInputStream(bytes, offset, len), charsetName));
         } catch (IOException e) {
            //no-op there are no IOExceptions on byte array sources
            throw new RuntimeException("Error reading from byte[] source in to chunkedbuffer", e);
         }
      } else {
         return this;
      }
   }

   /**
    * Expand the capacity of both the main and data chunks as necessary to hold at least
    * one more char
    * @return the number of chars available for writing on the lastChunk, i.e. the current capacity
    */
   private int ensureCapacity() {
      //the amount of space available in the last chunk
      int available = chunkSize - firstFree;
      if (available == 0) {
         if (lastChunk + 1 == chunks.length) {
            //reconsider main array growth strategy
            int len = chunks.length;
            char newarray[][] = new char[(len + 1) * 2][];
            System.arraycopy(chunks, 0, newarray, 0, len);
            chunks = newarray;
         }
         //add a data chunk
         chunks[++lastChunk] = new char[chunkSize];
         available = chunkSize;
         firstFree = 0;
      }
      return available;
   }

   /**
    * Allocate a substring from the chunked buffer to the end
    * @param start - the starting index
    */
   public String substring(int start) {
      return substring(start, length());
   }

   /**
    * Allocate a substring from the chunked buffer
    * @param start - the starting index
    * @param end - the end index
    */
   public String substring(int start, int end) {
      if (start < 0) {
         throw new StringIndexOutOfBoundsException(start);
      }
      if (end > length()) {
         throw new StringIndexOutOfBoundsException(end);
      }
      if (start > end) {
         throw new StringIndexOutOfBoundsException(end - start);
      }
      return fillStringBuffer(new StringBuffer(end - start), start / chunkSize, start % chunkSize, end - start).toString();
   }

   /**
    * Allocate a ChunkedCharBuffer from the chunked buffer to the end
    * @param start - the starting index
    */
   public ChunkedCharBuffer subbuffer(int start) {
      return subbuffer(start, length());
   }

   /**
    * Allocate a ChunkedCharBuffer from the chunked buffer
    * @param start - the starting index
    * @param end - the end index
    */
   public ChunkedCharBuffer subbuffer(int start, int end) {
      if (start < 0) {
         throw new StringIndexOutOfBoundsException(start);
      }
      if (end > length()) {
         throw new StringIndexOutOfBoundsException(end);
      }
      if (start > end) {
         throw new StringIndexOutOfBoundsException(end - start);
      }
      return fillChunkedCharBuffer(new ChunkedCharBuffer(chunkSize, (end - start) / chunkSize), start / chunkSize, start % chunkSize, end - start);
   }

   /**
    * @see java.lang.CharSequence#subSequence(int, int) This sequence is a read
    *      only view into this ChunkedCharBuffer changes to the buffer are
    *      reflected in the sequence, and may damage the integrity of the
    *      sequence, for a copy of the buffer use substring
    */
   public CharSequence subSequence(final int start, final int end) {
      final ChunkedCharBuffer buffer = this;
      if (start < 0) {
         throw new StringIndexOutOfBoundsException(start);
      }
      if (end > length()) {
         throw new StringIndexOutOfBoundsException(end);
      }
      if (start > end) {
         throw new StringIndexOutOfBoundsException(end - start);
      }
      //return a read only view into the chunked char buffer
      return new CharSequence() {
         private int hash = 0;

         public int length() {
            return end - start;
         }

         public char charAt(int index) {
            return buffer.charAt(index + start);
         }

         public CharSequence subSequence(int subStart, int subEnd) {
            return buffer.subSequence(start + subStart, start + subEnd);
         }

         @Override public String toString() {
            return buffer.substring(start, end);
         }

         @Override public int hashCode() {
            int h = hash;
            //hashcode following the style of String
            if (h == 0) {
               int len = length();
               for (int i = 0; i < len; i++) {
                  h = 31 * h + charAt(i);
               }
               hash = h;
            }
            return h;
         }

         @Override public boolean equals(Object obj) {
            if (this == obj) {
               return true;
            }
            if (obj == null) {
               return false;
            }
            try {
               CharSequence other = (CharSequence)obj;
               int length = length();
               if (other.length() != length) {
                  return false;
               }
               for (int i = 0; i < length; i++) {
                  if (charAt(i) != other.charAt(i)) {
                     return false;
                  }
               }
            } catch (ClassCastException e) {
               return false;
            }
            return true;
         }
      };
   }

   /**
    * Create a new string from the entire chunked buffer
    */
   @Override public String toString() {
      int length = length();
      return fillStringBuffer(new StringBuffer(length), 0, 0, length).toString();
   }

   /**
    * Write the contents of the buffer to the writer
    */
   public void writeTo(Writer writer) throws IOException {
      int length = length();
      int stopChunk = length / chunkSize;
      int stopColumn = length % chunkSize;

      for (int i = 0; i < stopChunk; i++) {
         writer.write(chunks[i], 0, chunkSize);
      }

      if (stopColumn > 0) {
         writer.write(chunks[stopChunk], 0, stopColumn);
      }
   }

   public char charAt(int pos) {
      if ((pos < 0) || (pos >= length())) {
         throw new StringIndexOutOfBoundsException(pos);
      }
      int startChunk = pos / chunkSize;
      return chunks[startChunk][pos % chunkSize];
   }

   /**
    * Returns this after resulting replacing all occurrences of oldChar in this
    * buffer with newChar.
    */
   public ChunkedCharBuffer replace(char oldChar, char newChar) {
      for (int pos = 0; pos < length(); pos++) {
         int startChunk = pos / chunkSize;
         if (chunks[startChunk][pos % chunkSize] == oldChar) {
            chunks[startChunk][pos % chunkSize] = newChar;
         }
      }
      return this;
   }

   /**
    * Replaces the character at the specific location with the given newChar.
    */
   public ChunkedCharBuffer replaceCharAt(int pos, char newChar) {
      if (pos < 0 || pos > length()) {
         if ((pos < 0) || (pos >= length())) {
            throw new StringIndexOutOfBoundsException(pos);
         }
      }
      chunks[pos / chunkSize][pos % chunkSize] = newChar;
      return this;
   }

   /**
    * return a new ChunkedCharBuffer which contains the same data as this, but reversed
    */
   public ChunkedCharBuffer reverse() {
      ChunkedCharBuffer reversedBuffer = new ChunkedCharBuffer(this.chunkSize, this.chunks.length);
      for (int i = size() - 1; i >= 0; i++) {
         reversedBuffer.append(charAt(i));
      }
      return reversedBuffer;
   }

   /**
    * Create a reader with access to the data in the buffer, this allows classes
    * to read directly out of the buffer. This reader will only read within the
    * bounds specified.
    * @param beginIndex inclusive
    * @param endIndex exclusive
    */
   public Reader toRangeReader(final int beginIndex, final int endIndex) {
      if (beginIndex < 0) {
         throw new StringIndexOutOfBoundsException(beginIndex);
      }
      if (endIndex > length()) {
         throw new StringIndexOutOfBoundsException("end index is greater than length: " + endIndex + " > " + length());
      }
      if (beginIndex > endIndex) {
         throw new StringIndexOutOfBoundsException("begin index is greater than endindex: " + beginIndex + " > " + endIndex);
      }
      return new Reader() {
         int index = beginIndex;
         int marked = 0;
         boolean closed = false;
         boolean eos = false;

         private void ensureOpen() throws IOException {
            if (closed) {
               throw new IOException("ChunkedCharBuffer Reader Closed by request");
            }
         }

         @Override public int read() throws IOException {
            ensureOpen();
            //already reached end-of-stream
            if (eos) {
               return -1;
            }
            int end = endIndex;
            //indicate the end-of-stream
            if (index >= end) {
               eos = true;
               return -1;
            }
            char cbuf[] = new char[1];
            getChars(index / chunkSize, index % chunkSize, 1, cbuf, 0);
            index++;
            return cbuf[0] & 0xff;
         }

         @Override public int read(char cbuf[]) throws IOException {
            return read(cbuf, 0, cbuf.length);
         }

         @Override public int read(char cbuf[], int off, int len) throws IOException {
            ensureOpen();
            //already reached end-of-stream
            if (eos) {
               return -1;
            }
            int end = endIndex;
            //indicate the end-of-stream
            if (index >= end) {
               eos = true;
               return -1;
            }
            len = Math.min(len, end - index);
            getChars(index / chunkSize, index % chunkSize, len, cbuf, off);
            index += len;
            return len;
         }

         @Override public boolean ready() throws IOException {
            ensureOpen();
            return true;
         }

         @Override public void close() throws IOException {
            ensureOpen();
            closed = true;
         }

         @Override public void reset() throws IOException {
            ensureOpen();
            index = marked;
            eos = false;
         }

         @Override public void mark(int readAheadLimit) throws IOException {
            ensureOpen();
            marked = index;
         }

         @Override  public boolean markSupported() {
            return true;
         }

      };
   }

   /**
    * Create a reader with access to the data in the buffer, this allows classes
    * to read directly out of the buffer
    */
   public Reader toReader() {
      return toRangeReader(0, length());
   }

   /**
    * Create a Writer with direct write access to the data in the buffer, this allows classes
    * to write directly into the buffer via the Writer interface
    */
   public Writer toWriter() {
      return new Writer(this) {
         boolean closed = false;
         
         @Override public void write(int c) throws IOException {
            ensureOpen();
            super.write(c);
         }

         @Override public void write(char cbuf[], int off, int len) throws IOException {
            ensureOpen();
            ChunkedCharBuffer.this.append(cbuf, off, len);
         }

         @Override public void write(String str, int off, int len) throws IOException {
            ensureOpen();
            ChunkedCharBuffer.this.append(str, off, len);
         }

         @Override  public void close() throws IOException {
            closed = true;
         }
         
         @Override  public void flush() throws IOException {
            //no-op
         }
         
         private void ensureOpen() throws IOException {
            if (closed) {
               throw new IOException("ChunkedCharBuffer Writer closed by request");
            }
         }
      };
   }

   /**
    * Create an InputStream with access to the data in the buffer, this allows
    * classes to read directly out of the buffer
    */
   public InputStream toInputStream() throws IOException {
      return toChunkedByteBuffer(Charset.forName(charsetName)).toInputStream();
   }

   /**
    * Create a copy of the current data converted to a ChunkedByteBuffer
    */
   public ChunkedByteBuffer toChunkedByteBuffer(Charset charset) throws CharacterCodingException, UnsupportedEncodingException {
      //configure the encoder the way that string encoder does
      CharsetEncoder encoder = charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
      float avgBytesPerChar = encoder.averageBytesPerChar();
      ChunkedByteBuffer cbb = new ChunkedByteBuffer((int)(chunkSize * avgBytesPerChar), chunks.length);
      cbb.setCharset(charset);

      int length = length();
      int stopChunk = length / chunkSize;
      int stopColumn = length % chunkSize;

      //encoding per char array chunk is much faster than the CharSequence interface to CharBuffer
      for (int i = 0; i < stopChunk; i++) {
         ByteBuffer bb = encoder.encode(CharBuffer.wrap(chunks[i], 0, chunkSize));
         cbb.append(bb);
      }
      if (stopColumn > 0) {
         ByteBuffer bb = encoder.encode(CharBuffer.wrap(chunks[stopChunk], 0, stopColumn));
         cbb.append(bb);
      }
      return cbb;

   }

   /**
    * Create a copy of the current data converted to a ChunkedByteBuffer, this
    * method defaults to using UTF8 for the character decoding
    */
   public ChunkedByteBuffer toChunkedByteBuffer() throws CharacterCodingException, UnsupportedEncodingException {
      return toChunkedByteBuffer(Charset.forName(charsetName));
   }

   /**
    * Tests if this buffer starts with the specified prefix beginning
    * a specified index.
    * @param   prefix    the prefix
    * @param   toffset   where to begin looking in the string
    */
   public boolean startsWith(String prefix, int index) {
      int plen = prefix.length();
      int count = 0;
      if ((index < 0) || (index > (length() - plen))) {
         return false;
      }
      while (--plen >= 0) {
         if (charAt(index++) != prefix.charAt(count++)) {
            return false;
         }
      }
      return true;
   }

   /**
    * Tests if this buffer starts with the specified prefix
    */
   public boolean startsWith(String prefix) {
      return startsWith(prefix, 0);
   }

   /**
    * Returns the index within this buffer of the first occurrence of the
    * specified substring, starting at the begining of this buffer
    * @param queryStr the substring to search for.
    */
   public int indexOf(String queryStr) {
      return indexOf(queryStr, 0);
   }

   /**
    * Returns the index within this buffer of the first occurrence of the
    * specified substring, starting at the specified index. There is no
    * restriction on the value of fromIndex. If it is negative, it has the same
    * effect as if it were zero: this entire buffer may be searched. If it is
    * greater than the length of this buffer, it has the same effect as if it
    * were equal to the length of this string: -1 is returned. When there is
    * time to do so, further research should be made into enhancing the
    * performance of this method, current seek time is nearly 7x times the
    * equvilent call on String. The time is entirely spent dealing with
    * additional bounds checking required by the simple implementation, perhaps
    * an enhanced version could be more aggressive about bounds checking, or
    * could use a more advanced search algorithm such as Boyer-Moore or
    * Knuth-Morris-Pratt
    * @param queryStr the substring to search for
    * @param fromIndex the index to start the search from
    */
   public int indexOf(String queryStr, int fromIndex) {
      int length = length();
      int querylen = queryStr.length();
      int max = length - querylen;
      if (fromIndex < 0) {
         fromIndex = 0;
      } else if (fromIndex >= length) {
         if (length == 0 && fromIndex == 0 && querylen == 0) {
            //there is an empty string at index 0 in an empty string
            return 0;
         }
         return -1;
      }
      if (querylen == 0) {
         return fromIndex;
      }

      char firstQueryChar = queryStr.charAt(0);
      int chunk = fromIndex / chunkSize;
      int i = fromIndex % chunkSize;
      int index = i + (chunk * chunkSize);

      //iterating chunks
      while (chunk <= lastChunk) {
         //look for first character
         while (index <= max && i < chunkSize && chunks[chunk][i] != firstQueryChar) {
            i++;
            index++;
         }
         if (index > max) {
            return -1;
         }
         if (i == chunkSize) {
            //wrap to the next chunk
            i = 0;
            chunk++;
         } else {
            //we found a first char match
            int j = i + 1;
            if (i == chunkSize - 1) {
               j = 0;
               i = 0;
               chunk++;
            } else {
               i++;
            }
            int followIndex = index + 1;
            int endIndex = followIndex + querylen - 1;
            int k = 1;
            int followChunk = followIndex / chunkSize;
            while (followIndex <= endIndex) {
               while (j < chunkSize && k < querylen && followIndex <= length) {
                  followIndex++;
                  if (chunks[followChunk][j++] != queryStr.charAt(k)) {
                     //look for first char again
                     break;
                  } else {
                     k++;
                  }
               }
               if (k == querylen) {
                  //full match
                  return index;
               }
               if (j != chunkSize) {
                  break;
               }
               //continue to the next chunk
               j = 0;
               followChunk++;
            }
            //we are pointing at a good i
            index++;
         }
      }
      return -1;
   }

   /**
    * Returns the index within this string of the first occurrence of the
    * specified character, starting the search at the begining of the buffer
    */
   public int indexOf(char ch) {
      return indexOf(ch, 0);
   }

   /**
    * Returns the index within this string of the first occurrence of the
    * specified character, starting the search at the specified index. There is
    * no restriction on the value of fromIndex. If it is negative, it has the
    * same effect as if it were zero: this entire buffer may be searched. If it
    * is greater than the length of this buffer, it has the same effect as if it
    * were equal to the length of this string: -1 is returned.
    */
   public int indexOf(char ch, int fromIndex) {
      int length = length();
      if (fromIndex < 0) {
         fromIndex = 0;
      } else if (fromIndex >= length) {
         return -1;
      }
      int chunk = fromIndex / chunkSize;
      int i = fromIndex % chunkSize;
      int index = i + (chunk * chunkSize);

      for (; chunk <= lastChunk; chunk++) {
         for (; index < length && i < chunkSize; i++, index++) {
            if (chunks[chunk][i] == ch) {
               return index;
            }
         }
         i = 0;
      }
      return -1;
   }

   /**
    * Returns the index within this string of the last occurrence of the
    * specified substring.
    */
   public int lastIndexOf(String queryStr) {
      return lastIndexOf(queryStr, length());
   }

   /**
    * Returns the index within this string of the last occurrence of the
    * specified substring. There is no restriction on the value of fromIndex. If
    * it is negative, it has the same effect as if it were zero: this entire
    * buffer may be searched. If it is greater than the length of this buffer,
    * it has the same effect as if it were equal to the length of this string:
    * -1 is returned. When there is time to do so, further research should be
    * made into enhancing the performance of this method, current seek time is
    * nearly 4x times the equvilent call on String. The time is entirely spent
    * dealing with additional bounds checking required by the simple
    * implementation, perhaps an enhanced version could be more aggressive about
    * bounds checking, or could use a more advanced search algorithm such as
    * Boyer-Moore or Knuth-Morris-Pratt
    * @param queryStr the substring to search for
    * @param fromIndex the index to start the search from
    */
   public int lastIndexOf(String queryStr, int fromIndex) {
      int querylen = queryStr.length();
      int length = length();

      if (fromIndex < 0) {
         return -1;
      }
      if (fromIndex > (length - querylen)) {
         fromIndex = length - querylen;
      }
      if (querylen == 0) {
         return fromIndex;
      }

      int min = querylen - 1;
      char lastQueryChar = queryStr.charAt(min);
      fromIndex += min;
      int chunk = fromIndex / chunkSize;
      int i = fromIndex % chunkSize;
      int index = i + (chunk * chunkSize);

      //iterating chunks
      while (chunk >= 0) {
         //look for first character
         while (index >= min && i >= 0 && chunks[chunk][i] != lastQueryChar) {
            i--;
            index--;
         }
         if (index < min) {
            return -1;
         }
         if (i == -1) {
            //wrap to the next chunk
            i = chunkSize - 1;
            chunk--;
         } else {
            //we found a first char match
            int j = i - 1;
            if (i == 0) {
               j = chunkSize - 1;
               i = chunkSize - 1;
               chunk--;
            } else {
               i--;
            }
            int followIndex = index - 1;
            int endIndex = followIndex - (querylen - 1);
            int k = querylen - 2;
            int followChunk = followIndex / chunkSize;
            while (followIndex >= endIndex) {
               while (j >= 0 && k >= 0 && followIndex >= 0) {
                  followIndex--;
                  if (chunks[followChunk][j--] != queryStr.charAt(k)) {
                     //look for first char again
                     break;
                  } else {
                     k--;
                  }
               }
               if (k == -1) {
                  //full match
                  return endIndex + 1;
               }
               if (j != -1) {
                  break;
               }
               //continue to the next chunk
               j = chunkSize - 1;
               followChunk--;
            }
            //we are pointing at a good i
            index--;
         }
      }
      return -1;
   }

   /**
    * Returns a java.util.regex.Matcher for this buffer.
    * @param regex the delimiting regular expression
    * @return Matcher for the matches against the buffer
    * @throws PatternSyntaxException if the regular expression's syntax is
    *         invalid
    * @see java.util.regex.Pattern
    * @see java.util.regex.Matcher
    */
   public Matcher matcher(String regex) {
      return Pattern.compile(regex).matcher(this);
   }

   /**
    * The array returned by this method contains each substring of this buffer
    * that is terminated by another substring that matches the given expression
    * or is terminated by the end of the buffer. The substrings in the array are
    * in the order in which they occur in this buffer. If the expression does
    * not match any part of the input then the resulting array has just one
    * element, namely this string.
    * @param regex the delimiting regular expression
    * @return the array of strings computed by splitting this string around
    *         matches of the given regular expression
    * @throws PatternSyntaxException if the regular expression's syntax is
    *         invalid
    * @see java.util.regex.Pattern
    */
   public String[] split(String regex) {
      return Pattern.compile(regex).split(this, 0);
   }

   /**
    * The array returned by this method contains each substring of this buffer
    * that is terminated by another substring that matches the given expression
    * or is terminated by the end of the buffer. The substrings in the array are
    * in the order in which they occur in this buffer. If the expression does
    * not match any part of the input then the resulting array has just one
    * element, namely this string.
    * @param regex the delimiting regular expression
    * @param limit the result threshold, as described above
    * @return the array of strings computed by splitting this string around
    *         matches of the given regular expression
    * @throws PatternSyntaxException if the regular expression's syntax is
    *         invalid
    * @see java.util.regex.Pattern
    */
   public String[] split(String regex, int limit) {
      return Pattern.compile(regex).split(this, limit);
   }

   /**
    * Valid implmentation of the .equals method for ChunkedCharBuffer
    */
   @Override public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      try {
         ChunkedCharBuffer other = (ChunkedCharBuffer)obj;
         int length = length();
         if (other.length() != length) {
            return false;
         }
         int slowCheckFrom = 0;
         //we can optmize a lot if our chunksize is the same
         if (chunkSize == other.chunkSize) {
            for (int i = 0; i < lastChunk; i++) {
               if (!Arrays.equals(chunks[i], other.chunks[i])) {
                  return false;
               }
            }
            slowCheckFrom = lastChunk * chunkSize;
         }
         //this method of checking is much slower, since we are doing bounds checks
         for (int i = slowCheckFrom; i < length; i++) {
            if (charAt(i) != other.charAt(i)) {
               return false;
            }
         }
      } catch (ClassCastException e) {
         return false;
      }
      return true;
   }

   /**
    * Fill the dest array with content from the buffer
    * @param srcBegin start index (inclusive) in the buffer
    * @param srcEnd end index (exclusive) in the buffer
    * @param dest the array to fill
    * @param destBegin start index in the destination array
    * @throws ArrayIndexOutOfBounds exception if the dest array cannot hold the requested data
    */
   public void getChars(int srcBegin, int srcEnd, char[] dest, int destBegin) throws ArrayIndexOutOfBoundsException {
      if (srcBegin < 0) {
         throw new StringIndexOutOfBoundsException(srcBegin);
      }
      if (srcEnd > length()) {
         throw new StringIndexOutOfBoundsException(srcEnd);
      }
      if (srcBegin > srcEnd) {
         throw new StringIndexOutOfBoundsException(srcEnd - srcBegin);
      }
      getChars(srcBegin / chunkSize, srcBegin % chunkSize, srcEnd - srcBegin, dest, destBegin);
   }

   private void getChars(int startChunk, int startColumn, int length, char[] dest, int destBegin) throws ArrayIndexOutOfBoundsException {
      int stop = (startChunk * chunkSize) + startColumn + length;
      int stopChunk = stop / chunkSize;
      int stopColumn = stop % chunkSize;

      for (int i = startChunk; i < stopChunk; i++) {
         int size = chunkSize - startColumn;
         System.arraycopy(chunks[i], startColumn, dest, destBegin, size);
         destBegin += size;
         startColumn = 0;
      }

      if (stopColumn > 0) {
         System.arraycopy(chunks[stopChunk], startColumn, dest, destBegin, stopColumn - startColumn);
      }
   }

   /**
    * $TS - remove for Unicode compliance
    * @deprecated
    * Get the characters a bytes using this charset specified in this ChunkedCharBuffer
    */
   @Deprecated public byte[] getBytes() throws UnsupportedEncodingException {
      return toString().getBytes(charsetName);
   }

   /**
    * Write the chunk data into the string bufffer, assume the buffer is well
    * allocated
    */
   private StringBuffer fillStringBuffer(StringBuffer sb, int startChunk, int startColumn, int length) {
      int stop = (startChunk * chunkSize) + startColumn + length;
      int stopChunk = stop / chunkSize;
      int stopColumn = stop % chunkSize;

      for (int i = startChunk; i < stopChunk; i++) {
         sb.append(chunks[i], startColumn, chunkSize - startColumn);
         startColumn = 0;
      }

      if (stopColumn > 0) {
         sb.append(chunks[stopChunk], startColumn, stopColumn - startColumn);
      }

      return sb;
   }

   /**
    * Write the chunk data into the ChunkedCharBuffer, assume the buffer is well
    * allocated
    */
   private ChunkedCharBuffer fillChunkedCharBuffer(ChunkedCharBuffer ccb, int startChunk, int startColumn, int length) {
      int stop = (startChunk * chunkSize) + startColumn + length;
      int stopChunk = stop / chunkSize;
      int stopColumn = stop % chunkSize;

      for (int i = startChunk; i < stopChunk; i++) {
         ccb.append(chunks[i], startColumn, chunkSize - startColumn);
         startColumn = 0;
      }

      if (stopColumn > 0) {
         ccb.append(chunks[stopChunk], startColumn, stopColumn - startColumn);
      }

      return ccb;
   }

}
