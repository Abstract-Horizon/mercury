/*
 * Copyright (c) 2004-2007 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.smtp.command;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.connection.ConnectionHandler;
import org.abstracthorizon.danube.support.RuntimeIOException;
import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.exception.ParserException;

import java.io.IOException;

/**
 * Base SMTP comamnd. It defines handling of exceptions from commands
 *
 * @author Daniel Sendula
 */
public abstract class SMTPCommand implements ConnectionHandler {

    /**
     * Constructor
     */
    public SMTPCommand() {
    }

    /**
     * Executed the command calling {@link #execute(SMTPSession)} method
     * @param connection smtp session
     */
    public void handleConnection(Connection connection) {
        SMTPSession smtpConnection = (SMTPSession)connection;
        try {
            execute(smtpConnection);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    /**
     * Executed the command
     * @param connection smtp session
     * @throws CommandException
     * @throws IOException
     * @throws ParserException
     */
    protected abstract void execute(SMTPSession connection) throws CommandException, IOException, ParserException;

}
