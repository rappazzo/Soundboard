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

package org.glassfish.grizzly.http.server.util;

import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.util.Constants;
import java.io.IOException;
import java.util.logging.Level;

import org.glassfish.grizzly.Grizzly;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.glassfish.grizzly.http.server.naming.DirContext;
import org.glassfish.grizzly.http.server.naming.NamingContext;
import org.glassfish.grizzly.http.server.naming.NamingException;
import org.glassfish.grizzly.http.util.Ascii;
import org.glassfish.grizzly.http.util.CharChunk;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.grizzly.http.util.MessageBytes;

import org.glassfish.grizzly.utils.Charsets;

/**
 * Mapper, which implements the servlet API mapping rules (which are derived
 * from the HTTP rules).
 *
 * @author Remy Maucherat
 */
@SuppressWarnings({"UnusedDeclaration"})
public class Mapper {

    private final static Logger logger = Grizzly.logger(Mapper.class);

    private static final String DEFAULT_SERVLET =
            System.getProperty("org.glassfish.grizzly.servlet.defaultServlet", "default");

    private static final String JSP_SERVLET =
            System.getProperty("org.glassfish.grizzly.servlet.jspServlet", "jsp");

    private static final CharChunk SLASH = new CharChunk();

    /**
     * Allow replacement of already added {@link Host}, {@link Context}
     * and {@link Wrapper}
     */
    private static boolean allowReplacement = false;

