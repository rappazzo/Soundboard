/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import org.glassfish.grizzly.http.server.naming.DirContext;

public class AlternateDocBase {

    enum UrlPatternType { EXACT, WILDCARD, EXTENSION }

    private String pattern;
    private UrlPatternType patternType;
    private int patternSlashCount;

    private String docBase;
    private String basePath;
    private String patternSuffix;
    private String wildcardPath;
    private DirContext resources;
    private DirContext webappResources;


    /**
     * Sets the url pattern of this AlternateDocBase.
     *
     * @param urlPattern The url pattern of this AlternateDocBase
     */
    public void setUrlPattern(String urlPattern) {
        this.pattern = urlPattern;

        this.patternSlashCount = getSlashCount(urlPattern);
        if (urlPattern.endsWith("/*")) {
            this.patternType = UrlPatternType.WILDCARD;
            this.wildcardPath = urlPattern.substring(
                0, urlPattern.length()-1);
        } else if (urlPattern.startsWith("*.")) {
            this.patternType = UrlPatternType.EXTENSION;
            this.patternSuffix = urlPattern.substring(1);
        } else {
            this.patternType = UrlPatternType.EXACT;
        }
    }


    /**
     * Gets the url pattern of this AlternateDocBase.
     *
     * @return The url pattern of this AlternativeDocBase
     */
    public String getUrlPattern() {
        return pattern;
    }


    /**
     * Gets the url pattern type (exact, wildcard, extension) of this
     * AlternateDocBase.
     *
     * @return The url pattern type (exact, wildcard, extension) of this
     * AlternativeDocBase
     */
    public UrlPatternType getUrlPatternType() {
        return patternType;
    }


    /**
     * Gets the number of slashes in the url pattern of this AlternateDocBase.
     *
     * @return Number of slashes in the url pattern of this AlternateDocBase.
     */
    public int getUrlPatternSlashCount() {
        return patternSlashCount;
    }


    /**
     * Gets the extension suffix of the url pattern of this AlternateDocBase.
     *
     * @return The extension suffix of the url pattern of this
     * AlternateDocBase, or null if the url pattern is not of type 'extension'
     */
    public String getUrlPatternSuffix() {
        return this.patternSuffix;
    }


    /**
     * Gets the wildcard path of this AlternateDocBase (this is the path
     * specified by the wildcard pattern, minus the trailing '*').
     *
     * @return The wildcard path of this AlternateDocBase, or null if the
     * pattern associated with this AlternateDocBase is not a wildcard pattern
     */
    public String getUrlPatternWildcardPath() {
        return this.wildcardPath;
    }


    /**
     * Sets the (possibly relative) doc base path of this AlternateDocBase.
     *
     * @param docBase The (possibly relative) doc base path of this
     * AlternateDocBase
     */
    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }


    /**
     * Gets the (possibly relative) doc base path of this AlternateDocBase.
     *
     * @return The (possibly relative) doc base path of this AlternativeDocBase
     */
    public String getDocBase() {
        return docBase;
    }


    /**
     * Sets the absolute doc base path of this AlternateDocBase.
     *
     * @param basePath The absolute doc base path of this AlternateDocBase
     */
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }


    /**
     * Gets the absolute doc base path of this AlternateDocBase.
     *
     * @return The absolute doc base path of this AlternativeDocBase
     */
    public String getBasePath() {
        return basePath;
    }


    /**
     * Sets the proxied resources of this AlternateDocBase.
     *
     * @param resources The proxied resources of this AlternateDocBase
     */
    public void setResources(DirContext resources) {
        this.resources = resources;
    }


    /**
     * Gets the proxied resources of this AlternateDocBase.
     *
     * @return The proxied resources of this AlternateDocBase
     */
    public DirContext getResources() {
        return resources;
    }


    /**
     * Sets the non-proxied resources of this AlternateDocBase.
     *
     * @param webappResources The non-proxied resources of this
     * AlternateDocBase
     */
    public void setWebappResources(DirContext webappResources) {
        this.webappResources = webappResources;
    }


    /**
     * Gets the non-proxied resources of this AlternateDocBase.
     *
     * @return The non-proxied resources of this AlternateDocBase
     */
    public DirContext getWebappResources() {
        return webappResources;
    }


    /**
     * Attempts to match the given request path against one of the given
     * alternate doc bases.
     *
     * @return The alternate doc base whose url pattern matches the given path,
     * or null if no matching alternate doc base could be found
     */
    public static AlternateDocBase findMatch(
                    String path,
                    List<AlternateDocBase> alternateDocBases) {

        if (alternateDocBases == null) {
            return null;
        }

        AlternateDocBase match = null;
        AlternateDocBase wildcardMatch = null;
        AlternateDocBase extensionMatch = null;
        int maxSlashCountMatch = 0;

        int pathSlashCount = getSlashCount(path);

        for (int i=0; i<alternateDocBases.size(); i++) {

            AlternateDocBase alternateDocBase = alternateDocBases.get(i);
            String pattern = alternateDocBase.getUrlPattern();
            int patternSlashCount = alternateDocBase.getUrlPatternSlashCount();
            AlternateDocBase.UrlPatternType type =
                alternateDocBase.getUrlPatternType();
            String wildcardPath = alternateDocBase.getUrlPatternWildcardPath();

            if (type == UrlPatternType.EXACT && path.equals(pattern)) {
                // Exact match found
                match = alternateDocBase;
                break;
            } else if (type == UrlPatternType.WILDCARD
                    && pathSlashCount >= patternSlashCount
                    && patternSlashCount > maxSlashCountMatch
                    && path.startsWith(wildcardPath)) {
                // We've found a longer wildcard match
                wildcardMatch = alternateDocBase;
                maxSlashCountMatch = patternSlashCount;
            } else if (type == UrlPatternType.EXTENSION
                    && path.endsWith(alternateDocBase.getUrlPatternSuffix())) {
                extensionMatch = alternateDocBase;
            }
        }

        if (match == null) {
            if (wildcardMatch != null) {
                match = wildcardMatch;
            } else {
                match = extensionMatch;
            }
        }

        return match;
    }


    /**
     * Determines the number for forward slashes in the given string.
     *
     * @param s The string whose forward slashes to count
     *
     * @return The number of forward slashes
     */
    private static int getSlashCount(String s) {

        int count = 0;

        if (s != null) {
            int index = s.indexOf('/');
            while (index >= 0) {
                count++;
                index = s.indexOf('/', index+1);
            }
        }

        return count;
    }

}
