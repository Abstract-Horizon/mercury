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
package org.abstracthorizon.mercury.smtp.command;

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.smtp.SMTPResponses;
import org.abstracthorizon.mercury.smtp.SMTPScanner;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.exception.ParserException;
import org.abstracthorizon.mercury.smtp.util.Path;

import java.io.IOException;

/**
 * MAIL TO command.
 *
 * @author Daniel Sendula
 */
public class MailCommand extends SMTPCommand {

    /**
     * Constructor
     */
    public MailCommand() {
    }

    /**
     * Executed the command
     * @param connection smtp session
     * @throws CommandException
     * @throws IOException
     * @throws ParserException
     */
    protected void execute(SMTPSession connection) throws CommandException, IOException, ParserException {
        if (connection.getState() == SMTPSession.STATE_AUTH_NEEDED) {
            connection.sendResponse(SMTPResponses.AUTHENTICATION_REQUIRED);
            return;
        }
        if (connection.getState() != SMTPSession.STATE_READY) {
            connection.sendResponse(SMTPResponses.BAD_SEQUENCE_OF_COMMANDS_RESPONSE);
            return;
        }
        connection.getMailSessionData().clear();

        SMTPScanner scanner = connection.getScanner();
        if (!scanner.is_char(' ')) { throw new ParserException("space"); }
        if (!scanner.keyword("FROM:")) { throw new ParserException("FROM:"); }
        if (scanner.is_char(' ')) {
            // Allow for incorrect clients that add space here..
        }
        Path path = new Path();
        if (!scanner.keyword("<>")) {
            if (!scanner.path(path)) { throw new ParserException("reverse path"); }
        }

        readExtraParameters(connection, scanner);

        connection.getMailSessionData().setSourceMailbox(path);
        processMailFrom(connection, path);

    }

    /**
     * Obtains extra parameters.
     *
     * @param session SMTP session
     * @param scanner STMP scanner
     * @throws IOException io exception
     * @throws ParserException parsing exception
     * @throws CommandException command exception
     */
    protected void readExtraParameters(SMTPSession connection, SMTPScanner scanner) throws IOException, ParserException, CommandException {
        scanner.skip_line();
    }

    /**
     * Sets from path to the session. This method can test if and make an
     * different action (sending different response).
     *
     * @param path path to be stored in the session
     * @param session SMTP session
     * @throws IOException
     */
    protected void processMailFrom(SMTPSession connection, Path path) throws IOException {
        connection.sendResponse(SMTPResponses.OK_RESPONSE);
        connection.setState(SMTPSession.STATE_MAIL);
    }

}
