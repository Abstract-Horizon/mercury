/*
 * Copyright (c) 2004-2019 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.sync.commands;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.sync.SyncResponses;
import org.abstracthorizon.mercury.sync.SyncSession;
import org.abstracthorizon.mercury.sync.exception.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sync QUIT/BYE/EXIT command.
 *
 * @author Daniel Sendula
 */
public class QuitCommand extends SyncCommand {

    protected static final Logger logger = LoggerFactory.getLogger(QuitCommand.class);

    /**
     * Constructor
     */
    public QuitCommand() {
    }

    /**
     * Executed the command
     * @param connection smtp session
     * @throws CommandException
     * @throws IOException
     * @throws ParserException
     */
    public void execute(SyncSession connection, String command, String cmdLine) throws CommandException, IOException, ParserException {
        try {
            connection.setStreamDebug(false);
            connection.setDropConnection(true);
            connection.sendResponse(SyncResponses.BYE_RESPONSE);
        } catch (IOException e) {
            // TODO should we drop the line here?
            // Scenario: data is late in the middle of e-mail
            // we have timeout + data arrive
            // -> loads of syntax errors and other side gives up
            // Solution: soon we have IO exception - we send response
            // and close the socket?
            if (!(e instanceof SocketTimeoutException)) {
                logger.error("Problem reading message", e);
            }
            connection.sendResponse(SyncResponses.GENERIC_ERROR_RESPONSE);
        } finally {
            connection.setStreamDebug(true);
        }
    }
}
