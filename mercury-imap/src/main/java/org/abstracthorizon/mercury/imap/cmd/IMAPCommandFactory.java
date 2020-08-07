/*
 * Copyright (c) 2004-2020 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.imap.cmd;

import org.abstracthorizon.danube.connection.ConnectionHandler;
import org.abstracthorizon.mercury.common.command.CommandException;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * IMAP Commands factory
 *
 * @author Daniel Sendula
 */
public class IMAPCommandFactory {

    /** Logger */
    protected static final Logger logger = LoggerFactory.getLogger(IMAPCommandFactory.class);

    /** Map of commands */
    protected static Map<String, Class<? extends IMAPCommand>> commands;

    {
        commands = new HashMap<String, Class<? extends IMAPCommand>>();
        add("APPEND", Append.class);
        add("AUTHENTICATE", Authenticate.class);
        add("CAPABILITY", Capability.class);
        add("CHECK", Check.class);
        add("CLOSE", Close.class);
        add("COPY", Copy.class);
        add("CREATE", Create.class);
        add("DELETE", Delete.class);
        add("EXAMINE", Examine.class);
        add("EXPUNGE", Expunge.class);
        add("FETCH", Fetch.class);
        add("IDLE", Idle.class);
        add("LIST", List.class);
        add("LOGIN", Login.class);
        add("LOGOUT", Logout.class);
        add("LSUB", LSub.class);
        add("NOOP", NOOP.class);
        add("PARTIAL", Partial.class);
        add("RENAME", Rename.class);
        add("SEARCH", Search.class);
        add("STATUS", Status.class);
        add("SELECT", Select.class);
        add("STARTTLS", StartTLS.class);
        add("STORE", Store.class);
        add("SUBSCRIBE", Subscribe.class);
        add("UID", UID.class);
        add("UNSUBSCRIBE", Unsubscribe.class);
    }

    /**
     * Constructor
     */
    public IMAPCommandFactory() {
        super();
    }

    /**
     * Adds new command
     * @param mnemonic mnemonic
     * @param c command
     */
    protected static void add(String mnemonic, Class<? extends IMAPCommand> c) {
        commands.put(mnemonic, c);
    }

    /**
     * Returns requested command
     * @param mnemonic mnemonic
     * @return command or {@link Bad} if command can't be found
     * @throws CommandException
     */
    public ConnectionHandler getCommand(String mnemonic) throws CommandException {

        Class<?> cls = (Class<?>)commands.get(mnemonic.toUpperCase());
        if (cls == null) {
            return new Bad();
        }

        try {
            Constructor<?> c = cls.getConstructor(new Class[]{String.class});
            if (c == null) {
                return new Bad();
            }

            ConnectionHandler command = (ConnectionHandler)c.newInstance(new Object[]{mnemonic});
            return command;
        } catch (Exception e) {
            logger.error("BAD: ", e);
            return new Bad();
        }

    }

}