    static {
        try {
            SLASH.append('/');
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    // ----------------------------------------------------- Instance Variables


    /**
     * Array containing the virtual hosts definitions.
     */
    protected Host[] hosts = new Host[0];


    /**
     * Default host name.
     */
    protected String defaultHostName = null;

    /**
     * Context associated with this wrapper, used for wrapper mapping.
     */
    protected Context context = new Context();


    // START GlassFish 1024
    private Map<String, String> defaultContextPathsMap = new HashMap<String, String>();


    /**
     * The port this Mapper is used
     */
    private int port = 0;


    // --------------------------------------------------------- Public Methods


    /**
     * Allow replacement of already added {@link Host}, {@link Context} and
     * {@link Wrapper} when invoking {@link #addHost}, {@link #addContext} etc.
     * Default is <tt>false</tt>
     */
    public static void setAllowReplacement(boolean ar){
        allowReplacement = ar;
    }


    /**
     * <tt>true</tt> if replacement of already added [@link Host}, {@link Context} and
     * {@link Wrapper} when invoking {@link #addHost}, {@link #addContext} etc.
     * Default is <tt>false</tt>
     */
    public static boolean allowReplacement(){
        return allowReplacement;
    }


    /**
     * The Port this instance is used for mapping.
     */
    public void setPort(int port){
         this.port = port;
    }


    /**
     * @return the port this instance is used for mapping.
     */
    public int getPort(){
        return port;
    }


    /**
     * @return Default host name
     */
    public String getDefaultHostName() {
        return defaultHostName;
    }


    /**
     * Set default host.
     *
     * @param defaultHostName Default host name
     */
    public void setDefaultHostName(String defaultHostName) {
        this.defaultHostName = defaultHostName;
    }

    /**
     * Add a new host to the mapper.
     *
     * @param name Virtual host name
     * @param host Host object
     */
    public synchronized void addHost(String name, String[] aliases, Object host) {
        Host[] newHosts = new Host[hosts.length + 1];
        Host newHost = new Host();
        ContextList contextList = new ContextList();
        // START GlassFish 1024
        Context[] defaultContexts = new Context[1];
        String[] defaultContextPaths = new String[1];
        // END GlassFish 1024
        newHost.name = name;
        newHost.contextList = contextList;
        newHost.object = host;
        // START GlassFish 1024
        newHost.defaultContexts = defaultContexts;
        newHost.defaultContextPaths = defaultContextPaths;
        // END GlassFish 1024
        Host oldElem = (Host) insertMapIgnoreCase(hosts, newHosts, newHost);
        if (oldElem == null) {
            hosts = newHosts;
        } else if (allowReplacement) {
            oldElem.object = host;
            contextList = oldElem.contextList;
        }
        for (String alias : aliases) {
            newHosts = new Host[hosts.length + 1];
            newHost = new Host();
            newHost.name = alias;
            newHost.contextList = contextList;
            // START GlassFish 1024
            newHost.defaultContexts = defaultContexts;
            newHost.defaultContextPaths = defaultContextPaths;
            // END GlassFish 1024
            newHost.object = host;
            if (insertMapIgnoreCase(hosts, newHosts, newHost) == null) {
                hosts = newHosts;
            }
        }

        String defaultContextPath = defaultContextPathsMap.get(name);
        if (defaultContextPath != null) {
            newHost.defaultContextPaths[0] = defaultContextPath;
        }
    }


    /**
     * Remove a host from the mapper.
     *
     * @param name Virtual host name
     */
    public synchronized void removeHost(String name) {
        // Find and remove the old host
        int pos = findIgnoreCase(hosts, name);
        if (pos < 0) {
            return;
        }
        Object host = hosts[pos].object;
        Host[] newHosts = new Host[hosts.length - 1];
        if (removeMapIgnoreCase(hosts, newHosts, name)) {
            hosts = newHosts;
        }
        // Remove all aliases (they will map to the same host object)
        for (Host newHost : newHosts) {
            if (newHost.object == host) {
                Host[] newHosts2 = new Host[hosts.length - 1];
                if (removeMapIgnoreCase(hosts, newHosts2, newHost.name)) {
                    hosts = newHosts2;
                }
            }
        }

        // START GlassFish 1024
        defaultContextPathsMap.remove(name);
        // END GlassFish 1024
    }

    public String[] getHosts() {
        String hostN[] = new String[hosts.length];
        for( int i = 0; i < hosts.length; i++ ) {
            hostN[i] = hosts[i].name;
        }
        return hostN;
    }


    /**
     * Set context, used for wrapper mapping (request dispatcher).
     *
     * @param welcomeResources Welcome files defined for this context
     * @param resources Static resources of the context
     */
    public void setContext(String path, String[] welcomeResources,
                           NamingContext resources) {
        context.name = path;
        context.welcomeResources = welcomeResources;
        context.resources = resources;
    }


    /**
     * Add a new Context to an existing Host.
     *
     * @param hostName Virtual host name this context belongs to
     * @param path Context path
     * @param context Context object
     * @param welcomeResources Welcome files defined for this context
     * @param resources Static resources of the context
     */
    public void addContext
            (String hostName, String path, Object context,
            String[] welcomeResources, NamingContext resources) {
        addContext(hostName, path, context, welcomeResources, resources, null);
    }


    /**
     * Add a new Context to an existing Host.
     *
     * @param hostName Virtual host name this context belongs to
     * @param path Context path
     * @param context Context object
     * @param welcomeResources Welcome files defined for this context
     * @param resources Static resources of the context
     * @param alternateDocBases the alternate docbases of the context
     */
    public void addContext
            (String hostName, String path, Object context,
            String[] welcomeResources, NamingContext resources,
            List<AlternateDocBase> alternateDocBases) {

        Host[] newHosts = hosts;
        int pos = findIgnoreCase(newHosts, hostName);
        if( pos <0 ) {
            addHost(hostName, new String[0], "");
            newHosts = hosts;
            pos = findIgnoreCase(newHosts, hostName);
        }
        if (pos < 0) {
            logger.log(Level.FINE, "No host found: {0} for Mapper listening on port: {1}",
                    new Object[]{hostName, port});
            return;
        }
        Host host = newHosts[pos];
        if (host.name.equalsIgnoreCase(hostName)) {
            int slashCount = slashCount(path);
            synchronized (host) {
                Context[] contexts = host.contextList.contexts;
                // Update nesting
                if (slashCount > host.contextList.nesting) {
                    host.contextList.nesting = slashCount;
                }
                Context[] newContexts = new Context[contexts.length + 1];
                Context newContext = new Context();
                newContext.name = path;
                newContext.object = context;
                newContext.welcomeResources = welcomeResources;
                newContext.resources = resources;
                newContext.alternateDocBases = alternateDocBases;
                Context oldElem = (Context) insertMap(contexts, newContexts, newContext);
                if (oldElem == null) {
                    host.contextList.contexts = newContexts;
                    // START GlassFish 1024
                    if (path.equals(host.defaultContextPaths[0])) {
                        host.defaultContexts[0] = newContext;
                    }
                    // END GlassFish 1024
                } else if (allowReplacement) {
                    oldElem.object = context;
                    oldElem.welcomeResources = welcomeResources;
                    oldElem.resources = resources;
                }
            }
        }
    }


    /**
     * Remove a context from an existing host.
     *
     * @param hostName Virtual host name this context belongs to
     * @param path Context path
     */
    public void removeContext(String hostName, String path) {
        Host[] newHosts = hosts;
        int pos = findIgnoreCase(newHosts, hostName);
        if (pos < 0) {
            return;
        }
        Host host = newHosts[pos];
        if (host.name.equalsIgnoreCase(hostName)) {
            synchronized (host) {
                Context[] contexts = host.contextList.contexts;
                if( contexts.length == 0 ){
                    return;
                }
                Context[] newContexts = new Context[contexts.length - 1];
                if (removeMap(contexts, newContexts, path)) {
                    host.contextList.contexts = newContexts;
                    // Recalculate nesting
                    host.contextList.nesting = 0;
                    for (Context newContext : newContexts) {
                        int slashCount = slashCount(newContext.name);
                        if (slashCount > host.contextList.nesting) {
                            host.contextList.nesting = slashCount;
                        }
                    }
                }
            }
        }
    }


    /**
     * Return all contexts, in //HOST/PATH form
     *
     * @return The context names
     */
    public String[] getContextNames() {
        List<String> list=new ArrayList<String>();
        for (Host host : hosts) {
            for (int j = 0; j < host.contextList.contexts.length; j++) {
                String cname = host.contextList.contexts[j].name;
                list.add("//" + host.name + (cname.startsWith("/") ? cname : "/"));
            }
        }
        String res[] = new String[list.size()];
        return list.toArray(res);
    }


    /**
     * Add a new Wrapper to an existing Context.
     *
     * @param hostName Virtual host name this wrapper belongs to
     * @param contextPath Context path this wrapper belongs to
     * @param path Wrapper mapping
     * @param wrapper Wrapper object
     */
    public void addWrapper(String hostName, String contextPath, String path,
                           Object wrapper) {
        addWrapper(hostName, contextPath, path, wrapper, false);
    }


    /**
     * Add a new Wrapper to an existing Context.
     *
     * @param hostName Virtual host name this wrapper belongs to
     * @param contextPath Context path this wrapper belongs to
     * @param path Wrapper mapping
     * @param wrapper Wrapper object
     * @param jspWildCard
     */
    public void addWrapper(String hostName, String contextPath, String path,
                           Object wrapper, boolean jspWildCard) {
        addWrapper(hostName, contextPath, path, wrapper, jspWildCard, null, false);
    }

    /**
     * Add a new Wrapper to an existing Context.
     *
     * @param hostName Virtual host name this wrapper belongs to
     * @param contextPath Context path this wrapper belongs to
     * @param path Wrapper mapping
     * @param wrapper Wrapper object
     * @param jspWildCard
     * @param servletName servlet name or null if unknown
     */
    public void addWrapper(String hostName, String contextPath, String path,
                           Object wrapper, boolean jspWildCard,
                           String servletName, boolean isEmptyPathSpecial) {
        Host[] newHosts = hosts;
        int pos = findIgnoreCase(newHosts, hostName);
        if (pos < 0) {
            return;
        }
        Host host = newHosts[pos];
        if (host.name.equalsIgnoreCase(hostName)) {
            Context[] contexts = host.contextList.contexts;
            int pos2 = find(contexts, contextPath);
            if( pos2<0 ) {
                 logger.log(Level.SEVERE, "No context found: {0}", contextPath);
                return;
            }
            Context ctx = contexts[pos2];
            if (ctx.name.equals(contextPath)) {
                addWrapper(ctx, path, wrapper, jspWildCard, servletName, isEmptyPathSpecial);
            }
        }
    }


    /**
     * Add a wrapper to the context associated with this wrapper.
     *
     * @param path Wrapper mapping
     * @param wrapper The Wrapper object
     */
    public void addWrapper(String path, Object wrapper) {
        addWrapper(context, path, wrapper);
    }


    public void addWrapper(String path, Object wrapper, boolean jspWildCard,
            boolean isEmptyPathSpecial) {
        addWrapper(context, path, wrapper, jspWildCard, isEmptyPathSpecial);
    }


    protected void addWrapper(Context context, String path, Object wrapper) {
        addWrapper(context, path, wrapper, false, false);
    }


    protected void addWrapper(Context context, String path, Object wrapper,
            boolean jspWildCard, boolean isEmptyPathSpecial) {
        addWrapper(context, path, wrapper, jspWildCard, null, isEmptyPathSpecial);
    }


    /**
     * Adds a wrapper to the given context.
     *
     * @param context The context to which to add the wrapper
     * @param path Wrapper mapping
     * @param wrapper The Wrapper object
     * @param jspWildCard true if the wrapper corresponds to the JspServlet
     * and the mapping path contains a wildcard; false otherwise
     * @param servletName then name of servletName or null if unknown
     */
    protected void addWrapper(Context context, String path, Object wrapper, boolean jspWildCard, String servletName,
        boolean isEmptyPathSpecial) {

        synchronized (context) {

            Wrapper newWrapper = new Wrapper();
            newWrapper.object = wrapper;
            newWrapper.jspWildCard = jspWildCard;
            newWrapper.servletName = servletName;
            if (path.endsWith("/*")) {
                // Wildcard wrapper
                newWrapper.name = path.substring(0, path.length() - 2);
                Wrapper[] oldWrappers = context.wildcardWrappers;
                Wrapper[] newWrappers =
                    new Wrapper[oldWrappers.length + 1];
                Wrapper oldElem = (Wrapper)
                        insertMap(oldWrappers, newWrappers, newWrapper);
                if (oldElem == null) {
                    context.wildcardWrappers = newWrappers;
                    int slashCount = slashCount(newWrapper.name);
                    if (slashCount > context.nesting) {
                        context.nesting = slashCount;
                    }
                } else if (allowReplacement) {
                    oldElem.object = wrapper;
                    oldElem.jspWildCard = jspWildCard;
                }
            } else if (path.startsWith("*.")) {
                // Extension wrapper
                newWrapper.name = path.substring(2);
                Wrapper[] oldWrappers = context.extensionWrappers;
                Wrapper[] newWrappers =
                    new Wrapper[oldWrappers.length + 1];
                Wrapper oldElem = (Wrapper)insertMap(oldWrappers, newWrappers, newWrapper);
                if (oldElem == null) {
                    context.extensionWrappers = newWrappers;
                } else if (allowReplacement){
                    oldElem.object = wrapper;
                    oldElem.jspWildCard = jspWildCard;
                }
            } else {
                boolean isSlashPath = "/".equals(path);
                if (isSlashPath) {
                    // Default wrapper
                    newWrapper.name = "";
                    context.defaultWrapper = newWrapper;
                }

                // Exact wrapper
                // also for "/" and non default servlet
                if (!isSlashPath || !DEFAULT_SERVLET.equals(servletName)) {
                    newWrapper.name = path;
                    if (isEmptyPathSpecial && path.length() == 0) {
                        context.emptyPathWrapper = newWrapper;
                    } else {
                        Wrapper[] oldWrappers = context.exactWrappers;
                        Wrapper[] newWrappers =
                            new Wrapper[oldWrappers.length + 1];
                        Wrapper oldElem = (Wrapper)insertMap(oldWrappers,
                            newWrappers, newWrapper);
                        if (oldElem == null) {
                            context.exactWrappers = newWrappers;
                        } else if (allowReplacement){
                            oldElem.object = wrapper;
                            oldElem.jspWildCard = jspWildCard;
                        }
                    }
                }
            }
        }
    }


    /**
     * Remove a wrapper from the context associated with this wrapper.
     *
     * @param path Wrapper mapping
     */
    public void removeWrapper(String path) {
        removeWrapper(context, path);
    }


    /**
     * Remove a wrapper from an existing context.
     *
     * @param hostName Virtual host name this wrapper belongs to
     * @param contextPath Context path this wrapper belongs to
     * @param path Wrapper mapping
     */
    public void removeWrapper
        (String hostName, String contextPath, String path) {
        Host[] newHosts = hosts;
        int pos = findIgnoreCase(newHosts, hostName);
        if (pos < 0) {
            return;
        }
        Host host = newHosts[pos];
        if (host.name.equalsIgnoreCase(hostName)) {
            Context[] contexts = host.contextList.contexts;
            int pos2 = find(contexts, contextPath);
            if (pos2 < 0) {
                return;
            }
            Context ctx = contexts[pos2];
            if (ctx.name.equals(contextPath)) {
                removeWrapper(ctx, path);
            }
        }
    }

    protected void removeWrapper(Context context, String path) {
        synchronized (context) {
            if (path.endsWith("/*")) {
                // Wildcard wrapper
                String name = path.substring(0, path.length() - 2);
                Wrapper[] oldWrappers = context.wildcardWrappers;
                Wrapper[] newWrappers =
                    new Wrapper[oldWrappers.length - 1];
                if (removeMap(oldWrappers, newWrappers, name)) {
                    // Recalculate nesting
                    context.nesting = 0;
                    for (Wrapper newWrapper : newWrappers) {
                        int slashCount = slashCount(newWrapper.name);
                        if (slashCount > context.nesting) {
                            context.nesting = slashCount;
                        }
                    }
                    context.wildcardWrappers = newWrappers;
                }
            } else if (path.startsWith("*.")) {
                // Extension wrapper
                String name = path.substring(2);
                Wrapper[] oldWrappers = context.extensionWrappers;
                Wrapper[] newWrappers =
                    new Wrapper[oldWrappers.length - 1];
                if (removeMap(oldWrappers, newWrappers, name)) {
                    context.extensionWrappers = newWrappers;
                }
            } else if ("/".equals(path)) {
                // Default wrapper
                context.defaultWrapper = null;
            } else {
                // Exact wrapper
                Wrapper[] oldWrappers = context.exactWrappers;
                Wrapper[] newWrappers =
                    new Wrapper[oldWrappers.length - 1];
                if (removeMap(oldWrappers, newWrappers, path)) {
                    context.exactWrappers = newWrappers;
                }
            }
        }
    }

    public String getWrappersString( String host, String context ) {
        String names[]=getWrapperNames(host, context);
        StringBuilder sb=new StringBuilder();
        for (String name : names) {
            sb.append(name).append(":");
        }
        return sb.toString();
    }

    public String[] getWrapperNames( String host, String context ) {
        List<String> list=new ArrayList<String>();
        if( host==null ) host="";
        if( context==null ) context="";
        for (Host host1 : hosts) {
            if (!host.equals(host1.name)) {
                continue;
            }
            for (int j = 0; j < host1.contextList.contexts.length; j++) {
                if (!context.equals(host1.contextList.contexts[j].name)) {
                    continue;
                }
                // found the context
                Context ctx = host1.contextList.contexts[j];
                list.add(ctx.defaultWrapper.path);
                for (int k = 0; k < ctx.exactWrappers.length; k++) {
                    list.add(ctx.exactWrappers[k].path);
                }
                for (int k = 0; k < ctx.wildcardWrappers.length; k++) {
                    list.add(ctx.wildcardWrappers[k].path + "*");
                }
                for (int k = 0; k < ctx.extensionWrappers.length; k++) {
                    list.add("*." + ctx.extensionWrappers[k].path);
                }
            }
        }
        String res[]=new String[list.size()];
        return list.toArray(res);
    }


    // START GlassFish 1024
    /**
     * Configures the given virtual server with the given default context path.
     *
     * The given default path corresponds to the context path of one of the
     * web contexts deployed on the virtual server that has been designated as
     * the virtual server's new default-web-module.
     *
     * @throws Exception if there is no web context deployed on the given
     * virtual server that matches the given default context path
     */
    public void setDefaultContextPath(String hostName,
                                      String defaultContextPath)
            throws Exception {

        if (defaultContextPath != null) {
            defaultContextPathsMap.put(hostName, defaultContextPath);
        }

        int pos = findIgnoreCase(hosts, hostName);
        if (pos < 0) {
            return;
        }

        hosts[pos].defaultContextPaths[0] = defaultContextPath;

        if (defaultContextPath != null) {
            addDefaultContext(hosts[pos], defaultContextPath);
        } else {
            hosts[pos].defaultContexts[0] = null;
            defaultContextPathsMap.remove(hostName);
        }
    }


    /**
     * Configures the given virtual server with the given default context path.
     *
     * The given default path corresponds to the context path of one of the
     * web contexts deployed on the virtual server that has been designated as
     * the virtual server's new default-web-module.
     *
     * @throws Exception if there is no web context deployed on the given
     * virtual server that matches the given default context path
     */
    private void addDefaultContext(Host host, String defaultContextPath)
            throws Exception {

        boolean defaultContextFound = false;

        Context[] contexts = host.contextList.contexts;

        if (contexts != null) {
            for (Context context1 : contexts) {
                if (context1.name.equals(defaultContextPath)) {
                    host.defaultContexts[0] = context1;
                    defaultContextFound = true;
                    break;
                }
            }
        }

        if (!defaultContextFound) {
            throw new Exception("No context matching " + defaultContextPath
                                + " deployed on virtual server "
                                + host.name);
        }
    }
    // END GlassFish 1024


    /**
     * Maps the decodedURI to the corresponding HttpHandler, considering that URI
     * may have a semicolon with extra data followed, which shouldn't be a part
     * of mapping process.
     *
     * @param requestPacket the request packet containing the host information
     *                      to be used by the mapping process.
     * @param decodedURI   decoded URI
     * @param mappingData  {@link MappingData} based on the URI.
     * @param semicolonPos semicolon position. Might be <tt>0</tt> if position
     *                     wasn't resolved yet (so it will be resolved in the method),
     *                     or <tt>-1</tt> if there is no semicolon in the URI.
     * @throws Exception if an error occurs mapping the request
     */
    public void mapUriWithSemicolon(final HttpRequestPacket requestPacket,
                                    final DataChunk decodedURI,
                                    final MappingData mappingData,
                                    int semicolonPos) throws Exception {
        final CharChunk charChunk = decodedURI.getCharChunk();
        final int oldEnd = charChunk.getEnd();

        if (semicolonPos == 0) {
            semicolonPos = decodedURI.indexOf(';', 0);
        }

        DataChunk localDecodedURI = decodedURI;
        if (semicolonPos >= 0) {
            charChunk.setEnd(semicolonPos);
            // duplicate the URI path, because Mapper may corrupt the attributes,
            // which follow the path
            localDecodedURI = mappingData.tmpMapperDC;
            localDecodedURI.duplicate(decodedURI);
        }


        map(requestPacket, localDecodedURI, mappingData);
        charChunk.setEnd(oldEnd);
    }


    /**
     * Maps the decodedURI to the corresponding HttpHandler, considering that URI
     * may have a semicolon with extra data followed, which shouldn't be a part
     * of mapping process.
     *
     * @param serverName the server name as described by the Host header.
     * @param decodedURI decoded URI
     * @param mappingData {@link MappingData} based on the URI.
     * @param semicolonPos semicolon position. Might be <tt>0</tt> if position
     *  wasn't resolved yet (so it will be resolved in the method),
     *  or <tt>-1</tt> if there is no semicolon in the URI.
     *
     * @throws Exception if an error occurs mapping the request
     */
    public void mapUriWithSemicolon(final DataChunk serverName,
            final DataChunk decodedURI,
            final MappingData mappingData,
            int semicolonPos)
            throws Exception {

        final CharChunk charChunk = decodedURI.getCharChunk();
        final int oldEnd = charChunk.getEnd();

        if (semicolonPos == 0) {
            semicolonPos = decodedURI.indexOf(';', 0);
        }

        DataChunk localDecodedURI = decodedURI;
        if (semicolonPos >= 0) {
            charChunk.setEnd(semicolonPos);
            // duplicate the URI path, because Mapper may corrupt the attributes,
            // which follow the path
            localDecodedURI = mappingData.tmpMapperDC;
            localDecodedURI.duplicate(decodedURI);
        }


        map(serverName, localDecodedURI, mappingData);
        charChunk.setEnd(oldEnd);
    }

    /**
     * Map the specified host name and URI, mutating the given mapping data.
     *
     * @param host Virtual host name
     * @param uri URI
     * @param mappingData This structure will contain the result of the mapping
     *                    operation
     */
    public void map(final DataChunk host, final DataChunk uri,
                    final MappingData mappingData) throws Exception {

        if (host.isNull()) {
            host.getCharChunk().append(defaultHostName);
        } else if (host.getLength() == 0) {
            throw new Exception("Host is not set");
        }
        
        host.toChars(Constants.DEFAULT_HTTP_CHARSET);
        uri.toChars(Charsets.UTF8_CHARSET);
        
        internalMap(host.getCharChunk(), uri.getCharChunk(), mappingData);

    }


    /**
     * Map the specified host name and URI, mutating the given mapping data.
     *
     * @param requestPacket the http request containing host name information
     *                      used by the mapping process.
     * @param uri         URI
     * @param mappingData This structure will contain the result of the mapping
     *                    operation
     */
    public void map(final HttpRequestPacket requestPacket, final DataChunk uri,
                    final MappingData mappingData) throws Exception {

        final CharChunk hostCC;
        if (hosts.length > 1) {
            final DataChunk host = requestPacket.serverName();
            if (host.isNull()) {
                host.getCharChunk().append(defaultHostName);
            } else if (host.getLength() == 0) {
                throw new Exception("Host is not set");
            } else {
                host.toChars(Constants.DEFAULT_HTTP_CHARSET);
            }
            
            hostCC = host.getCharChunk();
        } else {
            hostCC = null;
        }
        
        uri.toChars(Charsets.UTF8_CHARSET);
        internalMap(hostCC,
                    uri.getCharChunk(),
                    mappingData);

    }


    /**
     * Map the specified URI relative to the context,
     * mutating the given mapping data.
     *
     * @param uri URI
     * @param mappingData This structure will contain the result of the mapping
     *                    operation
     */
    public void map(MessageBytes uri, MappingData mappingData)
        throws Exception {

        uri.toChars();
        CharChunk uricc = uri.getCharChunk();
        uricc.setLimit(-1);
        internalMapWrapper(context, uricc, mappingData);

    }


    // -------------------------------------------------------- Private Methods


    /**
     * Map the specified URI.
     */
    private void internalMap(CharChunk host, CharChunk uri,
                                   MappingData mappingData)
        throws Exception {

        uri.setLimit(-1);

        Context[] contexts = null;
        Context ctx = null;
        int nesting = 0;

        int hostPos = -1;

        // Virtual host mapping
        if (mappingData.host == null) {
            Host[] newHosts = hosts;
            int pos = host == null || host.isNull() ?
                    -1 :
                    findIgnoreCase(newHosts, host);
            if (pos != -1 && host.equalsIgnoreCase(newHosts[pos].name)) {
                mappingData.host = newHosts[pos].object;
                hostPos = pos;
                contexts = newHosts[pos].contextList.contexts;
                nesting = newHosts[pos].contextList.nesting;
            } else {
                if (defaultHostName == null) {
                    return;
                }
                pos = findIgnoreCase(newHosts, defaultHostName);
                if (pos != -1 && defaultHostName.equalsIgnoreCase(newHosts[pos].name)) {
                    mappingData.host = newHosts[pos].object;
                    hostPos = pos;
                    contexts = newHosts[pos].contextList.contexts;
                    nesting = newHosts[pos].contextList.nesting;
                } else {
                    return;
                }
            }
        }

        // Context mapping
        if (mappingData.context == null) {

            // START GlassFish 1024
            boolean found = false;
            // END GlassFish 1024

            int pos = find(contexts, uri);
            if (pos == -1) {
                // START GlassFish 1024
                if (hosts[hostPos].defaultContexts[0] == null) {
                // END GlassFish 1024
                    return;
                // START GlassFish 1024
                }
                ctx = hosts[hostPos].defaultContexts[0];
                mappingData.context = ctx.object;
                mappingData.contextPath.setString(ctx.name);
                found = true;
                mappingData.isDefaultContext = true;
                // END GlassFish 1024
            }

            // START GlassFish 1024
            if (!found) {
            // END GlassFish 1024
                int lastSlash = -1;
                int uriEnd = uri.getEnd();
                int length;
                /* GlassFish 1024
                boolean found = false;
                */
                while (pos >= 0) {
                    if (uri.startsWith(contexts[pos].name)) {
                        length = contexts[pos].name.length();
                        if (uri.getLength() == length) {
                            found = true;
                            break;
                        } else if (uri.startsWithIgnoreCase("/", length)) {
                            found = true;
                            break;
                        }
                    }
                    if (lastSlash == -1) {
                        lastSlash = nthSlash(uri, nesting + 1);
                    } else {
                        lastSlash = lastSlash(uri);
                    }
                    uri.setEnd(lastSlash);
                    pos = find(contexts, uri);
                }
                uri.setEnd(uriEnd);

                if (!found) {
                    if ("".equals(contexts[0].name)) {
                        ctx = contexts[0];
                    // START GlassFish 1024
                    } else if (hosts[hostPos].defaultContexts[0] != null) {
                        ctx = hosts[hostPos].defaultContexts[0];
                        mappingData.isDefaultContext = true;
                    // END GlassFish 1024
                    }
                } else {
                    ctx = contexts[pos];
                }
                if (ctx != null) {
                    mappingData.context = ctx.object;
                    mappingData.contextPath.setString(ctx.name);
                }
            }
        }

        // Wrapper mapping
        if (ctx != null && mappingData.wrapper == null) {
            internalMapWrapper(ctx, uri, mappingData);
        }

    }


    /**
     * Wrapper mapping.
     */
    private void internalMapWrapper(Context context, CharChunk path,
                                          MappingData mappingData)
        throws Exception {

        int pathOffset = path.getStart();
        int pathEnd = path.getEnd();
        int servletPath;
        boolean noServletPath = false;

        // START GlassFish 1024
        if (mappingData.isDefaultContext) {
            servletPath = pathOffset;
        } else {
        // END GlassFish 1024
            int length = context.name.length();
            if (length != pathEnd - pathOffset) {
                servletPath = pathOffset + length;
            } else {
                noServletPath = true;
                path.append('/');
                pathOffset = path.getStart();
                pathEnd = path.getEnd();
                servletPath = pathOffset+length;
            }
        // START GlassFish 1024
        }
        // END GlassFish 1024

        path.setStart(servletPath);

        // Rule 0 -- Empty path match
        if (context.emptyPathWrapper != null) {
            if (path.equals(SLASH)) {
                mappingData.wrapper = context.emptyPathWrapper.object;
                mappingData.requestPath.setString("");
                mappingData.wrapperPath.setString("");
                mappingData.pathInfo.setString("/");
            }
        }

        // Rule 1 -- Exact Match
        Wrapper[] exactWrappers = context.exactWrappers;
        if (mappingData.wrapper == null) {
            internalMapExactWrapper(exactWrappers, path, mappingData);
        }

        // Rule 2 -- Prefix Match
        boolean checkJspWelcomeFiles = false;
        Wrapper[] wildcardWrappers = context.wildcardWrappers;
        if (mappingData.wrapper == null) {
            internalMapWildcardWrapper(wildcardWrappers, context.nesting,
                                       path, mappingData);
            if (mappingData.wrapper != null && mappingData.jspWildCard) {
                char[] buf = path.getBuffer();
                if (buf[pathEnd - 1] == '/') {
                    /*
                     * Path ending in '/' was mapped to JSP servlet based on
                     * wildcard match (e.g., as specified in url-pattern of a
                     * jsp-property-group.
                     * Force the context's welcome files, which are interpreted
                     * as JSP files (since they match the url-pattern), to be
                     * considered. See Bugzilla 27664.
                     */
                    mappingData.wrapper = null;
                    checkJspWelcomeFiles = true;
                } else {
                    // See Bugzilla 27704
                    mappingData.wrapperPath.setChars(buf, path.getStart(),
                                                     path.getEnd());
                    mappingData.pathInfo.recycle();
                }
            }
        }

        if (noServletPath) {
            boolean redirect = mappingData.wrapper == null;
            if (!redirect) {
                String wpath = mappingData.wrapperPath.toString();
                // correspond to url-pattern /*
                redirect = wpath != null && wpath.length() == 0;
            }

            if (redirect) {
                // The path is empty, redirect to "/"
                mappingData.redirectPath.setChars
                    (path.getBuffer(), pathOffset, pathEnd);
                path.setEnd(pathEnd - 1);
                return;
            }
        }

        // Rule 3 -- Extension Match
        Wrapper[] extensionWrappers = context.extensionWrappers;
        if (mappingData.wrapper == null && !checkJspWelcomeFiles) {
            internalMapExtensionWrapper(extensionWrappers, path, mappingData);
        }

        // Rule 4 -- Welcome resources processing for servlets
        if (mappingData.wrapper == null) {
            boolean checkWelcomeFiles = checkJspWelcomeFiles;
            if (!checkWelcomeFiles && pathEnd > 0) {
                char[] buf = path.getBuffer();
                checkWelcomeFiles = buf[pathEnd - 1] == '/';
            }

            if (checkWelcomeFiles) {
                // Rule 4a -- Static welcome resources
                for (int i = 0; i < context.welcomeResources.length
                         && mappingData.wrapper == null; i++) {
                    path.setStart(pathOffset);
                    path.setEnd(pathEnd);
                    path.append(context.welcomeResources[i], 0,
                                context.welcomeResources[i].length());
                    path.setStart(servletPath);

                    // Welcome resources processing for physical folder
                    if (mappingData.wrapper == null &&
                            context.resources != null) {
                        Object file = null;
                        String pathStr = path.toString();

                        if (context.alternateDocBases == null
                                || context.alternateDocBases.isEmpty()) {
                            try {
                                file = context.resources.lookup(pathStr);
                            } catch(NamingException nex) {
                                // Swallow not found, since this is normal
                            }
                        } else {
                            AlternateDocBase match =
                                AlternateDocBase.findMatch(pathStr,
                                    context.alternateDocBases);
                            if (match != null) {
                                try {
                                    file = match.getResources().lookup(pathStr);
                                } catch(NamingException nex) {
                                    // Swallow not found, since this is normal
                                }
                            } else {
                                // None of the url patterns for alternate
                                // docbases matched
                                try {
                                    file = context.resources.lookup(pathStr);
                                } catch(NamingException nex) {
                                    // Swallow not found, since this is normal
                                }
                            }
                        }

                        if (file != null && !(file instanceof DirContext) ) {
                            // Rule 4a1 -- exact match
                            internalMapExactWrapper(exactWrappers, path, mappingData);

                            // Rule 4a2 -- prefix match
                            if (mappingData.wrapper == null) {
                                internalMapWildcardWrapper(wildcardWrappers,
                                        context.nesting, path, mappingData);
                            }

                            // Rule 4a3 -- extension match
                            if (mappingData.wrapper == null) {
                                internalMapExtensionWrapper(extensionWrappers,
                                        path, mappingData);
                            }

                            // Rule 4a4 use default
                            if (mappingData.wrapper == null
                                && context.defaultWrapper != null) {
                                mappingData.wrapper =
                                    context.defaultWrapper.object;
                                mappingData.servletName =
                                    context.defaultWrapper.servletName;
                                mappingData.requestPath.setChars
                                    (path.getBuffer(), path.getStart(),
                                     path.getEnd());
                                mappingData.wrapperPath.setChars
                                    (path.getBuffer(), path.getStart(),
                                     path.getEnd());
                                mappingData.requestPath.setString(pathStr);
                                mappingData.wrapperPath.setString(pathStr);
                            }
                        }
                    }
                }

                // Rule 4b -- Non-static welcome resources
                if (mappingData.wrapper == null) {
                    for (int i = 0; i < context.welcomeResources.length
                             && mappingData.wrapper == null; i++) {
                        path.setStart(pathOffset);
                        path.setEnd(pathEnd);
                        path.append(context.welcomeResources[i], 0,
                                    context.welcomeResources[i].length());
                        path.setStart(servletPath);

                        // Rule 4b1 -- Welcome resources processing for exact match
                        internalMapExactWrapper(exactWrappers, path, mappingData);

                        // Rule 4b2 -- Welcome resources processing for prefix match
                        if (mappingData.wrapper == null) {
                            internalMapWildcardWrapper
                                (wildcardWrappers, context.nesting,
                                 path, mappingData);
                        }

                        // Rule 4b3 -- Welcome resources processing for extension match
                        if (mappingData.wrapper == null) {
                            internalMapExtensionWrapper(
                                    extensionWrappers, path, mappingData);
                        }

                        // cannot use jsp as the file does not exist
                        if (mappingData.wrapper != null &&
                                JSP_SERVLET.equals(mappingData.servletName)) {
                            mappingData.wrapper = null;
                        }
                    }
                }

                path.setStart(servletPath);
                path.setEnd(pathEnd);
            }
        }


        // Rule 7 -- Default servlet
        if (mappingData.wrapper == null && !checkJspWelcomeFiles) {
            if (context.defaultWrapper != null) {
                mappingData.wrapper = context.defaultWrapper.object;
                mappingData.servletName = context.defaultWrapper.servletName;
                mappingData.requestPath.setChars
                    (path.getBuffer(), path.getStart(), path.getEnd());
                mappingData.wrapperPath.setChars
                    (path.getBuffer(), path.getStart(), path.getEnd());
            }
            // Redirection to a folder
            char[] buf = path.getBuffer();
            if (context.resources != null && pathEnd > 0 && buf[pathEnd -1 ] != '/') {
                Object file = null;
                String pathStr = path.toString();

                if (context.alternateDocBases == null
                        || context.alternateDocBases.isEmpty()) {
                    try {
                        file = context.resources.lookup(pathStr);
                    } catch(NamingException nex) {
                        // Swallow, since someone else handles the 404
                    }
                } else {
                    AlternateDocBase match =
                        AlternateDocBase.findMatch(pathStr,
                            context.alternateDocBases);
                    if (match != null) {
                        try {
                            file = match.getResources().lookup(pathStr);
                        } catch(NamingException nex) {
                            // Swallow, since someone else handles the 404
                        }
                    } else {
                        // None of the url patterns for alternate
                        // docbases matched
                        try {
                            file = context.resources.lookup(pathStr);
                        } catch(NamingException nex) {
                            // Swallow, since someone else handles the 404
                        }
                    }
                }

                if (file != null && file instanceof DirContext) {
                    // Note: this mutates the path: do not do any processing
                    // after this (since we set the redirectPath, there
                    // shouldn't be any)
                    path.setStart(pathOffset);
                    path.append('/');
                    mappingData.redirectPath.setChars
                        (path.getBuffer(), path.getStart(), path.getEnd());
                } else {
                    mappingData.requestPath.setString(pathStr);
                    mappingData.wrapperPath.setString(pathStr);
                }
            }
        }

        path.setStart(pathOffset);
        path.setEnd(pathEnd);
    }


    /**
     * Exact mapping.
     */
    private void internalMapExactWrapper
        (Wrapper[] wrappers, CharChunk path, MappingData mappingData) {
        int pos = find(wrappers, path);
        if (pos != -1 && path.equals(wrappers[pos].name)) {
            mappingData.requestPath.setString(wrappers[pos].name);
            mappingData.wrapperPath.setString(wrappers[pos].name);
            mappingData.wrapper = wrappers[pos].object;
            mappingData.servletName = wrappers[pos].servletName;
        }
    }


    /**
     * Wildcard mapping.
     */
    private void internalMapWildcardWrapper
        (Wrapper[] wrappers, int nesting, CharChunk path,
         MappingData mappingData) {

        int pathEnd = path.getEnd();

        int lastSlash = -1;
        int length = -1;
        int pos = find(wrappers, path);
        if (pos != -1) {
            boolean found = false;
            while (pos >= 0) {
                if (path.startsWith(wrappers[pos].name)) {
                    length = wrappers[pos].name.length();
                    if (path.getLength() == length) {
                        found = true;
                        break;
                    } else if (path.startsWithIgnoreCase("/", length)) {
                        found = true;
                        break;
                    }
                }
                if (lastSlash == -1) {
                    lastSlash = nthSlash(path, nesting + 1);
                } else {
                    lastSlash = lastSlash(path);
                }
                path.setEnd(lastSlash);
                pos = find(wrappers, path);
            }
            path.setEnd(pathEnd);
            if (found) {
                mappingData.wrapperPath.setString(wrappers[pos].name);
                if (path.getLength() > length) {
                    mappingData.pathInfo.setChars
                        (path.getBuffer(),
                         path.getStart() + length,
                         path.getEnd());
                }
                mappingData.requestPath.setChars
                    (path.getBuffer(), path.getStart(), path.getEnd());
                mappingData.wrapper = wrappers[pos].object;
                mappingData.servletName = wrappers[pos].servletName;
                mappingData.jspWildCard = wrappers[pos].jspWildCard;
            }
        }
    }


    /**
     * Extension mappings.
     */
    private void internalMapExtensionWrapper
        (Wrapper[] wrappers, CharChunk path, MappingData mappingData) {
        char[] buf = path.getBuffer();
        int pathEnd = path.getEnd();
        int servletPath = path.getStart();
        int slash = -1;
        for (int i = pathEnd - 1; i >= servletPath; i--) {
            if (buf[i] == '/') {
                slash = i;
                break;
            }
        }
        if (slash >= 0) {
            int period = -1;
            for (int i = pathEnd - 1; i > slash; i--) {
                if (buf[i] == '.') {
                    period = i;
                    break;
                }
            }
            if (period >= 0) {
                path.setStart(period + 1);
                path.setEnd(pathEnd);
                int pos = find(wrappers, path);
                if (pos != -1
                    && path.equals(wrappers[pos].name)) {
                    mappingData.wrapperPath.setChars
                        (buf, servletPath, pathEnd);
                    mappingData.requestPath.setChars
                        (buf, servletPath, pathEnd);
                    mappingData.wrapper = wrappers[pos].object;
                    mappingData.servletName = wrappers[pos].servletName;
                }
                path.setStart(servletPath);
                path.setEnd(pathEnd);
            }
        }
    }


    /**
     * Find a map elemnt given its name in a sorted array of map elements.
     * This will return the index for the closest inferior or equal item in the
     * given array.
     */
    private static int find(MapElement[] map, CharChunk name) {
        return find(map, name, name.getStart(), name.getEnd());
    }


    /**
     * Find a map elemnt given its name in a sorted array of map elements.
     * This will return the index for the closest inferior or equal item in the
     * given array.
     */
    private static int find(MapElement[] map, CharChunk name,
                                  int start, int end) {

        int a = 0;
        int b = map.length - 1;

        // Special cases: -1 and 0
        if (b == -1) {
            return -1;
        }

        if (compare(name, start, end, map[0].name) < 0 ) {
            return -1;
        }
        if (b == 0) {
            return 0;
        }

        int i;
        while (true) {
            i = (b + a) >>> 1;
            int result = compare(name, start, end, map[i].name);
            if (result == 1) {
                a = i;
            } else if (result == 0) {
                return i;
            } else {
                b = i;
            }
            if (b - a == 1) {
                int result2 = compare(name, start, end, map[b].name);
                if (result2 < 0) {
                    return a;
                } else {
                    return b;
                }
            }
        }
    }


    /**
     * Find a map element given its name in a sorted array of map elements.
     * This will return the index for the closest inferior or equal item in the
     * given array.
     */
//    private static int findIgnoreCase(MapElement[] map, String name) {
//        CharChunk cc = new CharChunk();
//        char[] chars = name.toCharArray();
//        cc.setChars(chars, 0, chars.length);
//        return findIgnoreCase(map, cc);
//    }


    /**
     * Find a map element given its name in a sorted array of map elements.
     * This will return the index for the closest inferior or equal item in the
     * given array.
     */
     private static int findIgnoreCase(MapElement[] map, CharChunk name) {
         return findIgnoreCase(map, name, name.getStart(), name.getEnd());
     }


    /**
     * Find a map elemnt given its name in a sorted array of map elements.
     * This will return the index for the closest inferior or equal item in the
     * given array.
     */
    private static int findIgnoreCase(MapElement[] map, CharChunk name,
                                  int start, int end) {

        int a = 0;
        int b = map.length - 1;

        // Special cases: -1 and 0
        if (b == -1) {
            return -1;
        }
        if (compareIgnoreCase(name, start, end, map[0].name) < 0 ) {
            return -1;
        }
        if (b == 0) {
            return 0;
        }

        int i;
        while (true) {
            i = (b + a) >>> 1;
            int result = compareIgnoreCase(name, start, end, map[i].name);
            if (result == 1) {
                a = i;
            } else if (result == 0) {
                return i;
            } else {
                b = i;
            }
            if (b - a == 1) {
                int result2 = compareIgnoreCase(name, start, end, map[b].name);
                if (result2 < 0) {
                    return a;
                } else {
                    return b;
                }
            }
        }

    }

    /**
     * Find a map elemnt given its name in a sorted array of map elements.
     * This will return the index for the closest inferior or equal item in the
     * given array.
     */
    private static int findIgnoreCase(final MapElement[] map, final String name) {

        int a = 0;
        int b = map.length - 1;

        // Special cases: -1 and 0
        if (b == -1) {
            return -1;
        }
        if (compareIgnoreCase(name, map[0].name) < 0 ) {
            return -1;
        }
        if (b == 0) {
            return 0;
        }

        int i;
        while (true) {
            i = (b + a) >>> 1;
            int result = compareIgnoreCase(name, map[i].name);
            if (result == 1) {
                a = i;
            } else if (result == 0) {
                return i;
            } else {
                b = i;
            }
            if (b - a == 1) {
                int result2 = compareIgnoreCase(name, map[b].name);
                if (result2 < 0) {
                    return a;
                } else {
                    return b;
                }
            }
        }

    }

    /**
     * Find a map element given its name in a sorted array of map elements.
     * This will return the index for the closest inferior or equal item in the
     * given array.
     */
    private static int find(final MapElement[] map, final String name) {

        int a = 0;
        int b = map.length - 1;

        // Special cases: -1 and 0
        if (b == -1) {
            return -1;
        }

        if (name.compareTo(map[0].name) < 0) {
            return -1;
        }
        if (b == 0) {
            return 0;
        }

        int i;
        while (true) {
            i = (b + a) >>> 1;
            int result = name.compareTo(map[i].name);
            if (result > 0) {
                a = i;
            } else if (result == 0) {
                return i;
            } else {
                b = i;
            }
            if (b - a == 1) {
                int result2 = name.compareTo(map[b].name);
                if (result2 < 0) {
                    return a;
                } else {
                    return b;
                }
            }
        }

    }


    /**
     * Compare given char chunk with String.
     * Return -1, 0 or +1 if inferior, equal, or superior to the String.
     */
    private static int compare(CharChunk name, int start, int end,
                                     String compareTo) {
        int result = 0;
        char[] c = name.getBuffer();
        int len = compareTo.length();
        if (end - start < len) {
            len = end - start;
        }
        for (int i = 0; i < len && result == 0; i++) {
            if (c[i + start] > compareTo.charAt(i)) {
                result = 1;
            } else if (c[i + start] < compareTo.charAt(i)) {
                result = -1;
            }
        }
        if (result == 0) {
            if (compareTo.length() > end - start) {
                result = -1;
            } else if (compareTo.length() < end - start) {
                result = 1;
            }
        }
        return result;
    }


    /**
     * Compare given char chunk with String ignoring case.
     * Return -1, 0 or +1 if inferior, equal, or superior to the String.
     */
    private static int compareIgnoreCase(CharChunk name, int start, int end,
                                     String compareTo) {
        int result = 0;
        char[] c = name.getBuffer();
        int len = compareTo.length();
        if (end - start < len) {
            len = end - start;
        }
        for (int i = 0; i < len && result == 0; i++) {
            if (Ascii.toLower(c[i + start]) > Ascii.toLower(compareTo.charAt(i))) {
                result = 1;
            } else if (Ascii.toLower(c[i + start]) < Ascii.toLower(compareTo.charAt(i))) {
                result = -1;
            }
        }
        if (result == 0) {
            if (compareTo.length() > end - start) {
                result = -1;
            } else if (compareTo.length() < end - start) {
                result = 1;
            }
        }
        return result;
    }

    /**
     * Compare given char chunk with String ignoring case.
     * Return -1, 0 or +1 if inferior, equal, or superior to the String.
     */
    private static int compareIgnoreCase(String name, String compareTo) {
        int result = 0;
        final int nameLen = name.length();
        final int compareToLen = compareTo.length();
        
        final int len = nameLen < compareToLen ? nameLen : compareToLen;

        for (int i = 0; i < len && result == 0; i++) {
            final int nameLower = Ascii.toLower(name.charAt(i));
            final int compareToLower = Ascii.toLower(compareTo.charAt(i));
            if (nameLower > compareToLower) {
                result = 1;
            } else if (nameLower < compareToLower) {
                result = -1;
            }
        }
        if (result == 0) {
            if (compareToLen > nameLen) {
                result = -1;
            } else if (compareToLen < nameLen) {
                result = 1;
            }
        }
        return result;
    }

    /**
     * Find the position of the last slash in the given char chunk.
     */
    private static int lastSlash(CharChunk name) {

        char[] c = name.getBuffer();
        int end = name.getEnd();
        int start = name.getStart();
        int pos = end;

        while (pos > start) {
            if (c[--pos] == '/') {
                break;
            }
        }

        return pos;

    }


    /**
     * Find the position of the nth slash, in the given char chunk.
     */
    private static int nthSlash(CharChunk name, int n) {

        char[] c = name.getBuffer();
        int end = name.getEnd();
        int pos = name.getStart();
        int count = 0;

        while (pos < end) {
            if (c[pos++] == '/' && ++count == n) {
                pos--;
                break;
            }
        }

        return pos;

    }


    /**
     * Return the slash count in a given string.
     */
    private static int slashCount(String name) {
        int pos = -1;
        int count = 0;
        while ((pos = name.indexOf('/', pos + 1)) != -1) {
            count++;
        }
        return count;
    }


    /**
     * Insert into the right place in a sorted MapElement array, preventing
     * duplicates.
     *
     * @return old MapElement with matching name (this is an indication that
     * newElement has not been inserted), or null if oldMap did not contain
     * any MapElement with matching name (this is an indication that
     * newElement has been inserted)
     */
    private static MapElement insertMap
        (MapElement[] oldMap, MapElement[] newMap, MapElement newElement) {
        int pos = find(oldMap, newElement.name);
        if (pos != -1 && newElement.name.equals(oldMap[pos].name)) {
            return oldMap[pos];
        }
        System.arraycopy(oldMap, 0, newMap, 0, pos + 1);
        newMap[pos + 1] = newElement;
        System.arraycopy
            (oldMap, pos + 1, newMap, pos + 2, oldMap.length - pos - 1);
        return null;
    }


    /**
     * Inserts the given MapElement at the appropriate place in the given,
     * lexicograhically sorted, MapElement array, avoiding duplicates.
     *
     * Name comparisons are performed in a case-insensitive manner.
     *
     * @return old MapElement with matching name (this is an indication that
     * newElement has not been inserted), or null if oldMap did not contain
     * any MapElement with matching name (this is an indication that
     * newElement has been inserted)
     */
    private static MapElement insertMapIgnoreCase
        (MapElement[] oldMap, MapElement[] newMap, MapElement newElement) {
        CharChunk cc = new CharChunk();
        char[] chars = newElement.name.toCharArray();
        cc.setChars(chars, 0, chars.length);
        int pos = findIgnoreCase(oldMap, cc);
        if (pos != -1 && newElement.name.equalsIgnoreCase(oldMap[pos].name)) {
            return oldMap[pos];
        }
        System.arraycopy(oldMap, 0, newMap, 0, pos + 1);
        newMap[pos + 1] = newElement;
        System.arraycopy
            (oldMap, pos + 1, newMap, pos + 2, oldMap.length - pos - 1);
        return null;
    }


    /**
     * Removes from a sorted MapElement array.
     */
    private static boolean removeMap
        (MapElement[] oldMap, MapElement[] newMap, String name) {
        int pos = find(oldMap, name);
        if (pos != -1 && name.equals(oldMap[pos].name)) {
            System.arraycopy(oldMap, 0, newMap, 0, pos);
            System.arraycopy(oldMap, pos + 1, newMap, pos,
                             oldMap.length - pos - 1);
            return true;
        }
        return false;
    }


    /**
     * Removes the MapElement with the given name from the given,
     * lexicographically sorted, MapElement array.
     *
     * Name comparisons are performed in a case-insensitive manner.
     */
    private static boolean removeMapIgnoreCase
        (MapElement[] oldMap, MapElement[] newMap, String name) {
        CharChunk cc = new CharChunk();
        char[] chars = name.toCharArray();
        cc.setChars(chars, 0, chars.length);
        int pos = findIgnoreCase(oldMap, cc);
        if (pos != -1 && name.equalsIgnoreCase(oldMap[pos].name)) {
            System.arraycopy(oldMap, 0, newMap, 0, pos);
            System.arraycopy(oldMap, pos + 1, newMap, pos,
                             oldMap.length - pos - 1);
            return true;
        }
        return false;
    }


// ------------------------------------------------- MapElement Inner Class


    protected static abstract class MapElement {

        public String name = null;
        public Object object = null;

    }


    // ------------------------------------------------------- Host Inner Class


    protected static final class Host
        extends MapElement {

        public ContextList contextList = null;
        // START GlassFish 1024
        public String[] defaultContextPaths = null;
        public Context[] defaultContexts = null;
        // END GlassFish 1024
    }


    // ------------------------------------------------ ContextList Inner Class


    protected static final class ContextList {

        public Context[] contexts = new Context[0];
        public int nesting = 0;

    }


    // ---------------------------------------------------- Context Inner Class


    protected static final class Context
        extends MapElement {

        public String path = null;
        public String[] welcomeResources = new String[0];
        public NamingContext resources = null;
        public List<AlternateDocBase> alternateDocBases = null;
        public Wrapper defaultWrapper = null;
        public Wrapper emptyPathWrapper = null;
        public Wrapper[] exactWrappers = new Wrapper[0];
        public Wrapper[] wildcardWrappers = new Wrapper[0];
        public Wrapper[] extensionWrappers = new Wrapper[0];
        public int nesting = 0;

    }


    // ---------------------------------------------------- Wrapper Inner Class


    protected static class Wrapper
        extends MapElement {

        public String path = null;
        public boolean jspWildCard = false;
        public String servletName = null;
    }


    // -------------------------------------------------------- Testing Methods

    // FIXME: Externalize this
    /*
    public static void main(String args[]) {

        try {

        Mapper mapper = new Mapper();
        System.out.println("Start");

        mapper.addHost("sjbjdvwsbvhrb", new String[0], "blah1");
        mapper.addHost("sjbjdvwsbvhr/", new String[0], "blah1");
        mapper.addHost("wekhfewuifweuibf", new String[0], "blah2");
        mapper.addHost("ylwrehirkuewh", new String[0], "blah3");
        mapper.addHost("iohgeoihro", new String[0], "blah4");
        mapper.addHost("fwehoihoihwfeo", new String[0], "blah5");
        mapper.addHost("owefojiwefoi", new String[0], "blah6");
        mapper.addHost("iowejoiejfoiew", new String[0], "blah7");
        mapper.addHost("iowejoiejfoiew", new String[0], "blah17");
        mapper.addHost("ohewoihfewoih", new String[0], "blah8");
        mapper.addHost("fewohfoweoih", new String[0], "blah9");
        mapper.addHost("ttthtiuhwoih", new String[0], "blah10");
        mapper.addHost("lkwefjwojweffewoih", new String[0], "blah11");
        mapper.addHost("zzzuyopjvewpovewjhfewoih", new String[0], "blah12");
        mapper.addHost("xxxxgqwiwoih", new String[0], "blah13");
        mapper.addHost("qwigqwiwoih", new String[0], "blah14");

        System.out.println("Map:");
        for (int i = 0; i < mapper.hosts.length; i++) {
            System.out.println(mapper.hosts[i].name);
        }

        mapper.setDefaultHostName("ylwrehirkuewh");

        String[] welcomes = new String[2];
        welcomes[0] = "boo/baba";
        welcomes[1] = "bobou";

        mapper.addContext("iowejoiejfoiew", "", "context0", new String[0], null);
        mapper.addContext("iowejoiejfoiew", "/foo", "context1", new String[0], null);
        mapper.addContext("iowejoiejfoiew", "/foo/bar", "context2", welcomes, null);
        mapper.addContext("iowejoiejfoiew", "/foo/bar/bla", "context3", new String[0], null);

        mapper.addWrapper("iowejoiejfoiew", "/foo/bar", "/fo/*", "wrapper0");
        mapper.addWrapper("iowejoiejfoiew", "/foo/bar", "/", "wrapper1");
        mapper.addWrapper("iowejoiejfoiew", "/foo/bar", "/blh", "wrapper2");
        mapper.addWrapper("iowejoiejfoiew", "/foo/bar", "*.jsp", "wrapper3");
        mapper.addWrapper("iowejoiejfoiew", "/foo/bar", "/blah/bou/*", "wrapper4");
        mapper.addWrapper("iowejoiejfoiew", "/foo/bar", "/blah/bobou/*", "wrapper5");
        mapper.addWrapper("iowejoiejfoiew", "/foo/bar", "*.htm", "wrapper6");

        MappingData mappingData = new MappingData();
        MessageBytes host = MessageBytes.newInstance();
        host.setString("iowejoiejfoiew");
        MessageBytes uri = MessageBytes.newInstance();
        uri.setString("/foo/bar/blah/bobou/foo");
        uri.toChars();
        uri.getCharChunk().setLimit(-1);

        mapper.map(host, uri, mappingData);
        System.out.println("MD Host:" + mappingData.host);
        System.out.println("MD Context:" + mappingData.context);
        System.out.println("MD Wrapper:" + mappingData.wrapper);

        System.out.println("contextPath:" + mappingData.contextPath);
        System.out.println("wrapperPath:" + mappingData.wrapperPath);
        System.out.println("pathInfo:" + mappingData.pathInfo);
        System.out.println("redirectPath:" + mappingData.redirectPath);

        mappingData.recycle();
        mapper.map(host, uri, mappingData);
        System.out.println("MD Host:" + mappingData.host);
        System.out.println("MD Context:" + mappingData.context);
        System.out.println("MD Wrapper:" + mappingData.wrapper);

        System.out.println("contextPath:" + mappingData.contextPath);
        System.out.println("wrapperPath:" + mappingData.wrapperPath);
        System.out.println("pathInfo:" + mappingData.pathInfo);
        System.out.println("redirectPath:" + mappingData.redirectPath);

        for (int i = 0; i < 1000000; i++) {
            mappingData.recycle();
            mapper.map(host, uri, mappingData);
        }

        long time = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            mappingData.recycle();
            mapper.map(host, uri, mappingData);
        }
        System.out.println("Elapsed:" + (System.currentTimeMillis() - time));

        System.out.println("MD Host:" + mappingData.host);
        System.out.println("MD Context:" + mappingData.context);
        System.out.println("MD Wrapper:" + mappingData.wrapper);

        System.out.println("contextPath:" + mappingData.contextPath);
        System.out.println("wrapperPath:" + mappingData.wrapperPath);
        System.out.println("requestPath:" + mappingData.requestPath);
        System.out.println("pathInfo:" + mappingData.pathInfo);
        System.out.println("redirectPath:" + mappingData.redirectPath);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    */
}
