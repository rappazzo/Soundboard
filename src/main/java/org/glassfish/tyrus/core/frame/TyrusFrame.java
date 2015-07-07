/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
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
 */

package org.glassfish.tyrus.core.frame;

import java.nio.ByteBuffer;
import java.util.Locale;

import org.glassfish.tyrus.core.ProtocolException;
import org.glassfish.tyrus.core.TyrusWebSocket;

/**
 * Frame representation used in Tyrus runtime.
 * <p/>
 * Enriched {@link Frame} representation.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public abstract class TyrusFrame extends Frame {

    private FrameType frameType;

    /**
     * Constructor.
     *
     * @param frame     enriched frame.
     * @param frameType type of the frame.
     */
    protected TyrusFrame(Frame frame, FrameType frameType) {
        super(frame);
        this.frameType = frameType;
    }

    /**
     * Execution part of frame processing.
     *
     * @param socket socket on which the appropriate action will be performed.
     */
    public abstract void respond(TyrusWebSocket socket);

    /**
     * Returns the type of the frame. It is used for distinguishing frame types in monitoring.
     * <p/>
     * TODO: consider moving this to "MonitoredFrame" or something like this;
     *
     * @return type of the frame.
     */
    public FrameType getFrameType() {
        return frameType;
    }

    /**
     * {@link TyrusFrame} factory method.
     *
     * @param frame            original plain frame.
     * @param inFragmentedType type of fragment (text or binary).
     * @param remainder        decoding remainder. Used only for partial text frames.
     * @return new TyrusFrame.
     */
    public static TyrusFrame wrap(Frame frame, byte inFragmentedType, ByteBuffer remainder) {

        switch (frame.getOpcode()) {
            case 0x00:
                if ((inFragmentedType & 0x01) == 0x01) {
                    return new TextFrame(frame, remainder, true);
                } else {
                    return new BinaryFrame(frame, true);
                }
            case 0x01:
                return new TextFrame(frame, remainder);
            case 0x02:
                return new BinaryFrame(frame);
            case 0x08:
                return new CloseFrame(frame);
            case 0x09:
                return new PingFrame(frame);
            case 0x0A:
                return new PongFrame(frame);
            default:
                throw new ProtocolException(String.format("Unknown wrappedFrame type: %s",
                        Integer.toHexString(frame.getOpcode()).toUpperCase(Locale.US)));
        }
    }

    /**
     * An Enumeration of frame types.
     */
    public static enum FrameType {
        /**
         * Text frame.
         *
         * @see org.glassfish.tyrus.core.frame.TextFrame
         */
        TEXT,
        /**
         * Continuation text frame.
         *
         * @see org.glassfish.tyrus.core.frame.TextFrame
         */
        TEXT_CONTINUATION,
        /**
         * Binary frame.
         *
         * @see org.glassfish.tyrus.core.frame.BinaryFrame
         */
        BINARY,
        /**
         * Continuation binary frame.
         *
         * @see org.glassfish.tyrus.core.frame.BinaryFrame
         */
        BINARY_CONTINUATION,
        /**
         * Ping frame.
         *
         * @see org.glassfish.tyrus.core.frame.PingFrame
         */
        PING,
        /**
         * Pong frame.
         *
         * @see org.glassfish.tyrus.core.frame.PongFrame
         */
        PONG,
        /**
         * Close frame.
         *
         * @see org.glassfish.tyrus.core.frame.CloseFrame
         */
        CLOSE
    }
}
