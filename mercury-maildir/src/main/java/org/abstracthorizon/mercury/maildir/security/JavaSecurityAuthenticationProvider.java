/*
 * Copyright (c) 2005-2020 Creative Sphere Limited.
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

import java.io.IOException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * Authentication provider that uses Java security model. It tried to log in given login context.
 *
 * @author Daniel Sendula
 */
public class JavaSecurityAuthenticationProvider implements AuthenticationProvider {

    /** Login context name */
    protected String loginContext;

    /**
     * Constructor
     */
    public JavaSecurityAuthenticationProvider() {
    }

    /**
     * Stores login context for further use
     * @param loginContext login context name
     */
    public void init(String loginContext) {
        this.loginContext = loginContext;
    }

    /**
     * Authenticates user by logging to given login context
     * @param host not used
     * @param port not used
     * @param user user name to be used to log in login context
     * @param pass password to be used to log in login context
     * @return <code>true</code> if logging in login context was successful
     */
    public boolean authenticate(String host, int port, String user, char[] pass) {
        try {
            LoginContext lc = new LoginContext(loginContext, new Handler(user, pass));
            lc.login();
            lc.logout();
            return true;
        } catch (LoginException ignore) {
        }
        return false;
    }

    /**
     * Inner class that handles call backs
     */
    protected static class Handler implements CallbackHandler {
        protected String user;
        protected char[] pass;

        protected Handler(String user, char[] pass) {
            this.user = user;
            this.pass = pass;
        }

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    NameCallback nc = (NameCallback)callbacks[i];
                    nc.setName(user);
                } else if (callbacks[i] instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback)callbacks[i];
                    pc.setPassword(pass);
                }
             }
        }
    }
}
