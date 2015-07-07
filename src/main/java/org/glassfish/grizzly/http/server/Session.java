/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.server;

import java.util.concurrent.ConcurrentMap;
import org.glassfish.grizzly.utils.DataStructures;

/**
 * Simple session object.
 *
 * @author Jeanfrancois Arcand
 */
public class Session {

    /**
     * Cache attribute (thread safe)
     */
    private final ConcurrentMap<String,Object> attributes =
            DataStructures.<String, Object>getConcurrentMap();

    /**
     * A session identifier
     */
    private String id = null;


    /**
     * Is this Session valid.
     */
    private boolean isValid = true;


    /**
     * Timeout
     */
    private long sessionTimeout = -1;


     /**
     * Creation time stamp.
     */
    private long timestamp = -1;



    public Session() {
        this(null);
    }


    /**
     * Create a new session using a session identifier
     * @param id session identifier
     */
    public Session(String id) {
        this.id = id;
    }


    /**
     * Is the current Session valid?
     * @return true if valid.
     */
    public boolean isValid() {
        return isValid;
    }


    /**
     * Set this object as validated.
     * @param isValid
     */
    public void setValid(boolean isValid) {
        this.isValid = isValid;
        if (!isValid) {
            timestamp = -1;
        }
    }


    /**
     * Return the session identifier for this session.
     */
    public String getIdInternal() {
        return id;
    }


    /**
     * Return the session identifier for this session.
     */
    protected void setIdInternal(String id) {
        this.id = id;
    }


    /**
     * Add an attribute to this session.
     * @param key
     * @param value
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Return an attribute.
     *
     * @param key
     * @return an attribute
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }


    /**
     * Remove an attribute.
     * @param key
     * @return true if successful.
     */
    public Object removeAttribute(String key){
        return attributes.remove(key);
    }


    /**
     * Return a {@link ConcurrentMap} of attributes.
     * @return the attributes associated with this session.
     */
    public ConcurrentMap<String,Object> attributes() {
        return attributes;
    }


    /**
     * Return a long representing the maximum idle time (in milliseconds) a session can be.
     * @return a long representing the maximum idle time (in milliseconds) a session can be.
     */
    public long getSessionTimeout() {
        return sessionTimeout;
    }


    /**
     * Set a long representing the maximum idle time (in milliseconds) a session can be.
     * @param sessionTimeout a long representing the maximum idle time (in milliseconds) a session can be.
     */
    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }


    /**
     * @return the timestamp when this session has been created.
     */
    public long getTimestamp() {
        return timestamp;
    }


    /**
     * Set the timestamp when this session has been created.
     * @param timestamp a long representing when the session has been created.
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}
