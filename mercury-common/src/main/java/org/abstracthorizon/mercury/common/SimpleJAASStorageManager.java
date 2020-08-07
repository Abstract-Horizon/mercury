/*
 * Copyright (c) 2007-2020 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.common;

import java.io.IOException;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.abstracthorizon.mercury.common.exception.UserRejectedException;

/**
 * Simple JAAS storage manager. This storage manager uses login context
 * to authenticate user
 *
 * @author Daniel Sendula
 */
public class SimpleJAASStorageManager extends SimpleStorageManager {

    /** Login context name */
    protected String loginContext;

    /**
     * Constructor
     */
    public SimpleJAASStorageManager() {
    }

    /**
     * Sets login context string
     * @param loginContext login context string
     */
    public void setLoginContext(String loginContext) {
        this.loginContext = loginContext;
    }

    /**
     * Returns login context string
     * @return login context string
     */
    public String getLoginContext() {
        return loginContext;
    }

    /**
     * This method calls super find inbox method and then authenticates user against given password.
     * Mailbox is used for user's name.
     * @param mailbox mailbox
     * @param domain domain
     * @param password password
     * @throws UserRejectedException
     * @throws {@link MessagingException}
     */
    @Override
    public Folder findInbox(String mailbox, String domain, char[] password) throws UserRejectedException, MessagingException {
        Folder folder = super.findInbox(mailbox, domain, password);
        String loginContext = getLoginContext();
        if ((loginContext != null) && (loginContext.length() > 0)) {
            LoginContext lc = null;
            try {
                lc = new LoginContext(loginContext, new Handler(mailbox, password));
                lc.login();
            } catch (LoginException e) {
                throw new UserRejectedException("Access to mailbox " + mailbox + " is rejected");
            } finally {
                if (lc != null) {
                    try {
                        lc.logout();
                    } catch (LoginException ignore) {
                    }
                }
            }
        }

        return folder;
    }

    /**
     * Callback handler
     */
    protected static class Handler implements CallbackHandler {
        /** Username */
        protected String user;
        /** Password */
        protected char[] pass;

        /**
         * Constructor
         * @param user username
         * @param pass password
         */
        protected Handler(String user, char[] pass) {
            this.user = user;
            this.pass = pass;
        }

        /**
         * Handles callback
         * @param callbacks callbacks
         * @throws IOException
         * @throws UnsupportedCallbackException
         */
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
