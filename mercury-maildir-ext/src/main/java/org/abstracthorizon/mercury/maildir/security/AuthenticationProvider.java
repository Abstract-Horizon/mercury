/*
 * Copyright (c) 2005-2007 Creative Sphere Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   Creative Sphere - initial API and implementation
 *
 */
package org.abstracthorizon.mercury.maildir.security;

/**
 * Interface describing authentication provider.
 *
 * @author Daniel Sendula
 */
public interface AuthenticationProvider {

    /**
     * Initialises provider with given login context name
     * @param loginContext login context name
     */
    void init(String loginContext);

    /**
     * Tries to authenticate given user.
     * @param host host to be logged to
     * @param port port to be logged to
     * @param user username
     * @param pass password
     * @return <code>true</code> if authentication succeded.
     */
    boolean authenticate(String host, int port, String user, char[] pass);

}
