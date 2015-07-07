/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.grizzly.http.util;

import java.io.CharConversionException;
import java.io.IOException;

/**
 *  All URL decoding happens here. This way we can reuse, review, optimize
 *  without adding complexity to the buffers.
 *
 *  The conversion will modify the original buffer.
 *
 *  @author Costin Manolache
 */
public final class UDecoder {

    private static final String ALLOW_ENCODED_SLASH_NAME = "org.glassfish.grizzly.util.buf.UDecoder.ALLOW_ENCODED_SLASH";
    public static final boolean ALLOW_ENCODED_SLASH =
            Boolean.valueOf(System.getProperty(ALLOW_ENCODED_SLASH_NAME, "false"));
    private boolean allowEncodedSlash;

    public UDecoder() {
        this(ALLOW_ENCODED_SLASH);
    }

    public UDecoder(boolean allowEncodedSlash) {
        this.allowEncodedSlash = allowEncodedSlash;
    }

    /** URLDecode, will modify the source.  Includes converting
     *  '+' to ' '.
     */
    public void convert(ByteChunk mb)
            throws IOException {
        convert(mb, true);
    }

    /**
     * URLDecode the {@link ByteChunk}
     */
    public void convert(ByteChunk mb, boolean query) throws IOException {
        convert(mb, query, allowEncodedSlash);
    }

    /**
     * URLDecode the {@link ByteChunk}
     */
    public static void convert(ByteChunk mb, boolean query, boolean allowEncodedSlash)
            throws IOException {
        int start = mb.getStart();
        byte buff[] = mb.getBytes();
        int end = mb.getEnd();

        int idx = ByteChunk.indexOf(buff, start, end, '%');
        int idx2 = -1;
        if (query) {
            idx2 = ByteChunk.indexOf(buff, start, end, '+');
        }
        if (idx < 0 && idx2 < 0) {
            return;
        }

        // idx will be the smallest positive inxes ( first % or + )
        if (idx2 >= 0 && idx2 < idx) {
            idx = idx2;
        }
        if (idx < 0) {
            idx = idx2;
        }

        boolean noSlash = !(allowEncodedSlash || query);

        for (int j = idx; j < end; j++, idx++) {
            if (buff[j] == '+' && query) {
                buff[idx] = (byte) ' ';
            } else if (buff[j] != '%') {
                buff[idx] = buff[j];
            } else {
                // read next 2 digits
                if (j + 2 >= end) {
                    throw new CharConversionException("EOF");
                }
                byte b1 = buff[j + 1];
                byte b2 = buff[j + 2];
                if (!isHexDigit(b1) || !isHexDigit(b2)) {
                    throw new CharConversionException("isHexDigit");
                }

                j += 2;
                int res = x2c(b1, b2);
                if (noSlash && res == '/') {
                    throw new CharConversionException("Encoded slashes are not allowed by default.  To enable encoded"
                            + "slashes, set the property " + ALLOW_ENCODED_SLASH_NAME + " to true.");
                }
                buff[idx] = (byte) res;
            }
        }

        mb.setEnd(idx);

    }

    // -------------------- Additional methods --------------------
    // XXX What do we do about charset ????
    /** In-buffer processing - the buffer will be modified
     *  Includes converting  '+' to ' '.
     */
    public void convert(CharChunk mb)
            throws IOException {
        convert(mb, true);
    }

    /** In-buffer processing - the buffer will be modified
     */
    public static void convert(CharChunk mb, boolean query)
            throws IOException {
        //        log( "Converting a char chunk ");
        int start = mb.getStart();
        char buff[] = mb.getBuffer();
        int cend = mb.getEnd();

        int idx = CharChunk.indexOf(buff, start, cend, '%');
        int idx2 = -1;
        if (query) {
            idx2 = CharChunk.indexOf(buff, start, cend, '+');
        }
        if (idx < 0 && idx2 < 0) {
            return;
        }

        if (idx2 >= 0 && idx2 < idx) {
            idx = idx2;
        }
        if (idx < 0) {
            idx = idx2;
        }

        for (int j = idx; j < cend; j++, idx++) {
            final char c = buff[j];
            if (c == '+' && query) {
                buff[idx] = ' ';
            } else if (c != '%') {
                buff[idx] = c;
            } else {
                // read next 2 digits
                if (j + 2 >= cend) {
                    // invalid
                    throw new CharConversionException("EOF");
                }
                char b1 = buff[j + 1];
                char b2 = buff[j + 2];
                if (!isHexDigit(b1) || !isHexDigit(b2)) {
                    throw new CharConversionException("isHexDigit");
                }

                j += 2;
                int res = x2c(b1, b2);
                buff[idx] = (char) res;
            }
        }
        mb.setEnd(idx);
    }
    
