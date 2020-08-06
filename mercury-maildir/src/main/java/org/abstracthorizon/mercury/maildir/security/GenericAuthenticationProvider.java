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
 * Generic authentication provider that always returns <code>true</code>
 *
 * @author Daniel Sendula
 */
public class GenericAuthenticationProvider implements AuthenticationProvider {

    /**
     * Constructor
     */
    public GenericAuthenticationProvider() {
    }

    /**
     * Empty implementation
     * @param loginContext not user
     */
    public void init(String loginContext) {
    }

    /**
     * Empty implementation that always returns <code>true</code>
     * @param host not used
     * @param port not used
     * @param user not used
     * @param pass not used
     * @return <code>true</code>
     */
    public boolean authenticate(String host, int port, String user, char[] pass) {
        return true;
    }
}
