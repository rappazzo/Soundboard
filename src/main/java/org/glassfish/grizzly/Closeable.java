/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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
 */

package org.glassfish.grizzly;

/**
 * General asynchronous closable interface.
 * 
 * <tt>Closeable</tt> interface contains two sets of methods: close* and terminate*,
 * so interface implementations can provide graceful and abrupt releasing of resources.
 * 
 * @author Alexey Stashok
 */
public interface Closeable {
    /**
     * Closes this stream and releases any system resources associated with it.
     * 
     * If the stream is already closed then invoking this 
     * method has no effect.
     * Use this method, when no completion notification is needed.
     */
    public void terminateSilently();

    /**
     * Closes this stream and releases any system resources associated with it.
     * If the stream is already closed then invoking this 
     * method has no effect. 
     *
     * @return {@link java.util.concurrent.Future}, which could be checked in case, if close operation
     *         will be run asynchronously
     */
    public GrizzlyFuture<Closeable> terminate();
    
    /**
     * Gracefully (if supported by the implementation) closes this stream and
     * releases any system resources associated with it.
     * 
     * If the stream is already closed then invoking this 
     * method has no effect.
     * Use this method, when no completion notification is needed.
     */
    public void closeSilently();

    /**
     * Gracefully (if supported by the implementation) closes this stream and
     * releases any system resources associated with it.
     * If the stream is already closed then invoking this 
     * method has no effect. 
     *
     * @return {@link java.util.concurrent.Future}, which could be checked in case, if close operation
     *         will be run asynchronously
     */
    public GrizzlyFuture<Closeable> close();
    
    /**
     * Gracefully closes this stream and releases any system resources associated
     * with it.
     * This operation waits for all pending output data to be flushed before
     * closing the stream.
     * If the stream is already closed then invoking this 
     * method has no effect. 
     *
     * @param completionHandler {@link CompletionHandler} to be called, when
     *  the stream is closed
     * @deprecated please use {@link #close()} with the following {@link
     *  GrizzlyFuture#addCompletionHandler(org.glassfish.grizzly.CompletionHandler)} call
     */
    public void close(CompletionHandler<Closeable> completionHandler);
    
    /**
     * Add the {@link CloseListener}, which will be notified once the stream
     * will be closed.
     * 
     * @param closeListener {@link CloseListener}.
     */
    void addCloseListener(CloseListener closeListener);

    /**
     * Remove the {@link CloseListener}.
     *
     * @param closeListener {@link CloseListener}.
     * @return <tt>true</tt> if the listener was successfully removed, or
     *         <tt>false</tt> otherwise.
     */
    boolean removeCloseListener(CloseListener closeListener);
}