    /** URLDecode, will modify the source
     *  Includes converting  '+' to ' '.
     */
    public void convert(MessageBytes mb)
            throws IOException {
        convert(mb, true);
    }

    /**
     * URLDecode, will modify the source
     */
    public void convert(MessageBytes mb, boolean query) throws IOException {
        convert(mb, query, allowEncodedSlash);
    }

    /** URLDecode, will modify the source
     */
    public static void convert(MessageBytes mb, boolean query, boolean allowEncodingSlash)
            throws IOException {

        switch (mb.getType()) {
            case MessageBytes.T_STR:
                String strValue = mb.toString();
                if (strValue == null) {
                    return;
                }
                mb.setString(convert(strValue, query));
                break;
            case MessageBytes.T_CHARS:
                CharChunk charC = mb.getCharChunk();
                convert(charC, query);
                break;
            case MessageBytes.T_BYTES:
                ByteChunk bytesC = mb.getByteChunk();
                convert(bytesC, query, allowEncodingSlash);
                break;
        }
    }

    // XXX Old code, needs to be replaced !!!!
    //
    public static String convert(String str) {
        return convert(str, true);
    }

    public static String convert(String str, boolean query) {
        if (str == null) {
            return null;
        }

        if ((!query || str.indexOf('+') < 0) && str.indexOf('%') < 0) {
            return str;
        }

        StringBuilder dec = new StringBuilder();    // decoded string output
        int strPos = 0;
        int strLen = str.length();

        dec.ensureCapacity(str.length());
        while (strPos < strLen) {
            int laPos;        // lookahead position

            // look ahead to next URLencoded metacharacter, if any
            for (laPos = strPos; laPos < strLen; laPos++) {
                char laChar = str.charAt(laPos);
                if (laChar == '+' && query || laChar == '%') {
                    break;
                }
            }

            // if there were non-metacharacters, copy them all as a block
            if (laPos > strPos) {
                dec.append(str.substring(strPos, laPos));
                strPos = laPos;
            }

            // shortcut out of here if we're at the end of the string
            if (strPos >= strLen) {
                break;
            }

            // process next metacharacter
            char metaChar = str.charAt(strPos);
            if (metaChar == '+') {
                dec.append(' ');
                strPos++;
            } else if (metaChar == '%') {
                // We throw the original exception - the super will deal with
                // it
                //                try {
                dec.append((char) Integer.parseInt(str.substring(strPos + 1, strPos + 3), 16));
                strPos += 3;
            }
        }

        return dec.toString();
    }

    private static boolean isHexDigit(int c) {
        return c >= '0' && c <= '9'
                || c >= 'a' && c <= 'f'
                || c >= 'A' && c <= 'F';
    }

    private static int x2c(byte b1, byte b2) {
        int digit = b1 >= 'A' ? (b1 & 0xDF) - 'A' + 10 : b1 - '0';
        digit *= 16;
        digit += b2 >= 'A' ? (b2 & 0xDF) - 'A' + 10 : b2 - '0';
        return digit;
    }

    private static int x2c(char b1, char b2) {
        int digit = b1 >= 'A' ? (b1 & 0xDF) - 'A' + 10 : b1 - '0';
        digit *= 16;
        digit += b2 >= 'A' ? (b2 & 0xDF) - 'A' + 10 : b2 - '0';
        return digit;
    }

    public boolean isAllowEncodedSlash() {
        return allowEncodedSlash;
    }

    /**
     * Override the default value
     * @param allowEncodedSlash
     */
    public void setAllowEncodedSlash(boolean allowEncodedSlash) {
        this.allowEncodedSlash = allowEncodedSlash;
    }
}
